package net.statemesh.k8s.exception;

public class TektonPipelineCreationException extends K8SException {
    public TektonPipelineCreationException(String message) {
        super(message);
    }

    public TektonPipelineCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
