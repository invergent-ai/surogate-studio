package net.statemesh.service.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class AppTemplateDTO implements Serializable {
    private String id;
    private String name;
    private String description;
    private String longDescription;
    private String icon;
    private String template;
    private String category;
    private Integer zorder;
    private ProviderDTO provider;
    private String hashtags;
}
