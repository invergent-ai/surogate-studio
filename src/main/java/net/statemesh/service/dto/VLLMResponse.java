package net.statemesh.service.dto;

import lombok.*;

import java.io.Serializable;
import java.util.List;

@Setter
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class VLLMResponse implements Serializable {
    private String id;
    private String model;
    private List<Choice> choices;

    @Data
    public static class Choice {
        private Integer index;
        private Message message;
    }

    @Data
    public static class Message {
        private String role;
        private String content;
    }
}
