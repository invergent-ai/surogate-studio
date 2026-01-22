package net.statemesh.service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;


@Getter
@Setter
@ToString(exclude = {"user"})
public class NodeReservationDTO implements Serializable {
    private String id;

    @NotNull
    private Instant created;
    private Instant updated;

    @Size(max = 255)
    private String accessToken;

    @NotNull
    @Size(max = 100)
    private String internalName;

    @NotNull
    private Instant expireTime;

    @NotNull
    @Size(max = 255)
    private String smId;

    @NotNull
    @Size(max = 255)
    private String shortSmId;

    @Size(max = 63)
    private String userKey;

    @Size(max = 50)
    private String zoneId;

    @Size(max = 50)
    private String machineId;

    @Size(max = 50)
    private String bootId;

    @Size(max = 20)
    private String ip;

    private Boolean deleted;
    private Boolean fulfilled;
    private UserDTO user;
    private NodeDTO node;

    private List<NodeReservationErrorDTO> errors;

    public NodeReservationDTO accessToken(String accessToken) {
        this.setAccessToken(accessToken);
        return this;
    }

    public NodeReservationDTO userKey(String userKey) {
        this.setUserKey(userKey);
        return this;
    }

    public NodeReservationDTO ip(String ip) {
        this.setIp(ip);
        return this;
    }

    public NodeReservationDTO fulfill() {
        this.setFulfilled(Boolean.TRUE);
        return this;
    }
}
