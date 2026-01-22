package net.statemesh.service.dto;

import lombok.Data;
import net.statemesh.domain.enumeration.ColorScheme;
import net.statemesh.domain.enumeration.MenuMode;

@Data
public class ThemeSettingsDTO {
    private String theme;
    private ColorScheme colorScheme;
    private Integer scale;
    private MenuMode menuMode;
    private Boolean ripple;
}
