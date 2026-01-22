package net.statemesh.k8s.exception;

public class RoleBindingCreationException extends RuntimeException {
    public RoleBindingCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
