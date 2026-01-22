package net.statemesh.k8s.exception;

public class ServiceCreationException extends K8SException {
    public ServiceCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
