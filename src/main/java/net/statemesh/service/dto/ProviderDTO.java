package net.statemesh.service.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class ProviderDTO implements Serializable {
    private String id;
    private String name;
    private String description;
    private Boolean active;
}
