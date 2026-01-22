package net.statemesh.k8s.exception;

public class IngressCreationException extends K8SException {
    public IngressCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
