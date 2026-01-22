package net.statemesh.service.dto.pub;

import lombok.Data;

import java.io.Serializable;

@Data
public class PublicClusterDTO implements Serializable {
    private PublicZoneDTO zone;
}
