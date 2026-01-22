package net.statemesh.k8s.exception;

public class RouterDeploymentCreationException extends K8SException {
    public RouterDeploymentCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
