package net.statemesh.node;

import lombok.*;

import java.util.List;

@Data
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ShellTaskBody {
    String command;
    List<String> env;
}
