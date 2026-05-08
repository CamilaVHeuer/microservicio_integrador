package com.camicompany.microserviciointegrador.service;

import com.camicompany.microserviciointegrador.client.HelipagosClient;
import com.camicompany.microserviciointegrador.domain.Payment;
import com.camicompany.microserviciointegrador.domain.PaymentStatus;
import com.camicompany.microserviciointegrador.dto.*;
import com.camicompany.microserviciointegrador.dto.createPaymentDto.*;
import com.camicompany.microserviciointegrador.exception.*;
import com.camicompany.microserviciointegrador.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceImpTest {
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private HelipagosClient helipagosClient;
    private PaymentService paymentService;

    private final String redirectUrl = "http://redirect";
    private final String webhookUrl = "http://webhook";
    private final String apiKey = "test-api-key";

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImp(paymentRepository, helipagosClient, redirectUrl, webhookUrl, apiKey);
    }

    @Test
    void createPaymentSuccessfully() {
        CreatePaymentRequest req = new CreatePaymentRequest(1000L, "desc", LocalDate.now().plusDays(1), "ref1");
    HelipagosCreatePaymentResponse helipagosResp = new HelipagosCreatePaymentResponse(
            1, "GENERADA", "REF1", "checkoutUrl"
    );

    when(paymentRepository.findByReferenciaExterna("REF1")).thenReturn(Optional.empty());
    when(helipagosClient.createPayment(any())).thenReturn(helipagosResp);
    when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
                    Payment payment = invocation.getArgument(0);
                    payment.setId(1L);
                    return payment;
                });
    //Act
    PaymentResponse response = paymentService.createPayment(req);

    // Assert
    assertEquals(1L, response.paymentId());
    assertEquals("1", response.id_sp());
    assertEquals("REF1", response.referencia_externa());
    assertEquals("GENERATED", response.estado_interno());
    assertEquals("GENERADA", response.estado_externo());
    assertEquals("checkoutUrl", response.checkout_url());
    verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void getPaymentExistingSuccessfully() {


        Payment payment = createPaymentEntity("1", "desc", "REF1", "GENERADA",
                PaymentStatus.GENERATED,  "checkoutUrl");
        payment.setId(1L);
        when(paymentRepository.findByIdSp("1")).thenReturn(Optional.of(payment));
        HelipagosGetPaymentResponse helipagosResp = new HelipagosGetPaymentResponse(1, "PROCESADA", "REF1");
        when(helipagosClient.getPayment("1")).thenReturn(helipagosResp);

        PaymentResponse response = paymentService.getPayment("1");

        assertEquals(1L, response.paymentId());
        assertEquals("1", response.id_sp());
        assertEquals("REF1", response.referencia_externa());
        assertEquals("PROCESSED", response.estado_interno());
        assertEquals("PROCESADA", response.estado_externo());
        assertEquals("checkoutUrl", response.checkout_url());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void getPaymentShouldThrowResourceNotFoundException() {
        when(paymentRepository.findByIdSp("1")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> paymentService.getPayment("1"));
        verify(paymentRepository).findByIdSp("1");
    }

    @Test
    void cancelPaymentSuccessfully() {
        Payment payment = createPaymentEntity("1", "desc", "REF1", "GENERADA",
                PaymentStatus.GENERATED,  "checkoutUrl");
        payment.setId(1L);
        when(paymentRepository.findByIdSp("1")).thenReturn(Optional.of(payment));
        HelipagosGetPaymentResponse helipagosResp = new HelipagosGetPaymentResponse(1, "GENERADA", "REF1");
        when(helipagosClient.getPayment("1")).thenReturn(helipagosResp);

        PaymentResponse response = paymentService.cancelPayment("1");
        assertEquals(1L, response.paymentId());
        assertEquals("1", response.id_sp());
        assertEquals("REF1", response.referencia_externa());
        assertEquals("CANCELLED", response.estado_interno());
        assertEquals("VENCIDA", response.estado_externo());
        assertEquals("checkoutUrl", response.checkout_url());

        verify(helipagosClient).cancelPayment("1");
        verify(paymentRepository, atLeastOnce()).save(any());
    }

    @Test
    void cancelPaymentShouldThrowResourceNotFoundException() {
        when(paymentRepository.findByIdSp("1")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> paymentService.cancelPayment("1"));
        verify(paymentRepository).findByIdSp("1");
    }

    @Test
    void processWebhookSuccessfully() {
        HelipagosWebhookRequest req = new HelipagosWebhookRequest(1, "PROCESADA", "ref1", null, null, null);
        Payment payment = createPaymentEntity("1", "desc", "REF1", "GENERADA",
                PaymentStatus.GENERATED,  "checkoutUrl");
        payment.setId(1L);
        when(paymentRepository.findByIdSp("1")).thenReturn(Optional.of(payment));

        paymentService.processWebhook(req, apiKey);

        assertEquals(1L, payment.getId());
        assertEquals("1", payment.getIdSp());
        assertEquals("REF1", payment.getReferenciaExterna());
        assertEquals(PaymentStatus.PROCESSED, payment.getEstadoInterno());
        assertEquals("PROCESADA", payment.getEstadoExterno());
        assertEquals("checkoutUrl", payment.getCheckoutUrl());
        verify(paymentRepository).save(payment);
    }

    @Test
    void createPaymentWhenDuplicateReference() {
        CreatePaymentRequest req = new CreatePaymentRequest(1000L, "desc", LocalDate.now().plusDays(1), "ref1");
        Payment existing = createPaymentEntity("1", "desc", "REF1", "GENERADA",
                PaymentStatus.GENERATED,  "checkoutUrl");
        existing.setId(1L);
        when(paymentRepository.findByReferenciaExterna("REF1")).thenReturn(Optional.of(existing));

        PaymentResponse response = paymentService.createPayment(req);

        assertNotNull(response);
        assertEquals(1L, response.paymentId());
        assertEquals("1", response.id_sp());
        assertEquals("REF1", response.referencia_externa());

        verify(helipagosClient, never()).createPayment(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void createPaymentShouldThrowExternalServiceExceptionWhenHelipagosUnavailable() {
        CreatePaymentRequest req = new CreatePaymentRequest(1000L, "desc", LocalDate.now().plusDays(1), "ref1");
        when(paymentRepository.findByReferenciaExterna(any())).thenReturn(Optional.empty());
        when(helipagosClient.createPayment(any())).thenThrow(new ExternalServiceException("Helipagos unavailable"));

        assertThrows(ExternalServiceException.class, () -> paymentService.createPayment(req));
    }

    @Test
    void processWebhookWhenUnknownIdSp() {
        HelipagosWebhookRequest req = new HelipagosWebhookRequest(999, "PROCESADA", "ref1", null, null, null);
        when(paymentRepository.findByIdSp("999")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> paymentService.processWebhook(req, apiKey));
    }

    @Test
    void processWebhookConcurrent() throws Exception {

        HelipagosWebhookRequest req = new HelipagosWebhookRequest(1, "PROCESADA", "ref1",
                        null, null, null);

        Payment payment = createPaymentEntity("1", "desc", "REF1", "GENERADA",
                PaymentStatus.GENERATED, "checkout_url");
        payment.setId(1L);
        when(paymentRepository.findByIdSp("1")).thenReturn(Optional.of(payment));

        int threads = 50;

        ExecutorService executorService = Executors.newFixedThreadPool(10);

        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {

            executorService.submit(() -> {
                try {
                    paymentService.processWebhook(req, apiKey);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        verify(paymentRepository, atLeastOnce()).save(any(Payment.class));
        assertEquals(PaymentStatus.PROCESSED, payment.getEstadoInterno());
        assertEquals("PROCESADA", payment.getEstadoExterno());

        executorService.shutdown();
    }

    //helper method
    private Payment createPaymentEntity(String idSp, String desc, String referenciaExterna, String estadoExterno,
                                        PaymentStatus estadoInterno, String checkout_url) {
        Payment payment = new Payment();
        payment.setIdSp(idSp);
        payment.setDescripcion(desc);
        payment.setReferenciaExterna(referenciaExterna);
        payment.setEstadoExterno(estadoExterno);
        payment.setEstadoInterno(estadoInterno);
        payment.setCheckoutUrl(checkout_url);

        return payment;
    }
}


