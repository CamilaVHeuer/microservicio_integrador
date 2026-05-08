
package com.camicompany.microserviciointegrador.integration;
import com.camicompany.microserviciointegrador.dto.HelipagosWebhookRequest;
import com.camicompany.microserviciointegrador.client.HelipagosClient;
import com.camicompany.microserviciointegrador.domain.Payment;
import com.camicompany.microserviciointegrador.domain.PaymentStatus;
import com.camicompany.microserviciointegrador.dto.HelipagosGetPaymentResponse;
import com.camicompany.microserviciointegrador.dto.createPaymentDto.HelipagosCreatePaymentResponse;
import com.camicompany.microserviciointegrador.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.camicompany.microserviciointegrador.dto.createPaymentDto.CreatePaymentRequest;
import org.springframework.http.MediaType;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.LocalDate;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
public class PaymentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private HelipagosClient helipagosClient;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PaymentRepository paymentRepository;

    private static final String BASE_URL = "/api/v1/payments";


    @Test
    void createPaymentShouldReturn201AndPersist() throws Exception {
        when(helipagosClient.createPayment(any()))
                .thenReturn(
                        new HelipagosCreatePaymentResponse(
                                123,
                                "GENERADA",
                                "REF123",
                                "http://checkout-test"
                        )
                );

        CreatePaymentRequest req = new CreatePaymentRequest(
                10000L,
                "Pago de servicios",
                LocalDate.now().plusDays(1),
                "REF123"
        );

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id_sp").value(123))
                .andExpect(jsonPath("$.referencia_externa").value("REF123"))
                .andExpect(jsonPath("$.estado_externo").value("GENERADA"))
                .andExpect(jsonPath("$.estado_interno").value("GENERATED"))
                .andExpect(jsonPath("$.referencia_externa").value("REF123"))
                .andExpect(jsonPath("$.checkout_url").value("http://checkout-test"));

        verify(helipagosClient).createPayment(any());
    }

    @Test
    void createPaymentWhenMissingFieldShouldReturn400() throws Exception {
        CreatePaymentRequest req = new CreatePaymentRequest(null, "desc",
                LocalDate.now().plusDays(1), "REF1000");
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void getPaymentShouldReturn200AndSyncStatus() throws Exception {

        Payment payment = createPaymentEntity("200", "desc", 10000L, LocalDate.now().plusDays(1),
                "REF123", "GENERADA", PaymentStatus.GENERATED, "http://checkout-test"
        );

        paymentRepository.save(payment);

        when(helipagosClient.getPayment(any()))
                .thenReturn(new HelipagosGetPaymentResponse(
                        200,
                        "GENERADA",
                        "REF2000"
                ));


        mockMvc.perform(get(BASE_URL + "/" + payment.getIdSp()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").exists())
                .andExpect(jsonPath("$.id_sp").value(payment.getIdSp()))
                .andExpect(jsonPath("$.referencia_externa").value("REF123"))
                .andExpect(jsonPath("$.estado_externo").value("GENERADA"))
                .andExpect(jsonPath("$.estado_interno").value("GENERATED"))
                .andExpect(jsonPath("$.checkout_url").value("http://checkout-test"));

        verify(helipagosClient).getPayment("200");
    }


    @Test
    void getPaymentNotFoundShouldReturn404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value(containsString("Payment not found with id_sp: 999999")))
                .andExpect(jsonPath("$.timestamp").exists());
    }


    @Test
    void cancelPaymentShouldReturn200AndUpdateStatus() throws Exception {
        Payment payment = createPaymentEntity("200", "desc", 10000L, LocalDate.now().plusDays(1),
                "REF123", "GENERADA", PaymentStatus.GENERATED, "http://checkout-test");

        paymentRepository.save(payment);

        //mock of internal sync to ensure the payment is in sync before cancellation
        when(helipagosClient.getPayment(any()))
                .thenReturn(new HelipagosGetPaymentResponse(
                        200,
                        "GENERADA",
                        "REF2000"
                ));
        //mock of cancel call to helipagos
        doNothing().when(helipagosClient).cancelPayment(any());

        mockMvc.perform(delete(BASE_URL + "/" + payment.getIdSp()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").exists())
                .andExpect(jsonPath("$.id_sp").value(payment.getIdSp()))
                .andExpect(jsonPath("$.referencia_externa").value("REF123"))
                .andExpect(jsonPath("$.estado_externo").value("VENCIDA"))
                .andExpect(jsonPath("$.estado_interno").value("CANCELLED"));

        verify(helipagosClient).getPayment("200");
        verify(helipagosClient).cancelPayment("200");
    }

    @Test
    void cancelPaymentNotFoundShouldReturn404() throws Exception {

        mockMvc.perform(delete(BASE_URL + "/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message")
                        .value(containsString("Payment not found with id_sp: 999999")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void webhookShouldUpdatePaymentStatusAndShouldReturn200() throws Exception {
        Payment payment = createPaymentEntity("300", "desc", 10000L, LocalDate.now().plusDays(1),
                "REF300", "GENERADA", PaymentStatus.GENERATED, "http://checkout-test");

        paymentRepository.save(payment);

        HelipagosWebhookRequest req = new HelipagosWebhookRequest(300, "PROCESADA", "REF300", null, null, null);
        mockMvc.perform(post(BASE_URL + "/webhook")
                        .header("api-key", "test-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
        Payment updated = paymentRepository.findByIdSp("300").orElseThrow();

        assertEquals("PROCESADA", updated.getEstadoExterno());
        assertEquals(PaymentStatus.PROCESSED, updated.getEstadoInterno());
    }

    @Test
    void webhookShouldBeIdempotentAndReturn200() throws Exception {

        // Arrange
        Payment payment = createPaymentEntity("400", "desc", 10000L, LocalDate.now().plusDays(1),
                "REF400", "GENERADA", PaymentStatus.GENERATED, "http://checkout-test");
        paymentRepository.save(payment);

        HelipagosWebhookRequest req =
                new HelipagosWebhookRequest(
                        400,
                        "PROCESADA",
                        "REF400",
                        null,
                        null,
                        null
                );

        // First webhook delivery
        mockMvc.perform(post(BASE_URL + "/webhook")
                        .header("api-key", "test-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        // Second duplicated webhook delivery
        mockMvc.perform(post(BASE_URL + "/webhook")
                        .header("api-key", "test-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        // Assert final state
        Payment updated =
                paymentRepository.findByIdSp("400")
                        .orElseThrow();

        assertEquals("PROCESADA", updated.getEstadoExterno());
        assertEquals(PaymentStatus.PROCESSED, updated.getEstadoInterno());
    }

    @Test
    void webhookShouldReturn400WhenApiKeyIsMissing() throws Exception {
        HelipagosWebhookRequest req = new HelipagosWebhookRequest(301, "PROCESADA", "REF301", null, null, null);
        mockMvc.perform(post(BASE_URL + "/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void webhookShouldReturn400WhenApiKeyIsInvalid() throws Exception {
        HelipagosWebhookRequest req = new HelipagosWebhookRequest(302, "PROCESADA", "REF302", null, null, null);
        mockMvc.perform(post(BASE_URL + "/webhook")
                        .header("api-key", "invalid-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid webhook api-key"));
    }

    private Payment createPaymentEntity(String idSp, String desc, Long importe, LocalDate fechaVto, String referenciaExterna, String estadoExterno,
                                        PaymentStatus estadoInterno, String checkout_url) {
        Payment payment = new Payment();
        payment.setIdSp(idSp);
        payment.setDescripcion(desc);
        payment.setImporte(importe);
        payment.setFechaVto(fechaVto);
        payment.setReferenciaExterna(referenciaExterna);
        payment.setEstadoExterno(estadoExterno);
        payment.setEstadoInterno(estadoInterno);
        payment.setCheckoutUrl(checkout_url);

        return payment;
    }
}
