package net.statemesh.service.dto.vllm;

import lombok.Data;

@Data
public class AttachedFileDTO {
    private String name;
    private Long size;
    private String type;  // "image", "audio", "video"
    private String base64;
    private String mimeType;
}
