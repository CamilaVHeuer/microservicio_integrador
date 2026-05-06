package com.camicompany.microserviciointegrador.exception;

public class StatusConflictException extends RuntimeException {
    public StatusConflictException(String message) {
        super(message);
    }
}
