package com.camicompany.microserviciointegrador.service;

import com.camicompany.microserviciointegrador.dto.CreatePaymentRequest;
import com.camicompany.microserviciointegrador.dto.PaymentResponse;

public interface PaymentService {

    PaymentResponse createPayment(CreatePaymentRequest request);

    PaymentResponse getPayment(String idSp);

}
