package com.camicompany.microserviciointegrador.service;

import com.camicompany.microserviciointegrador.dto.CreatePaymentRequest;
import com.camicompany.microserviciointegrador.dto.CreatePaymentResponseDto;

public interface PaymentService {

    CreatePaymentResponseDto createPayment(CreatePaymentRequest request);

}
