package net.statemesh.service.query;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.config.Constants;
import net.statemesh.domain.*;
import net.statemesh.repository.DatabaseRepository;
import net.statemesh.service.criteria.DatabaseCriteria;
import net.statemesh.service.dto.DatabaseDTO;
import net.statemesh.service.mapper.CycleAvoidingMappingContext;
import net.statemesh.service.mapper.DatabaseMapper;
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
public class DatabaseQueryService extends QueryService<Database> {
    private final DatabaseRepository databaseRepository;
    private final DatabaseMapper databaseMapper;

    @Transactional(readOnly = true)
    public Page<DatabaseDTO> findByCriteria(DatabaseCriteria criteria, Pageable page, String username) {
        log.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Database> specification = createSpecification(criteria, username);
        return databaseRepository.findAll(specification, page)
            .map(o -> databaseMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    protected Specification<Database> createSpecification(DatabaseCriteria criteria, String username) {
        Specification<Database> specification = Specification.where(null);

        /*
         * Default filtering
         */
        if (!StringUtils.isEmpty(username)) {
            specification = specification.and(
                buildSpecification(
                    new StringFilter().setEquals(username),
                    root -> root
                        .join(Database_.project, JoinType.LEFT)
                        .join(Project_.user, JoinType.LEFT)
                        .get(User_.login)
                )
            );
        }
        specification = specification.and(
            buildSpecification(
                new StringFilter().setEquals(Constants.STATE_MESH_ORGANIZATION),
                root -> root
                    .join(Database_.project, JoinType.LEFT)
                    .join(Project_.organization, JoinType.LEFT)
                    .get(Organization_.id)
            )
        );

        if (criteria != null) {
            Specification<Database> nameSpec = Specification.where(null);
            if (criteria.getName() != null && criteria.getName().getContains() != null) {
                nameSpec = nameSpec.or(buildStringSpecification(criteria.getName(), Database_.name));
            }

            // Only add the nameSpec if it contains any conditions
            specification = specification.and(nameSpec);

            // Apply the Project ID criteria using AND
            if (criteria.getProjectId() != null && criteria.getProjectId().getEquals() != null) {
                specification = specification.and(
                    buildSpecification(criteria.getProjectId(),
                        (Root<Database> root) -> root.join(Database_.project, JoinType.LEFT).get(Project_.id))
                );
            }
        }

        return specification;
    }
}
