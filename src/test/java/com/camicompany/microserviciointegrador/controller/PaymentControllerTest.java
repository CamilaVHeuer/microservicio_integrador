package com.camicompany.microserviciointegrador.controller;

import com.camicompany.microserviciointegrador.dto.HelipagosWebhookRequest;
import com.camicompany.microserviciointegrador.exception.ResourceNotFoundException;
import com.camicompany.microserviciointegrador.security.ApiKeyFilter;
import com.camicompany.microserviciointegrador.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.camicompany.microserviciointegrador.dto.PaymentResponse;
import com.camicompany.microserviciointegrador.dto.createPaymentDto.CreatePaymentRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.LocalDate;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import com.camicompany.microserviciointegrador.exception.ExternalServiceBadRequestException;

@WebMvcTest(controllers = PaymentController.class, excludeAutoConfiguration =  SecurityAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
public class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private ApiKeyFilter apiKeyFilter;

    private static final String BASE_URL = "/api/v1/payments";

    @Test
        void createPaymentShouldReturn201() throws Exception {
        CreatePaymentRequest req = new CreatePaymentRequest(10000L, "Pago de servicios",
                LocalDate.now().plusDays(1), "REF123456");
        PaymentResponse resp = new PaymentResponse(1L, "1", "REF123456",
                "GENERATED", "GENERADA", "url");
        when(paymentService.createPayment(any())).thenReturn(resp);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id_sp").value("1"))
                .andExpect(jsonPath("$.referencia_externa").value("REF123456"))
                .andExpect(jsonPath("$.estado_interno").value("GENERATED"))
                .andExpect(jsonPath("$.estado_externo").value("GENERADA"))
                .andExpect(jsonPath("$.checkout_url").value("url"));
        verify(paymentService).createPayment(any(CreatePaymentRequest.class));
    }

    @Test
        void createPaymentWithMissingFieldsShouldReturn400() throws Exception {
        // All fields null
        CreatePaymentRequest req = new CreatePaymentRequest(null, null, null, null);
        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
        void createPaymentWithInvalidReferenciaExternaShouldReturn400() throws Exception {
        CreatePaymentRequest req = new CreatePaymentRequest(10000L, "Pago de servicios", LocalDate.now().plusDays(1), "");
        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value(containsString("The external reference cannot be blank")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
        void createPaymentWithInvalidDescripcionShouldReturn400() throws Exception {
        CreatePaymentRequest req = new CreatePaymentRequest(10000L, "", LocalDate.now().plusDays(1), "REF123456");
        mockMvc.perform(post("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value(containsString("The description cannot be blank")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
        void createPaymentWithInvalidAmountShouldReturn400() throws Exception {
        CreatePaymentRequest req = new CreatePaymentRequest(null, "Pago de servicios", LocalDate.now().plusDays(1), "REF123456");
        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value(containsString("The amount cannot be null")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
        void createPaymentWithInvalidFechaVtoShouldReturn400() throws Exception {
        CreatePaymentRequest req = new CreatePaymentRequest(10000L, "Pago de servicios", null, "REF123456");
        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value(containsString("The expiration date cannot be null")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
        void createPaymentWithFechaVtoInPastShouldReturn400() throws Exception {
        CreatePaymentRequest req = new CreatePaymentRequest(10000L, "Pago de servicios", LocalDate.now().minusDays(1), "REF123456");
        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value(containsString("The expiration date cannot be earlier than today")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
        void getPaymentShouldReturn200() throws Exception {
        PaymentResponse resp = new PaymentResponse(1L, "1", "REF123456", "GENERATED", "GENERADA", "url");
        when(paymentService.getPayment("1")).thenReturn(resp);
        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id_sp").value("1"))
                .andExpect(jsonPath("$.referencia_externa").value("REF123456"))
                .andExpect(jsonPath("$.estado_interno").value("GENERATED"))
                .andExpect(jsonPath("$.estado_externo").value("GENERADA"))
                .andExpect(jsonPath("$.checkout_url").value("url"));

    }

    @Test
        void getPaymentNotFoundShouldReturn404() throws Exception {
        when(paymentService.getPayment("1")).thenThrow(new ResourceNotFoundException("Payment not found with id_sp: 99"));
        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Payment not found with id_sp: 99"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
        void cancelPaymentShouldReturn200() throws Exception {
        PaymentResponse resp = new PaymentResponse(1L, "1", "REF123456", "CANCELLED", "VENCIDA", "url");
        when(paymentService.cancelPayment("1")).thenReturn(resp);
        mockMvc.perform(MockMvcRequestBuilders.delete( BASE_URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id_sp").value("1"))
                .andExpect(jsonPath("$.referencia_externa").value("REF123456"))
                .andExpect(jsonPath("$.estado_interno").value("CANCELLED"))
                .andExpect(jsonPath("$.estado_externo").value("VENCIDA"))
                .andExpect(jsonPath("$.checkout_url").value("url"));
                ;
    }

    @Test
        void cancelPaymentNotFoundShouldReturn404() throws Exception {
        when(paymentService.cancelPayment("1")).thenThrow(new ResourceNotFoundException("Payment not found with id_sp: 99"));
        mockMvc.perform(delete(BASE_URL + "/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Payment not found with id_sp: 99"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void webhookShouldReturn200() throws Exception {
        HelipagosWebhookRequest req = new HelipagosWebhookRequest(1, "PROCESADA",
                "REF123",null, null, null
        );
        mockMvc.perform(post(BASE_URL +"/webhook")
                        .header("api-key", "test-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
        verify(paymentService).processWebhook(any(HelipagosWebhookRequest.class), eq("test-api-key"));
    }

    @Test
    void webhookShouldReturn400WhenApiKeyIsMissing() throws Exception {
        HelipagosWebhookRequest req = new HelipagosWebhookRequest(1, "PROCESADA",
                "REF123",null, null, null
        );
        mockMvc.perform(post(BASE_URL +"/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void webhookShouldReturn400WhenApiKeyIsInvalid() throws Exception {
        HelipagosWebhookRequest req = new HelipagosWebhookRequest(1, "PROCESADA",
                "REF123",null, null, null
        );
        doThrow(new ExternalServiceBadRequestException("Invalid webhook api-key")).when(paymentService).
                processWebhook(any(HelipagosWebhookRequest.class), eq("test-api-key"));
        mockMvc.perform(post(BASE_URL +"/webhook")
                        .header("api-key", "test-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Invalid webhook api-key"))
                .andExpect(jsonPath("$.timestamp").exists());
    }



    

    
}
