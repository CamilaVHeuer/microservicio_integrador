package com.camicompany.microserviciointegrador.client;

import com.camicompany.microserviciointegrador.dto.HelipagosCreatePaymentRequest;
import com.camicompany.microserviciointegrador.dto.HelipagosCreatePaymentResponse;
import com.camicompany.microserviciointegrador.exception.ExternalServiceBadRequestException;
import com.camicompany.microserviciointegrador.exception.ExternalServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class HelipagosClient {

    private final WebClient webClient;

    @Value("${helipagos.base-url}")
    private String baseUrl;

    @Value("${helipagos.create-payment-path}")
    private String createPaymentPath;

    @Value("${helipagos.token}")
    private String token;

    public HelipagosClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public HelipagosCreatePaymentResponse createPayment(HelipagosCreatePaymentRequest request) {

        try {
            return webClient.post()
                    .uri(baseUrl + createPaymentPath)
                    .header("Authorization", "Bearer " + token)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(HelipagosCreatePaymentResponse.class)
                    .block();

        } catch (WebClientRequestException e) {
            // timeout / connection / DNS
            throw new ExternalServiceException("Helipagos not available", e);

        } catch (WebClientResponseException e) {

            if (e.getStatusCode().is5xxServerError()) {
                // error on the Helipagos side
                throw new ExternalServiceException("Helipagos server error", e);
            }

            if (e.getStatusCode().is4xxClientError()) {
                // error in my request
                throw new ExternalServiceBadRequestException(
                        "Invalid request to Helipagos: " + e.getResponseBodyAsString(), e
                );
            }

            throw new ExternalServiceException("Unexpected error from Helipagos", e);
        }
    }
}
