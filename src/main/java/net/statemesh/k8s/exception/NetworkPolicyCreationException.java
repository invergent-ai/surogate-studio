package net.statemesh.k8s.exception;

public class NetworkPolicyCreationException extends K8SException {
    public NetworkPolicyCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
