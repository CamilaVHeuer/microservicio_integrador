package com.camicompany.microserviciointegrador.domain.payment;

public enum PaymentStatus {
    PENDING,
    GENERATED,
    PROCESSED,
    COMPLETED,
    FAILED,
    CANCELLED,
    ERROR
}
