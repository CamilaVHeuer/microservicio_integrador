package com.camicompany.microserviciointegrador.securityIntegration;

import com.camicompany.microserviciointegrador.client.HelipagosClient;
import com.camicompany.microserviciointegrador.domain.Payment;
import com.camicompany.microserviciointegrador.domain.PaymentStatus;
import com.camicompany.microserviciointegrador.dto.HelipagosGetPaymentResponse;
import com.camicompany.microserviciointegrador.dto.createPaymentDto.CreatePaymentRequest;
import com.camicompany.microserviciointegrador.dto.createPaymentDto.HelipagosCreatePaymentResponse;
import com.camicompany.microserviciointegrador.dto.registerUserDto.RegisterRequest;
import com.camicompany.microserviciointegrador.repository.PaymentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class PaymentSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private HelipagosClient helipagosClient;

    @Autowired
    private PaymentRepository paymentRepository;

    private static final String BASE_URL = "/api/v1/payments";



    @Test
    void createPaymentShouldReturn201WithValidApiKey() throws Exception {

        String apiKey = obtainApiKey("user1", "Password1");

        when(helipagosClient.createPayment(any()))
                .thenReturn(new HelipagosCreatePaymentResponse(
                                123,
                                "GENERADA",
                                "REF123",
                                "http://checkout"
                        )
                );

        CreatePaymentRequest req = new CreatePaymentRequest(
                        10000L,
                        "Pago test",
                        LocalDate.now().plusDays(1),
                        "REF123"
                );

        mockMvc.perform(post(BASE_URL)
                        .header("x-api-key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id_sp").value(123))
                .andExpect(jsonPath("$.referencia_externa").value("REF123"))
                .andExpect(jsonPath("$.estado_externo").value("GENERADA"))
                .andExpect(jsonPath("$.estado_interno").value("GENERATED"))
                .andExpect(jsonPath("$.checkout_url").value("http://checkout"));
    }

    @Test
    void createPaymentShouldReturn401WhenApiKeyIsMissing() throws Exception {

        CreatePaymentRequest req = new CreatePaymentRequest(
                10000L,
                "Pago test",
                LocalDate.now().plusDays(1),
                "REF123"
        );

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Access denied: authentication required"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void createPaymentShouldReturn401WhenApiKeyIsInvalid() throws Exception {

        CreatePaymentRequest req = new CreatePaymentRequest(
                10000L,
                "Pago test",
                LocalDate.now().plusDays(1),
                "REF123"
        );

        mockMvc.perform(post(BASE_URL)
                        .header("x-api-key", "ak_invalid_api_key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getPaymentShouldReturn200WithValidApiKey() throws Exception {

        String apiKey = obtainApiKey("user2", "Password1");

        Payment payment = createPaymentEntity("300", "desc", 10000L, LocalDate.now().plusDays(1), "REF300", "GENERADA",
                PaymentStatus.GENERATED, "http://checkout");

        paymentRepository.save(payment);

        when(helipagosClient.getPayment(any()))
                .thenReturn(new HelipagosGetPaymentResponse(
                        300,
                        "GENERADA",
                        "REF200"
                ));

        mockMvc.perform(get(BASE_URL + "/" + payment.getIdSp())
                        .header("x-api-key", apiKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id_sp").value("300"))
                .andExpect(jsonPath("$.referencia_externa").value("REF300"))
                .andExpect(jsonPath("$.estado_externo").value("GENERADA"))
                .andExpect(jsonPath("$.estado_interno").value("GENERATED"))
                .andExpect(jsonPath("$.checkout_url").value("http://checkout"));
    }

    @Test
    void getPaymentShouldReturn401WhenApiKeyIsMissing() throws Exception {

        mockMvc.perform(get(BASE_URL + "/200"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Access denied: authentication required"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void getPaymentShouldReturn401WhenApiKeyIsInvalid() throws Exception {

        mockMvc.perform(get(BASE_URL + "/200")
                        .header("x-api-key", "invalid-api-key"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cancelPaymentShouldReturn200WithValidApiKey() throws Exception {

        String apiKey = obtainApiKey("user3", "Password1");

        Payment payment = createPaymentEntity("300", "desc",10000L, LocalDate.now().plusDays(1),
                "REF300", "GENERADA",
                PaymentStatus.GENERATED, "http://checkout");
        paymentRepository.save(payment);

        when(helipagosClient.getPayment(any()))
                .thenReturn(new HelipagosGetPaymentResponse(
                        300,
                        "GENERADA",
                        "REF300"
                ));

        mockMvc.perform(delete(BASE_URL + "/" + payment.getIdSp())
                        .header("x-api-key", apiKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id_sp").value("300"))
                .andExpect(jsonPath("$.referencia_externa").value("REF300"))
                .andExpect(jsonPath("$.estado_externo").value("VENCIDA"))
                .andExpect(jsonPath("$.estado_interno").value("CANCELLED"));
    }

    @Test
    void cancelPaymentShouldReturn401WhenApiKeyIsMissing() throws Exception {

        mockMvc.perform(delete(BASE_URL + "/300"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Access denied: authentication required"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void cancelPaymentShouldReturn401WhenApiKeyIsInvalid() throws Exception {

        mockMvc.perform(delete(BASE_URL + "/300")
                        .header("x-api-key", "invalid-api-key"))
                .andExpect(status().isUnauthorized());
    }

    private String obtainApiKey(String username, String password) throws Exception {
        RegisterRequest request  = new RegisterRequest(username, password);

        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(response);
        return jsonNode.get("apiKey").asText();
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
