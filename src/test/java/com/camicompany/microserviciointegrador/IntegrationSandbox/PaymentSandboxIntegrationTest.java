package com.camicompany.microserviciointegrador.IntegrationSandbox;

import com.camicompany.microserviciointegrador.dto.createPaymentDto.CreatePaymentRequest;
import com.camicompany.microserviciointegrador.dto.registerUserDto.RegisterRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDate;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@Tag("sandbox")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("sandbox")
public class PaymentSandboxIntegrationTest {

    private static final String BASE_URL = "/api/v1/payments";
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createPaymentShouldCreateRealPaymentInSandbox() throws Exception {

        String username = "user1_" + System.currentTimeMillis();
        String apiKey = obtainApiKey(username, "Password1");
        CreatePaymentRequest req = new CreatePaymentRequest(10000L, "Pago sandbox", LocalDate.now().plusDays(1),
                "REF" + System.currentTimeMillis());


        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-API-KEY", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").exists())
                .andExpect(jsonPath("$.id_sp").exists())
                .andExpect(jsonPath("$.referencia_externa").value(req.referenciaExterna()))
                .andExpect(jsonPath("$.estado_externo").value("GENERADA"))
                .andExpect(jsonPath("$.estado_interno").value("GENERATED"))
                .andExpect(jsonPath("$.checkout_url").exists());


    }
    @Test
    void getPaymentShouldReturnRealPaymentFromSandbox() throws Exception {
        String username = "user1_" + System.currentTimeMillis();
        String apiKey = obtainApiKey(username, "Password1");

        String id_sp = createSandboxPayment(apiKey);

        mockMvc.perform(get(BASE_URL + "/" + id_sp)
                        .header("X-API-KEY", apiKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").exists())
                .andExpect(jsonPath("$.id_sp").value(id_sp))
                .andExpect(jsonPath("$.estado_externo").exists())
                .andExpect(jsonPath("$.estado_interno").exists())
                .andExpect(jsonPath("$.checkout_url").exists());
    }

    @Test
    void cancelPaymentShouldCancelRealPaymentInSandbox() throws Exception {
        String username = "user1_" + System.currentTimeMillis();
        String apiKey = obtainApiKey(username, "Password1");

        String id_sp= createSandboxPayment(apiKey);

        mockMvc.perform(delete(BASE_URL + "/" + id_sp)
                        .header("X-API-KEY", apiKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").exists())
                .andExpect(jsonPath("$.id_sp").value(id_sp))
                .andExpect(jsonPath("$.estado_externo").value("VENCIDA"))
                .andExpect(jsonPath("$.estado_interno").value("CANCELLED"));
    }


    private String createSandboxPayment(String apiKey) throws Exception {

        CreatePaymentRequest req = new CreatePaymentRequest(
                10000L,
                "Pago sandbox helper",
                LocalDate.now().plusDays(1),
                "REF" + System.currentTimeMillis()
        );

        String response = mockMvc.perform(post(BASE_URL)
                        .header("X-API-KEY", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(response);

        return jsonNode.get("id_sp").asText();
    }

    private String obtainApiKey(String username, String password) throws Exception {
        RegisterRequest request  = new RegisterRequest(username, password);

        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(response);
        return jsonNode.get("apiKey").asText();
    }



}
