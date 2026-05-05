package com.camicompany.microserviciointegrador.service;

import com.camicompany.microserviciointegrador.repository.PaymentRepository;
import org.springframework.stereotype.Service;

@Service
public class PaymentServiceImp implements PaymentService {

    private final PaymentRepository paymentRepository;

     public PaymentServiceImp(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

     // Implementar métodos de servicio relacionados con los pagos aquí
}
