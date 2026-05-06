package com.camicompany.microserviciointegrador.controller;

import com.camicompany.microserviciointegrador.dto.CreatePaymentRequest;
import com.camicompany.microserviciointegrador.dto.PaymentResponse;
import com.camicompany.microserviciointegrador.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        PaymentResponse paymentResponse = paymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentResponse);
    }

    @GetMapping("/{idSp}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable String idSp) {
        PaymentResponse paymentResponse = paymentService.getPayment(idSp);
        return ResponseEntity.ok(paymentResponse);
    }
}
