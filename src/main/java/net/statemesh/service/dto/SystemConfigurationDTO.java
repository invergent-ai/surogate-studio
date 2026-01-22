package net.statemesh.service.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * A DTO for the {@link net.statemesh.domain.SystemConfiguration} entity.
 */
@Data
public class SystemConfigurationDTO implements Serializable {
    private String id;
    private String webDomain;
}
