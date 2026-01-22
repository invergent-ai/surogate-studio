package net.statemesh.k8s.exception;

public class K8SException extends RuntimeException {
    public K8SException(String message) {
        super(message);
    }
    public K8SException(String message, Throwable cause) {
        super(message, cause);
    }
}
