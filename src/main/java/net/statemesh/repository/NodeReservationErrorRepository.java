package net.statemesh.repository;

import net.statemesh.domain.NodeReservationError;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NodeReservationErrorRepository extends JpaRepository<NodeReservationError, String>, JpaSpecificationExecutor<NodeReservationError> {
    List<NodeReservationError> findNodeReservationErrorByNodeReservationId(String nodeReservationId);
}
