package net.statemesh.node;

import lombok.*;

import java.util.List;

@Data
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class TaskBody {
    public static final int TaskTypeShell = 0;
    public static final int TaskTypeNetbench = 1;

    String id;
    Integer type;
    String payload;
    Long timeout; // in milliseconds
}



