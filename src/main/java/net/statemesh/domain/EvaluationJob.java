package net.statemesh.domain;

import jakarta.persistence.*;
import lombok.*;
import net.statemesh.domain.enumeration.EvaluationJobStatus;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "evaluation_job")
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationJob implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_name", nullable = false)
    private String runName;

    @Column(name = "base_model", nullable = false)
    private String baseModel;

    @Embedded
    private RefSelection baseModelRef;

    @Column(name = "description")
    private String description;

    @Column(name = "language")
    private String language;

    @Column(name = "judge_model")
    private String judgeModel;

    @Column(name = "judge_model_api")
    private String judgeModelApi;

    @Column(name = "use_gateway")
    private Boolean useGateway;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private EvaluationJobStatus status;

    @Builder.Default
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "evaluation_job_notify",
        joinColumns = @JoinColumn(name = "evaluation_job_id")
    )
    @Column(name = "notify_value", length = 255)
    private Set<String> notify = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "evaluationJob", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<EvaluationBenchmark> benchmarks = new HashSet<>();
}
