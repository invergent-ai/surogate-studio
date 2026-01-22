package net.statemesh.repository;

import net.statemesh.domain.Node;
import net.statemesh.domain.enumeration.NodeStatus;
import net.statemesh.domain.projections.NodeStatusProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the Node entity.
 */
@Repository
public interface NodeRepository extends JpaRepository<Node, String>, JpaSpecificationExecutor<Node> {
    default Optional<Node> findOneWithEagerRelationships(String id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Node> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Node> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }
    Optional<Node> findByInternalName(String internalName);

    @Query(
        value = "select node from Node node left join fetch node.cluster left join fetch node.user",
        countQuery = "select count(node) from Node node"
    )
    Page<Node> findAllWithToOneRelationships(Pageable pageable);

    @Query("select node from Node node left join fetch node.cluster left join fetch node.user")
    List<Node> findAllWithToOneRelationships();

    @Query("select node from Node node left join fetch node.cluster left join fetch node.user where node.id =:id")
    Optional<Node> findOneWithToOneRelationships(@Param("id") String id);

    @Query("select node from Node node left join fetch node.cluster cluster " +
        "where node.internalName =:internalName and (node.deleted is NULL or node.deleted = FALSE) " +
        "and cluster.cid =:clusterId")
    Optional<Node> findByInternalNameAndClusterCid(@Param("internalName") String internalName,
                                                   @Param("clusterId") String clusterId);

    @Query("select node from Node node left join node.cluster cluster " +
        "where (node.deleted is NULL or node.deleted = FALSE) " +
        "and cluster.cid =:clusterId")
    List<Node> findByClusterCid(@Param("clusterId") String clusterId);

    @Query("select node from Node node left join node.cluster cluster left join node.user user " +
        "where (node.deleted is NULL or node.deleted = FALSE) " +
        "and cluster.cid =:clusterId and user.id =:userId")
    List<Node> findByClusterCidAndUserId(@Param("clusterId") String clusterId,
                                         @Param("userId") String userId);

    @Query("select node from Node node left join fetch node.user user " +
        "where (node.deleted is NULL or node.deleted = FALSE) " +
        "and user.id =:userId")
    List<Node> findAllByUser_Id(@Param("userId") String userId);

    Long countAllByUser_Id(String userId);

    @Query("select median(node.hourlyPrice) from Node node left join node.resource resource " +
        "where abs(resource.capacityCpu - :cpu) < 2 and abs(resource.capacityMemory - :memory) < 4096")
    Double computeSimilarPrice(@Param("cpu") BigDecimal cpu,
                               @Param("memory") BigDecimal memory);

    @Modifying
    @Query("update Node node set node.status =:status where node.id =:id")
    void updateNodeStatus(@Param("id") String id, @Param("status") NodeStatus status);

    @Modifying
    @Query("update Node node set node.lastStartTime =:startTime where node.id =:id")
    void updateStartTime(@Param("id") String id, @Param("startTime") Instant startTime);

    @Modifying
    @Query("update Node node set node.datacenterName =:datacenterName where node.id =:id")
    void updateDatacenterName(@Param("id") String id, @Param("datacenterName") String datacenterName);

    @Modifying
    @Query("update Node node set node.rayCluster =:rayCluster where node.id =:id")
    void updateRayCluster(@Param("id") String id, @Param("rayCluster") String rayCluster);

    @Query("select node from Node node where node.id =:id")
    Optional<NodeStatusProjection> findOneById(@Param("id") String id);

    List<Node> findAllByDeletedIsNullAndStatus(NodeStatus status);
}
