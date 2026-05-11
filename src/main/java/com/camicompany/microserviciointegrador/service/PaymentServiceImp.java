package com.camicompany.microserviciointegrador.service;

import com.camicompany.microserviciointegrador.client.HelipagosClient;
import com.camicompany.microserviciointegrador.domain.payment.Payment;
import com.camicompany.microserviciointegrador.domain.payment.PaymentStatus;
import com.camicompany.microserviciointegrador.dto.*;
import com.camicompany.microserviciointegrador.dto.createPaymentDto.CreatePaymentRequest;
import com.camicompany.microserviciointegrador.dto.createPaymentDto.HelipagosCreatePaymentRequest;
import com.camicompany.microserviciointegrador.dto.createPaymentDto.HelipagosCreatePaymentResponse;
import com.camicompany.microserviciointegrador.dto.getPaymentDto.HelipagosGetPaymentResponse;
import com.camicompany.microserviciointegrador.dto.weebhookDto.HelipagosWebhookRequest;
import com.camicompany.microserviciointegrador.exception.*;
import com.camicompany.microserviciointegrador.mapper.PaymentMapper;
import com.camicompany.microserviciointegrador.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentServiceImp implements PaymentService {

  private final PaymentRepository paymentRepository;
  private final HelipagosClient helipagosClient;
  private final String redirectUrl;
  private final String webhookUrl;
  private final String apiKey;

  private static final Logger log = LoggerFactory.getLogger(PaymentServiceImp.class);

  public PaymentServiceImp(
      PaymentRepository paymentRepository,
      HelipagosClient helipagosClient,
      @Value("${app.redirect-url}") String redirectUrl,
      @Value("${app.webhook-url}") String webhookUrl,
      @Value("${app.api-key}") String apiKey) {
    this.paymentRepository = paymentRepository;
    this.helipagosClient = helipagosClient;
    this.redirectUrl = redirectUrl;
    this.webhookUrl = webhookUrl;
    this.apiKey = apiKey;
  }

  @Override
  public PaymentResponse createPayment(CreatePaymentRequest request) {

    String referenciaExternaNormalized =
        PaymentMapper.normalizeReference(request.referenciaExterna());

    Payment pendingPayment;

    try {
      pendingPayment = createPendingPayment(request, referenciaExternaNormalized);

    } catch (DataIntegrityViolationException e) {

      log.info("Duplicate referencia_externa detected: {}", referenciaExternaNormalized);

      Payment existingPayment =
          paymentRepository.findByReferenciaExterna(referenciaExternaNormalized).orElseThrow();

      return PaymentMapper.toResponseDto(existingPayment);
    }

    try {
      HelipagosCreatePaymentRequest helipagosRequest =
          PaymentMapper.toHelipagosRequest(request, redirectUrl, webhookUrl);

      HelipagosCreatePaymentResponse response = helipagosClient.createPayment(helipagosRequest);

      Payment updated = updatePaymentWithHelipagosResponse(pendingPayment.getId(), response);

      return PaymentMapper.toResponseDto(updated);

    } catch (ExternalServiceBadRequestException e) {

      log.error("Invalid request to Helipagos, paymentId={}", pendingPayment.getId(), e);

      markErrorSafely(pendingPayment.getId());

      throw e;
    } catch (ExternalServiceException e) {

      log.error("Helipagos unavailable, paymentId={}", pendingPayment.getId(), e);

      markErrorSafely(pendingPayment.getId());

      throw e;
    } catch (InternalServiceException e) {
      log.error("Error updating pendingPayment, paymentId={}", pendingPayment.getId(), e);

      markErrorSafely(pendingPayment.getId());

      throw e;
    } catch (Exception e) {
      log.error("Unexpected error creating payment, paymentId={}", pendingPayment.getId(), e);

      markErrorSafely(pendingPayment.getId());

      throw new InternalServiceException("Unexpected internal error", e);
    }
  }

  @Transactional
  private Payment createPendingPayment(
      CreatePaymentRequest request, String referenciaExternaNormalized) {
    Payment payment = new Payment();

    payment.setReferenciaExterna(referenciaExternaNormalized);
    payment.setImporte(request.importe());
    payment.setDescripcion(request.descripcion());
    payment.setFechaVto(request.fechaVto());
    payment.setEstadoInterno(PaymentStatus.PENDING);

    return paymentRepository.save(payment);
  }

  @Transactional
  private Payment updatePaymentWithHelipagosResponse(
      Long paymentId, HelipagosCreatePaymentResponse response) {

    Payment payment =
        paymentRepository
            .findById(paymentId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Payment not found with id: " + paymentId));

    PaymentMapper.toEntityFromHelipagosResponse(payment, response);

    return paymentRepository.save(payment);
  }

  @Transactional
  public void markAsError(Long paymentId) {

    Payment payment = paymentRepository.findById(paymentId).orElseThrow();

    payment.setEstadoInterno(PaymentStatus.ERROR);

    paymentRepository.save(payment);
  }

  private void markErrorSafely(Long paymentId) {
    try {
      markAsError(paymentId);
    } catch (Exception e) {
      log.error("Could not mark payment as error", e);
    }
  }

  @Transactional
  @Override
  public PaymentResponse getPayment(String idSp) {

    Payment payment = getAndSyncPayment(idSp);
    return PaymentMapper.toResponseDto(payment);
  }

  @Transactional
  @Override
  public PaymentResponse cancelPayment(String idSp) {
    Payment payment = getAndSyncPayment(idSp);

    String estadoExterno = payment.getEstadoExterno();

    if ("VENCIDA".equalsIgnoreCase(estadoExterno)) {
      log.info("Payment {} already cancelled (idempotent)", idSp);
      return PaymentMapper.toResponseDto(payment);
    }

    // Only allow cancellation if external status is "GENERADA" or "RECHAZADA"
    if (!canBeCancelled(estadoExterno)) {
      throw new StatusConflictException("Cannot cancel payment in state: " + estadoExterno);
    }

    log.info("Outgoing request to cancel payment {} in Helipagos", idSp);
    helipagosClient.cancelPayment(idSp);

    payment.setEstadoExterno("VENCIDA");
    payment.setEstadoInterno(PaymentStatus.CANCELLED);

    paymentRepository.save(payment);

    return PaymentMapper.toResponseDto(payment);
  }

  @Transactional
  @Override
  public void processWebhook(HelipagosWebhookRequest request, String apiKey) {

    log.info("Processing webhook request {}", request.id_sp());

    if (!validateApiKey(apiKey)) {
      throw new ExternalServiceBadRequestException("Invalid webhook api-key");
    }

    if (request.estado() == null || request.estado().isBlank()) {
      log.error("Invalid webhook estado for id_sp {}: {}", request.id_sp(), request.estado());
      throw new ExternalServiceException("Invalid webhook payload: estado is null or blank");
    }

    Payment payment = paymentRepository.findByIdSp(String.valueOf(request.id_sp())).orElse(null);
    if (payment == null) {
      log.warn("Webhook received for unknown payment id_sp {}", request.id_sp());
      try {
        payment = tryReconcilePaymentFromWebhook(request);
        log.info("Payment reconciled from webhook for id_sp {}", request.id_sp());
      } catch (ResourceNotFoundException e) {
        log.error(
            "Failed to reconcile payment from webhook for id_sp {}: {}",
            request.id_sp(),
            e.getMessage());
        return;
      }
    }

    if (payment.getEstadoExterno() != null
        && payment.getEstadoExterno().equalsIgnoreCase(request.estado())) {
      log.info("Duplicate webhook ignored for payment {}", request.id_sp());
      return;
    }

    PaymentMapper.syncStatusFromHelipagos(payment, request.estado());

    paymentRepository.save(payment);
    log.info("Webhook processed for payment {}", request.id_sp());
  }

  // ---------------- HELPER METHODS -----------------------------

  private boolean canBeCancelled(String estado) {
    return "GENERADA".equalsIgnoreCase(estado) || "RECHAZADA".equalsIgnoreCase(estado);
  }

  private boolean validateApiKey(String apiKey) {
    return apiKey != null && apiKey.equals(this.apiKey);
  }

  private Payment getAndSyncPayment(String idSp) {
    Payment payment =
        paymentRepository
            .findByIdSp(idSp)
            .orElseThrow(
                () -> new ResourceNotFoundException("Payment not found with id_sp: " + idSp));

    log.info("Outgoing request to get payment {} from Helipagos", idSp);
    HelipagosGetPaymentResponse response = helipagosClient.getPayment(idSp);

    if (response.estado_pago() == null || response.estado_pago().isBlank()) {
      log.error(
          "Helipagos returned invalid estado_pago for id_sp {}: {}", idSp, response.estado_pago());
      throw new ExternalServiceException(
          "Invalid response from Helipagos: estado_pago is null or blank");
    }

    return syncPayment(payment, response);
  }

  private Payment syncPayment(Payment payment, HelipagosGetPaymentResponse response) {

    if (!payment.getEstadoExterno().equals(response.estado_pago())) {
      log.info(
          "Payment {} updated status: {} -> {}",
          payment.getIdSp(),
          payment.getEstadoExterno(),
          response.estado_pago());
      PaymentMapper.syncStatusFromHelipagos(payment, response.estado_pago());

      paymentRepository.save(payment);
    }
    return payment;
  }

  private Payment tryReconcilePaymentFromWebhook(HelipagosWebhookRequest request) {
    String referenciaExternaNormalized =
        PaymentMapper.normalizeReference(request.referencia_externa());

    Payment payment =
        paymentRepository
            .findByReferenciaExterna(referenciaExternaNormalized)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Payment not found with referencia_externa: "
                            + referenciaExternaNormalized));
    log.info(
        "Reconciling payment {} from webhook referencia_externa {}",
        payment.getId(),
        referenciaExternaNormalized);

    PaymentMapper.syncPaymentFromWebhook(payment, request);
    return paymentRepository.save(payment);
  }
}
