package net.statemesh.k8s.exception;

public class DockerSecretCreationException extends K8SException {
    public DockerSecretCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
