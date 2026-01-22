package net.statemesh.service.query;

import jakarta.persistence.criteria.JoinType;
import net.statemesh.config.Constants;
import net.statemesh.domain.*;
import net.statemesh.repository.NodeRepository;
import net.statemesh.service.criteria.NodeCriteria;
import net.statemesh.service.dto.NodeDTO;
import net.statemesh.service.mapper.CycleAvoidingMappingContext;
import net.statemesh.service.mapper.NodeMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;
import tech.jhipster.service.filter.BooleanFilter;
import tech.jhipster.service.filter.StringFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for executing complex queries for {@link Node} entities in the database.
 * The main input is a {@link NodeCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link List} of {@link NodeDTO} or a {@link Page} of {@link NodeDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class NodeQueryService extends QueryService<Node> {
    private final Logger log = LoggerFactory.getLogger(NodeQueryService.class);

    private final NodeRepository nodeRepository;
    private final NodeMapper nodeMapper;

    public NodeQueryService(NodeRepository nodeRepository,
                            NodeMapper nodeMapper) {
        this.nodeRepository = nodeRepository;
        this.nodeMapper = nodeMapper;
    }

    /**
     * Return a {@link Page} of {@link NodeDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page     The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<NodeDTO> findByCriteria(NodeCriteria criteria, Pageable page, String username) {
        log.trace("find by criteria : {}, page: {}", criteria, page);
        final Specification<Node> specification =  createSpecification(criteria, username);
        return nodeRepository.findAll(specification, page).map(o -> nodeMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(NodeCriteria criteria, String username) {
        log.trace("count by criteria : {}", criteria);
        final Specification<Node> specification = createSpecification(criteria, username);
        return nodeRepository.count(specification);
    }

    /**
     * Function to convert {@link NodeCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Node> createSpecification(NodeCriteria criteria, String username) {
        Specification<Node> specification = Specification.where(null);

        /*
         * Default filtering
         */
        if (!StringUtils.isEmpty(username)) {
            specification = specification.and(
                buildSpecification(
                    new StringFilter().setEquals(username),
                    root -> root
                        .join(Node_.user, JoinType.LEFT)
                        .get(User_.login)
                )
            );
        }
        specification = specification.and(
            buildSpecification(
                new StringFilter().setEquals(Constants.STATE_MESH_ORGANIZATION),
                root -> root
                    .join(Node_.cluster, JoinType.LEFT)
                    .join(Cluster_.zone, JoinType.LEFT)
                    .join(Zone_.organization, JoinType.LEFT)
                    .get(Organization_.id)
            )
        );
        specification = specification.and(
            buildSpecification(new BooleanFilter().setEquals(Boolean.FALSE), Node_.deleted).or(
            byFieldSpecified((root) -> root.get(Node_.deleted), Boolean.FALSE)
        ));

        List<Specification<Node>> fieldSpecs = new ArrayList<>();
        if (criteria.getName() != null) {
            fieldSpecs.add(buildStringSpecification(criteria.getName(), Node_.name));
        }
        if (criteria.getDescription() != null) {
            fieldSpecs.add(buildStringSpecification(criteria.getDescription(), Node_.description));
        }
        if (criteria.getDatacenterName() != null) {
            fieldSpecs.add(buildStringSpecification(criteria.getDatacenterName(), Node_.datacenterName));
        }
        specification = specification.and(Specification.anyOf(fieldSpecs));

        return specification;
    }
}
