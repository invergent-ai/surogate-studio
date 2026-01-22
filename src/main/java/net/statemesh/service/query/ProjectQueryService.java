package net.statemesh.service.query;

import jakarta.persistence.criteria.JoinType;
import net.statemesh.config.Constants;
import net.statemesh.domain.*;
import net.statemesh.repository.ProjectRepository;
import net.statemesh.service.criteria.ProjectCriteria;
import net.statemesh.service.dto.ProjectDTO;
import net.statemesh.service.mapper.CycleAvoidingMappingContext;
import net.statemesh.service.mapper.ProjectMapper;
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
 * Service for executing complex queries for {@link Project} entities in the database.
 * The main input is a {@link ProjectCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link List} of {@link ProjectDTO} or a {@link Page} of {@link ProjectDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class ProjectQueryService extends QueryService<Project> {
    private final Logger log = LoggerFactory.getLogger(ProjectQueryService.class);

    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;

    public ProjectQueryService(ProjectRepository projectRepository,
                               ProjectMapper projectMapper) {
        this.projectRepository = projectRepository;
        this.projectMapper = projectMapper;
    }

    /**
     * Return a {@link Page} of {@link ProjectDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page     The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<ProjectDTO> findByCriteria(ProjectCriteria criteria, Pageable page, String username) {
        log.trace("find by criteria : {}, page: {}", criteria, page);
        final Specification<Project> specification =  createSpecification(criteria, username);
        return projectRepository.findAll(specification, page).map(o -> projectMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(ProjectCriteria criteria, String username) {
        log.trace("count by criteria : {}", criteria);
        final Specification<Project> specification = createSpecification(criteria, username);
        return projectRepository.count(specification);
    }

    /**
     * Function to convert {@link ProjectCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Project> createSpecification(ProjectCriteria criteria, String username) {
        Specification<Project> specification = Specification.where(null);

        /*
         * Default filtering
         */
        if (!StringUtils.isEmpty(username)) {
            specification = specification.and(
                buildSpecification(
                    new StringFilter().setEquals(username),
                    root -> root
                        .join(Project_.user, JoinType.LEFT)
                        .get(User_.login)
                )
            );
        }
        specification = specification.and(
            buildSpecification(
                new StringFilter().setEquals(Constants.STATE_MESH_ORGANIZATION),
                root -> root
                    .join(Project_.organization, JoinType.LEFT)
                    .get(Organization_.id)
            )
        );
        specification = specification.and(
            buildSpecification(new BooleanFilter().setEquals(Boolean.FALSE), Project_.deleted).or(
                byFieldSpecified((root) -> root.get(Project_.deleted), Boolean.FALSE)
            ));

        if (criteria.getZoneId() != null) {
            specification = specification.and(
                buildSpecification(
                        criteria.getZoneId(),
                        root -> root
                        .join(Project_.cluster, JoinType.LEFT)
                        .join(Cluster_.zone, JoinType.LEFT)
                        .get(Zone_.zoneId))
                    .or(
                    byFieldSpecified((root) -> root.get(Project_.cluster), Boolean.FALSE)
                ));
        }

        List<Specification<Project>> fieldSpecs = new ArrayList<>();
        if (criteria.getName() != null) {
            fieldSpecs.add(buildStringSpecification(criteria.getName(), Project_.name));
        }
        if (criteria.getAlias() != null) {
            fieldSpecs.add(buildStringSpecification(criteria.getAlias(), Project_.alias));
        }
        if (criteria.getDescription() != null) {
            fieldSpecs.add(buildStringSpecification(criteria.getDescription(), Project_.description));
        }
        specification = specification.and(Specification.anyOf(fieldSpecs));

        return specification;
    }
}
