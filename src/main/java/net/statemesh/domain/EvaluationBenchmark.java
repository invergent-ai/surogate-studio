package net.statemesh.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "evaluation_benchmark")
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationBenchmark implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "shots")
    private Integer shots;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "evaluation_benchmark_tasks",
        joinColumns = @JoinColumn(name = "evaluation_benchmark_id")
    )
    @Column(name = "task_name", length = 255)
    private Set<String> selectedTasks = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluation_job_id")
    private EvaluationJob evaluationJob;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EvaluationBenchmark)) return false;
        return id != null && id.equals(((EvaluationBenchmark) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "EvaluationBenchmark{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", type='" + type + '\'' +
            ", shots=" + shots +
            ", selectedTasks=" + selectedTasks +
            '}';
    }
}
