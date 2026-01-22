package net.statemesh.k8s.exception;

public class RoleCreationException extends RuntimeException {
    public RoleCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
