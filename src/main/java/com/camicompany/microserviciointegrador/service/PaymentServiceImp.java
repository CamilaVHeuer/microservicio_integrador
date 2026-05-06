package com.camicompany.microserviciointegrador.service;

import com.camicompany.microserviciointegrador.client.HelipagosClient;
import com.camicompany.microserviciointegrador.domain.Payment;
import com.camicompany.microserviciointegrador.dto.CreatePaymentRequest;
import com.camicompany.microserviciointegrador.dto.CreatePaymentResponseDto;
import com.camicompany.microserviciointegrador.dto.HelipagosCreatePaymentRequest;
import com.camicompany.microserviciointegrador.dto.HelipagosCreatePaymentResponse;
import com.camicompany.microserviciointegrador.mapper.PaymentMapper;
import com.camicompany.microserviciointegrador.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

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
    public CreatePaymentResponseDto createPayment(CreatePaymentRequest request) {
        // 1. Generar referencia única
        String referenciaExterna = UUID.randomUUID().toString();

        // 2. Armar request hacia Helipagos
        HelipagosCreatePaymentRequest helipagosRequest =
                PaymentMapper.toHelipagosRequest(
                        request,
                        referenciaExterna,
                        redirectUrl,
                        webhookUrl
                );

        // 3. Llamar a Helipagos
        HelipagosCreatePaymentResponse helipagosResponse =
                helipagosClient.createPayment(helipagosRequest);

        if (helipagosResponse.referencia_externa() == null ||
                !referenciaExterna.equals(helipagosResponse.referencia_externa())) {
            log.warn("Helipagos mismatch referencia_externa. Sent: {}, Received: {}",
                    referenciaExterna, helipagosResponse.referencia_externa());
        }

        // 4. Mapear a entidad
        Payment payment =
                PaymentMapper.toEntity(request, helipagosResponse, referenciaExterna);

        // 5. Guardar en DB
        Payment savedPayment = paymentRepository.save(payment);

        // 6. Mapear a DTO de salida
        return PaymentMapper.toResponseDto(savedPayment);
    }

}
