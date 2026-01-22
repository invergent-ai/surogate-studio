package net.statemesh.service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

@Getter
@Setter
@Builder
public class NodeReservationErrorDTO implements Serializable {
    private String id;

    @NotNull
    private Instant created;

    private String error;

    private String errors;
}
