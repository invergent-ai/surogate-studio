package net.statemesh.k8s.exception;

public class TektonTaskCreationException extends K8SException {
    public TektonTaskCreationException(String message) {
        super(message);
    }

    public TektonTaskCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
