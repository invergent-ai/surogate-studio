package net.statemesh.service.lakefs;

public class LakeFsException extends RuntimeException {
    public LakeFsException(String message) {
        super(message);
    }
    public LakeFsException(String message, Throwable cause) {
        super(message, cause);
    }
}
