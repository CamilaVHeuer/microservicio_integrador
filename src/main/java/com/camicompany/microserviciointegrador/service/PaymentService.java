package com.camicompany.microserviciointegrador.service;

import com.camicompany.microserviciointegrador.dto.createPaymentDto.CreatePaymentRequest;
import com.camicompany.microserviciointegrador.dto.weebhookDto.HelipagosWebhookRequest;
import com.camicompany.microserviciointegrador.dto.PaymentResponse;

public interface PaymentService {

    PaymentResponse createPayment(CreatePaymentRequest request);

    PaymentResponse getPayment(String idSp);

    PaymentResponse cancelPayment(String idSp);

    void processWebhook(HelipagosWebhookRequest request, String apiKey);


}
