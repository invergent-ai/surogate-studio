package net.statemesh.web.rest.vm;

import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;

@Getter
public class FieldErrorVM implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String objectName;
    private final String field;
    private final String message;

    public FieldErrorVM(String dto, String field, String message) {
        this.objectName = dto;
        this.field = field;
        this.message = message;
    }
}
