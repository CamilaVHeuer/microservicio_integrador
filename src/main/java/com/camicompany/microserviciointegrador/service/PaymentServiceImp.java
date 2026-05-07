package com.camicompany.microserviciointegrador.service;

import com.camicompany.microserviciointegrador.client.HelipagosClient;
import com.camicompany.microserviciointegrador.domain.Payment;
import com.camicompany.microserviciointegrador.domain.PaymentStatus;
import com.camicompany.microserviciointegrador.dto.*;
import com.camicompany.microserviciointegrador.dto.createPaymentDto.CreatePaymentRequest;
import com.camicompany.microserviciointegrador.dto.createPaymentDto.HelipagosCreatePaymentRequest;
import com.camicompany.microserviciointegrador.dto.createPaymentDto.HelipagosCreatePaymentResponse;
import com.camicompany.microserviciointegrador.exception.ExternalServiceBadRequestException;
import com.camicompany.microserviciointegrador.exception.ExternalServiceException;
import com.camicompany.microserviciointegrador.exception.ResourceNotFoundException;
import com.camicompany.microserviciointegrador.exception.StatusConflictException;
import com.camicompany.microserviciointegrador.mapper.PaymentMapper;
import com.camicompany.microserviciointegrador.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional
public class PaymentServiceImp implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final HelipagosClient helipagosClient;
    private final String redirectUrl;
    private final String webhookUrl;
    private final String apiKey;

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImp.class);

    public PaymentServiceImp(PaymentRepository paymentRepository, HelipagosClient helipagosClient,
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

        String referenciaExternaNormalized = PaymentMapper.normalizeReference(request.referenciaExterna());
        // 1. Verificar idempotencia
        Payment existingPayment = paymentRepository
                .findByReferenciaExterna(referenciaExternaNormalized)
                .orElse(null);

        if (existingPayment != null) {
            log.info("Idempotent request detected for referencia_externa {}", referenciaExternaNormalized);
            return PaymentMapper.toResponseDto(existingPayment);
        }

        // 2. Armar request hacia Helipagos
        HelipagosCreatePaymentRequest helipagosRequest =
                PaymentMapper.toHelipagosRequest(
                        request,
                        redirectUrl,
                        webhookUrl
                );

        // 3. Llamar a Helipagos
        log.info("Outgoing request to create payment {} in Helipagos", referenciaExternaNormalized);
        HelipagosCreatePaymentResponse helipagosResponse =
                helipagosClient.createPayment(helipagosRequest);

        //Validacion defensiva
        if (helipagosResponse.referencia_externa() == null ||
                !referenciaExternaNormalized.equals(helipagosResponse.referencia_externa())) {
            log.warn("Helipagos mismatch referencia_externa. Sent: {}, Received: {}",
                    referenciaExternaNormalized, helipagosResponse.referencia_externa());
        }

        // 4. Mapear a entidad
        Payment payment =
                PaymentMapper.toEntity(request, helipagosResponse);

        // 5. Guardar en DB con proteccion de concurrencia
        try {
            Payment savedPayment = paymentRepository.save(payment);
            // 6. Mapear a DTO de salida
            return PaymentMapper.toResponseDto(savedPayment);
        } catch (DataIntegrityViolationException e) {
            // caso concurrencia: otro request insertó primero
            log.error("Duplicate referencia_externa detected {}",
                    referenciaExternaNormalized);

            Payment alreadyCreated = paymentRepository
                    .findByReferenciaExterna(referenciaExternaNormalized)
                    .orElseThrow(() -> {
                        log.error("Duplicated referencia_externa not found {}", referenciaExternaNormalized);
                        return e;
                    }); // muy improbable, pero defensivo
            return PaymentMapper.toResponseDto(alreadyCreated);
        }
    }

    @Override
    public PaymentResponse getPayment(String idSp) {
        // 1. Obtener payment y sincronizar estado con Helipagos
        Payment payment = getAndSyncPayment(idSp);
        // 2. Devolver respuesta
        return PaymentMapper.toResponseDto(payment);

    }

    @Override
    public PaymentResponse cancelPayment(String idSp) {
        Payment payment = getAndSyncPayment(idSp);

        String estadoExterno = payment.getEstadoExterno();

        // 3. Idempotencia (ya cancelado)
        if ("VENCIDA".equalsIgnoreCase(estadoExterno)) {
            log.info("Payment {} already cancelled (idempotent)", idSp);
            return PaymentMapper.toResponseDto(payment);
        }

        // 4. Validación de negocio según Helipagos
        // Solo permite cancelar si está en GENERADA o RECHAZADA
        if (!canBeCancelled(estadoExterno)) {
            throw new StatusConflictException(
                    "Cannot cancel payment in state: " + estadoExterno);
        }
        // 5. Llamar a Helipagos para cancelar
        log.info("Outgoing request to cancel payment {} in Helipagos", idSp);
        helipagosClient.cancelPayment(idSp);

        // 6. Actualizar estado local (optimista)
        payment.setEstadoExterno("VENCIDA");
        payment.setEstadoInterno(PaymentStatus.CANCELLED);

        // 7. Guardar
        paymentRepository.save(payment);

        // 8. Devolver
        return PaymentMapper.toResponseDto(payment);
    }

    @Override
    public void processWebhook(HelipagosWebhookRequest request, String apiKey) {

        log.info("Processing webhook request {}", request.id_sp());
        // 1. validar api key
        if (!validateApiKey(apiKey)) {
            throw new ExternalServiceBadRequestException("Invalid webhook api key");
        }

        //VALIDACIÓN CLAVE
        if (request.estado() == null || request.estado().isBlank()) {
            log.error("Invalid webhook estado for id_sp {}: {}", request.id_sp(), request.estado());
            throw new ExternalServiceException("Invalid webhook payload: estado is null or blank");
        }

        // 2. buscar payment
        Payment payment = paymentRepository.findByIdSp(
                String.valueOf(request.id_sp())).orElse(null);
        if (payment == null) {
            log.warn("Webhook received for unknown payment id_sp {}", request.id_sp());
            return; // no exception, solo ignoramos
        }

        // 3. Idempotencia: mismo estado
        if (payment.getEstadoExterno() != null && payment.getEstadoExterno().equalsIgnoreCase(request.estado())) {
            log.info("Duplicate webhook ignored for payment {}", request.id_sp());
            return;
        }

        // 4. Sincronizar estado
        PaymentMapper.syncStatusFromHelipagos(payment, request.estado());

        paymentRepository.save(payment);
        log.info("Webhook processed for payment {}", request.id_sp());

    }

    @Override
    public PaymentResponse getPaymentByIsSp(String idSp) {
        Payment payment = paymentRepository.findByIdSp(idSp)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id_sp: " + idSp));
        return PaymentMapper.toResponseDto(payment);
    }


    private boolean canBeCancelled(String estado) {
        return "GENERADA".equalsIgnoreCase(estado) ||
                "RECHAZADA".equalsIgnoreCase(estado);
    }

    private boolean validateApiKey(String apiKey) {
        return apiKey != null && apiKey.equals(this.apiKey);
    }

    private Payment getAndSyncPayment(String idSp) {
        // 1. Buscar en DB
        Payment payment = paymentRepository.findByIdSp(idSp).orElseThrow(() ->
                new ResourceNotFoundException("Payment not found with id_sp: " + idSp));

        // 2. Consultar estado en Helipagos
        log.info("Outgoing request to get payment {} from Helipagos", idSp);
        HelipagosGetPaymentResponse response =
                helipagosClient.getPayment(idSp);

        //  VALIDACIÓN CLAVE
        if (response.estado_pago() == null || response.estado_pago().isBlank()) {
            log.error("Helipagos returned invalid estado_pago for id_sp {}: {}", idSp, response.estado_pago());
            throw new ExternalServiceException("Invalid response from Helipagos: estado_pago is null or blank");
        }

        return syncPayment(payment, response);
    }

    private Payment syncPayment(Payment payment, HelipagosGetPaymentResponse response) {
        // 3. Sincronizar estado
        if (!payment.getEstadoExterno().equals(response.estado_pago())) {
            log.info("Payment {} updated status: {} -> {}",
                    payment.getIdSp(), payment.getEstadoExterno(), response.estado_pago());
            PaymentMapper.syncStatusFromHelipagos(payment, response.estado_pago());

            // 4. Guardar actualización
            paymentRepository.save(payment);
        }
        return payment;
    }
}



