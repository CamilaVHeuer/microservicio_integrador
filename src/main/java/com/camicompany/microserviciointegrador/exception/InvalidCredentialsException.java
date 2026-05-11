package com.camicompany.microserviciointegrador.exception;

public class InvalidCredentialsException extends RuntimeException {
  public InvalidCredentialsException(String message) {
    super(message);
  }
}
