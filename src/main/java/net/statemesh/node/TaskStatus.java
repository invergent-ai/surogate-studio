package net.statemesh.node;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class TaskStatus {
    public static final int ExitCodeUnknown = -255;
    public static final int ExitCodeNotFound = -254;
    public static final int ExitCodeTimeout = -253;
    public static final int ExitCodeError = -252;

    TaskBody task;
    String output;
    String error;
    Integer exitCode;
}
