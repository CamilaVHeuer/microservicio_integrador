package com.camicompany.microserviciointegrador.service;

import com.camicompany.microserviciointegrador.dto.CreatePaymentRequest;
import com.camicompany.microserviciointegrador.dto.CreatePaymentResponseDto;
import com.camicompany.microserviciointegrador.mapper.PaymentMapper;
import com.camicompany.microserviciointegrador.repository.PaymentRepository;
import org.springframework.stereotype.Service;

@Service
public class PaymentServiceImp implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;

     public PaymentServiceImp(PaymentRepository paymentRepository, PaymentMapper paymentMapper) {
        this.paymentRepository = paymentRepository;
         this.paymentMapper = paymentMapper;
     }

    @Override
    public CreatePaymentResponseDto createPayment(CreatePaymentRequest request) {
        return null;
    }

}
