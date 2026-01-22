package net.statemesh.service.dto;

import lombok.Data;

@Data
public class ValidationS3ResponseDTO {
    private boolean valid;
    private String message;
}
