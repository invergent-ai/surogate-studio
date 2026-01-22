package net.statemesh.k8s.exception;

public class VolumeCreationException extends K8SException {
    public VolumeCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
