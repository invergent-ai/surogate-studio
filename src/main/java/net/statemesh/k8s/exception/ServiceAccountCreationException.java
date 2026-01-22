package net.statemesh.k8s.exception;

public class ServiceAccountCreationException extends RuntimeException {
    public ServiceAccountCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
