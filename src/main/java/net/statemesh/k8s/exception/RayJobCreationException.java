package net.statemesh.k8s.exception;

public class RayJobCreationException extends K8SException {
    public RayJobCreationException(String message) {
        super(message);
    }

    public RayJobCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
