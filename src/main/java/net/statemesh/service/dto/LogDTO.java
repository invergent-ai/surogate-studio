package net.statemesh.service.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.Instant;

@Setter
@Getter
@ToString
public class LogDTO implements Serializable {
    private Long id;
    private Instant timestamp;
    private String message;
    private Long applicationId;
    private String applicationName;
    private String source;
}
