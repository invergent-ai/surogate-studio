package net.statemesh.service.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class RefDTO {
    private String id;
    private String commitId;
    private Map<String, String> metadata;
}
