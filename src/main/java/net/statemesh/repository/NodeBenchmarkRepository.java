package net.statemesh.repository;

import net.statemesh.domain.NodeBenchmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NodeBenchmarkRepository extends JpaRepository<NodeBenchmark, String>, JpaSpecificationExecutor<NodeBenchmark> {
    @Query("select nodeBenchmark from NodeBenchmark nodeBenchmark " +
        "where nodeBenchmark.nodeReservation.shortSmId = :shortSmId " +
        "order by nodeBenchmark.created desc " +
        "limit 1")
    Optional<NodeBenchmark> findLatestByShortSmId(@Param("shortSmId") String shortSmId);

    @Query("select nodeBenchmark from NodeBenchmark nodeBenchmark " +
        "where nodeBenchmark.nodeReservation.node.id = :nodeId " +
        "order by nodeBenchmark.created desc " +
        "limit 1")
    Optional<NodeBenchmark> findLatestByNodeId(@Param("nodeId") String nodeId);

    List<NodeBenchmark> findAllByNodeReservation_Id(String nodeReservationId);
}
