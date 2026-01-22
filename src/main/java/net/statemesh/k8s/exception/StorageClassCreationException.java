package net.statemesh.k8s.exception;

public class StorageClassCreationException extends K8SException {
    public StorageClassCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
