package com.camicompany.microserviciointegrador.exception;

public class ExternalServiceBadRequestException extends RuntimeException {

    public ExternalServiceBadRequestException(String message) {
        super(message);
    }

    public ExternalServiceBadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
