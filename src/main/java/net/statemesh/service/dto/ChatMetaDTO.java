package net.statemesh.service.dto;

import lombok.*;

import java.io.Serializable;

@Setter
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ChatMetaDTO implements Serializable {
    private String status;
    private String pid;
    private String cmd;
    private String running;
    private String error;
}
