package net.statemesh.service.query;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.SingularAttribute;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.config.Constants;
import net.statemesh.domain.*;
import net.statemesh.domain.enumeration.ApplicationMode;
import net.statemesh.repository.ApplicationRepository;
import net.statemesh.service.criteria.ApplicationCriteria;
import net.statemesh.service.criteria.filter.ApplicationModeFilter;
import net.statemesh.service.dto.ApplicationDTO;
import net.statemesh.service.mapper.ApplicationMapper;
import net.statemesh.service.mapper.CycleAvoidingMappingContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;
import tech.jhipster.service.filter.StringFilter;

@Service
@Transactional(readOnly = true)
@Slf4j
@RequiredArgsConstructor
public class ApplicationQueryService extends QueryService<Application> {
    private final ApplicationRepository applicationRepository;
    private final ApplicationMapper applicationMapper;

    @Transactional(readOnly = true)
    public Page<ApplicationDTO> findByCriteria(ApplicationCriteria criteria, Pageable page, String username) {
        log.trace("find by criteria : {}, page: {}", criteria, page);
        final Specification<Application> specification = createSpecification(criteria, username);
        return applicationRepository.findAll(specification, page)
            .map(o -> applicationMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    protected Specification<Application> createSpecification(ApplicationCriteria criteria, String username) {
        Specification<Application> specification = Specification.where(null);

        /*
         * Default filtering
         */
        if (!StringUtils.isEmpty(username)) {
            specification = specification.and(
                buildSpecification(
                    new StringFilter().setEquals(username),
                    root -> root
                        .join(Application_.project, JoinType.LEFT)
                        .join(Project_.user, JoinType.LEFT)
                        .get(User_.login)
                )
            );
        }
        specification = specification.and(
            buildSpecification(
                new StringFilter().setEquals(Constants.STATE_MESH_ORGANIZATION),
                root -> root
                    .join(Application_.project, JoinType.LEFT)
                    .join(Project_.organization, JoinType.LEFT)
                    .get(Organization_.id)
            )
        );

        if (criteria != null) {
            Specification<Application> nameOrAliasSpec = Specification.where(null);

            // Combine name and alias using OR
            if (criteria.getName() != null && criteria.getName().getContains() != null) {
                nameOrAliasSpec = nameOrAliasSpec.or(buildStringSpecification(criteria.getName(), Application_.name));
            }
            if (criteria.getAlias() != null && criteria.getAlias().getContains() != null) {
                nameOrAliasSpec = nameOrAliasSpec.or(buildStringSpecification(criteria.getAlias(), Application_.alias));
            }

            // Only add the nameOrAliasSpec if it contains any conditions
            specification = specification.and(nameOrAliasSpec);

            // Apply the Project ID criteria using AND
            if (criteria.getProjectId() != null && criteria.getProjectId().getEquals() != null) {
                specification = specification.and(
                    buildSpecification(criteria.getProjectId(),
                        (Root<Application> root) -> root.join(Application_.project, JoinType.LEFT).get(Project_.id))
                );
            }

            // Apply the mode using AND
            if (criteria.getMode() != null && criteria.getMode().getEquals() != null) {
                specification = specification.and(
                    buildApplicationTypeSpecification(criteria.mode(), Application_.mode)
                );
            }
        }

        return specification;
    }

    protected Specification<Application> buildApplicationTypeSpecification(
        ApplicationModeFilter filter,
        SingularAttribute<Application, ApplicationMode> field) {
        return buildSpecification(filter, root -> root.get(field));
    }
}
