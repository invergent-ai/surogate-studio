package net.statemesh.k8s.exception;

public class DeploymentCreationException extends K8SException {
    public DeploymentCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
