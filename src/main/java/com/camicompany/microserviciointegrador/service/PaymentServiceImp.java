package com.camicompany.microserviciointegrador.service;

import com.camicompany.microserviciointegrador.client.HelipagosClient;
import com.camicompany.microserviciointegrador.domain.Payment;
import com.camicompany.microserviciointegrador.domain.PaymentStatus;
import com.camicompany.microserviciointegrador.dto.*;
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

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImp.class);

     public PaymentServiceImp(PaymentRepository paymentRepository, HelipagosClient helipagosClient,
                              @Value("${app.redirect-url}") String redirectUrl,
                              @Value("${app.webhook-url}") String webhookUrl) {
         this.paymentRepository = paymentRepository;
         this.helipagosClient = helipagosClient;
         this.redirectUrl = redirectUrl;
         this.webhookUrl = webhookUrl;
     }

    @Override
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        // 1. Verificar idempotencia
        Payment existingPayment = paymentRepository
                .findByReferenciaExterna(request.referenciaExterna())
                .orElse(null);

        if (existingPayment != null) {
            log.info("Idempotent request detected for referencia_externa {}", request.referenciaExterna());
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
        HelipagosCreatePaymentResponse helipagosResponse =
                helipagosClient.createPayment(helipagosRequest);

        //Validacion defensiva
        if (helipagosResponse.referencia_externa() == null ||
                !request.referenciaExterna().equals(helipagosResponse.referencia_externa())) {
            log.warn("Helipagos mismatch referencia_externa. Sent: {}, Received: {}",
                    request.referenciaExterna(), helipagosResponse.referencia_externa());
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
            // 🔥 caso concurrencia: otro request insertó primero
            log.error("Concurrent request detected for referencia_externa {}",
                    request.referenciaExterna());

            Payment alreadyCreated = paymentRepository
                    .findByReferenciaExterna(request.referenciaExterna())
                    .orElseThrow(() -> e); // muy improbable, pero defensivo

            return PaymentMapper.toResponseDto(alreadyCreated);
        }
    }

    @Override
    public PaymentResponse getPayment(String idSp) {

        // 1. Buscar en DB
        Payment payment = paymentRepository.findByIdSp(idSp).orElseThrow(() -> new ResourceNotFoundException("Payment not found with id_sp: " + idSp));

        // 2. Consultar estado en Helipagos
        HelipagosGetPaymentResponse response =
                helipagosClient.getPayment(idSp);

        // 🔴 VALIDACIÓN CLAVE
        if (response.estado_pago() == null || response.estado_pago().isBlank()) {
            log.error("Helipagos returned invalid estado_pago for id_sp {}: {}", idSp, response.estado_pago());
            throw new ExternalServiceException("Invalid response from Helipagos: estado_pago is null or blank");
        }
        // 3. Sincronizar estado
        if(!payment.getEstadoExterno().equals(response.estado_pago())) {
            log.info("Payment {} updated status: {} -> {}",
                    idSp, payment.getEstadoExterno(), response.estado_pago());
            PaymentMapper.syncStatusFromHelipagos(payment, response.estado_pago());

            // 4. Guardar actualización
            paymentRepository.save(payment);
        }
        // 5. Devolver respuesta
        return PaymentMapper.toResponseDto(payment);

    }

    @Override
    public PaymentResponse cancelPayment(String idSp) {
        // 1. Buscar en DB
        Payment payment = paymentRepository.findByIdSp(idSp)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id_sp: " + idSp));

        String estadoExterno = payment.getEstadoExterno();
        // 2. Validar estado nulo (defensivo)
        if (estadoExterno == null || estadoExterno.isBlank()) {
            throw new ExternalServiceException(
                    "Cannot determine external state for payment " + idSp);
        }

        // 3. Idempotencia (ya cancelado)
        if ("VENCIDA".equalsIgnoreCase(estadoExterno)) {
            log.info("Payment {} already cancelled (idempotent)", idSp);
            return PaymentMapper.toResponseDto(payment);
        }

        // 4. Validación de negocio según Helipagos
        // Solo permite cancelar si está en GENERADA o RECHAZADA
        if (!canBeCancelled(estadoExterno)) {throw new StatusConflictException(
                    "Cannot cancel payment in state: " + estadoExterno);
        }

        // 5. Llamar a Helipagos
        helipagosClient.cancelPayment(idSp);

        // 6. Actualizar estado local (optimista)
        payment.setEstadoExterno("VENCIDA");
        payment.setEstadoInterno(PaymentStatus.CANCELLED);

        // 7. Guardar
        paymentRepository.save(payment);

        // 8. Devolver
        return PaymentMapper.toResponseDto(payment);
    }

    private boolean canBeCancelled(String estado) {
        return "GENERADA".equalsIgnoreCase(estado) ||
                "RECHAZADA".equalsIgnoreCase(estado);
    }

}
