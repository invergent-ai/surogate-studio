package net.statemesh.service.exception;

public class Web3Exception extends Exception {
    public Web3Exception(String message) {
        super(message);
    }

    public Web3Exception(Exception e) {
        super(e.getMessage());
    }
}
