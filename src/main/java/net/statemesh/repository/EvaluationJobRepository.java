package net.statemesh.repository;

import net.statemesh.domain.EvaluationJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EvaluationJobRepository extends JpaRepository<EvaluationJob, Long> {
}
