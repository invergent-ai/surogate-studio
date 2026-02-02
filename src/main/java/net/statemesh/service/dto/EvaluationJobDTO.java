package net.statemesh.service.dto;

import lombok.*;
import net.statemesh.domain.RefSelection;
import net.statemesh.domain.enumeration.EvaluationJobStatus;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationJobDTO implements Serializable {
    private Long id;
    private String runName;
    private String baseModel;
    private RefSelection baseModelRef;
    private String description;
    private String language;
    private String judgeModel;
    private String judgeModelApi;
    private Boolean useGateway;
    private EvaluationJobStatus status;

    @Builder.Default
    private Set<String> notify = new HashSet<>();

    @Builder.Default
    private Set<EvaluationBenchmarkDTO> benchmarks = new HashSet<>();
}
