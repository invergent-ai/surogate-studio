package net.statemesh.service.dto.vllm;

import lombok.Data;

import java.util.List;

@Data
public class ChatMessageDTO {
    private String role;
    private Object content;
    private List<AttachedFileDTO> files;
}
