package net.statemesh.k8s.exception;

public class NamespaceCreationException extends K8SException {
    public NamespaceCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
