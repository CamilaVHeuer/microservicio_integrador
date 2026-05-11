package com.camicompany.microserviciointegrador.client;

import com.camicompany.microserviciointegrador.dto.createPaymentDto.HelipagosCreatePaymentRequest;
import com.camicompany.microserviciointegrador.dto.createPaymentDto.HelipagosCreatePaymentResponse;
import com.camicompany.microserviciointegrador.dto.getPaymentDto.HelipagosGetPaymentResponse;
import com.camicompany.microserviciointegrador.exception.ExternalServiceBadRequestException;
import com.camicompany.microserviciointegrador.exception.ExternalServiceException;
import com.camicompany.microserviciointegrador.service.PaymentServiceImp;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class HelipagosClient {

  private final WebClient webClient;
  private static final Logger log = LoggerFactory.getLogger(PaymentServiceImp.class);

  @Value("${helipagos.base-url}")
  private String baseUrl;

  @Value("${helipagos.create-payment-path}")
  private String createPaymentPath;

  @Value("${helipagos.get-payment-path}")
  private String getPaymentPath;

  @Value("${helipagos.cancel-payment-path}")
  private String cancelPaymentPath;

  @Value("${helipagos.token}")
  private String token;

  public HelipagosClient(WebClient webClient) {
    this.webClient = webClient;
  }

  public HelipagosCreatePaymentResponse createPayment(HelipagosCreatePaymentRequest request) {

    try {
      return webClient
          .post()
          .uri(baseUrl + createPaymentPath)
          .header("Authorization", "Bearer " + token)
          .header("Content-Type", "application/json")
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
            "Invalid request to Helipagos: " + e.getResponseBodyAsString(), e);
      }
      throw new ExternalServiceException("Unexpected error from Helipagos", e);
    }
  }

  public HelipagosGetPaymentResponse getPayment(String idSp) {

    try {
      List<HelipagosGetPaymentResponse> responseList =
          webClient
              .post()
              .uri(baseUrl + getPaymentPath + "?id=" + idSp)
              .header("Authorization", "Bearer " + token)
              .header("Content-Type", "application/json")
              .retrieve()
              .bodyToMono(new ParameterizedTypeReference<List<HelipagosGetPaymentResponse>>() {})
              .block();
      if (responseList == null || responseList.isEmpty()) {
        throw new ExternalServiceException("Helipagos returned empty response for id: " + idSp);
      }
      return responseList.get(0);

    } catch (WebClientRequestException e) {
      throw new ExternalServiceException("Helipagos not available", e);

    } catch (WebClientResponseException e) {

      if (e.getStatusCode().is5xxServerError()) {
        throw new ExternalServiceException("Helipagos server error", e);
      }

      if (e.getStatusCode().is4xxClientError()) {
        throw new ExternalServiceBadRequestException(
            "Invalid request to Helipagos: " + e.getResponseBodyAsString(), e);
      }
      throw new ExternalServiceException("Unexpected error from Helipagos", e);
    }
  }

  public void cancelPayment(String idSp) {

    try {
      webClient
          .put()
          .uri(baseUrl + cancelPaymentPath + "?id=" + idSp)
          .header("Authorization", "Bearer " + token)
          .header("Content-Type", "application/json")
          .retrieve()
          .toBodilessEntity()
          .block();

    } catch (WebClientRequestException e) {
      throw new ExternalServiceException("Helipagos not available", e);

    } catch (WebClientResponseException e) {

      if (e.getStatusCode().is5xxServerError()) {
        throw new ExternalServiceException("Helipagos server error", e);
      }

      if (e.getStatusCode().is4xxClientError()) {
        throw new ExternalServiceBadRequestException(
            "Invalid cancel request to Helipagos: " + e.getResponseBodyAsString(), e);
      }
      throw new ExternalServiceException("Unexpected error from Helipagos", e);
    }
  }
}
