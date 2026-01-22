package net.statemesh.service.dto;

import lombok.*;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationBenchmarkDTO implements Serializable {
    private Long id;
    private String name;
    private String type;
    private Integer shots;
    private Set<String> selectedTasks = new HashSet<>();
}
