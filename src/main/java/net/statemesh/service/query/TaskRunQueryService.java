package net.statemesh.service.query;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.config.Constants;
import net.statemesh.domain.*;
import net.statemesh.repository.TaskRunRepository;
import net.statemesh.service.criteria.TaskRunCriteria;
import net.statemesh.service.dto.TaskRunDTO;
import net.statemesh.service.mapper.CycleAvoidingMappingContext;
import net.statemesh.service.mapper.TaskRunMapper;
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
public class TaskRunQueryService extends QueryService<TaskRun> {
    private final TaskRunRepository taskRunRepository;
    private final TaskRunMapper taskRunMapper;

    @Transactional(readOnly = true)
    public Page<TaskRunDTO> findByCriteria(TaskRunCriteria criteria, Pageable page, String username) {
        final Specification<TaskRun> specification = createSpecification(criteria, username);
        return taskRunRepository.findAll(specification, page)
            .map(o -> taskRunMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    protected Specification<TaskRun> createSpecification(TaskRunCriteria criteria, String username) {
        Specification<TaskRun> specification = Specification.where(null);

        /*
         * Default filtering
         */
        if (!StringUtils.isEmpty(username)) {
            specification = specification.and(
                buildSpecification(
                    new StringFilter().setEquals(username),
                    root -> root
                        .join(TaskRun_.project, JoinType.LEFT)
                        .join(Project_.user, JoinType.LEFT)
                        .get(User_.login)
                )
            );
        }
        specification = specification.and(
            buildSpecification(
                new StringFilter().setEquals(Constants.STATE_MESH_ORGANIZATION),
                root -> root
                    .join(TaskRun_.project, JoinType.LEFT)
                    .join(Project_.organization, JoinType.LEFT)
                    .get(Organization_.id)
            )
        );

        if (criteria != null) {
            Specification<TaskRun> nameOrAliasSpec = Specification.where(null);

            // Combine name and alias using OR
            if (criteria.getName() != null && criteria.getName().getContains() != null) {
                nameOrAliasSpec = nameOrAliasSpec.or(buildStringSpecification(criteria.getName(), TaskRun_.name));
            }
            // Only add the nameOrAliasSpec if it contains any conditions
            specification = specification.and(nameOrAliasSpec);

            // Apply the Project ID criteria using AND
            if (criteria.getProjectId() != null && criteria.getProjectId().getEquals() != null) {
                specification = specification.and(
                    buildSpecification(criteria.getProjectId(),
                        (Root<TaskRun> root) -> root.join(TaskRun_.project, JoinType.LEFT).get(Project_.id))
                );
            }

            // Apply the type using AND
            if (criteria.getType() != null && criteria.getType().getIn() != null) {
                specification = specification.and(
                    buildSpecification(criteria.getType(), root -> root.get(TaskRun_.type))
                );
            }
            // Apply the provisioningStatus using AND
            if (criteria.getProvisioningStatus() != null && criteria.getProvisioningStatus().getIn() != null) {
                specification = specification.and(
                    buildSpecification(criteria.getProvisioningStatus(), root -> root.get(TaskRun_.provisioningStatus))
                );
            }
            // Apply the startTime using AND
            if (criteria.getStartTime() != null && criteria.getStartTime().getGreaterThan() != null) {
                specification = specification.and(
                    buildSpecification(criteria.getStartTime(), root -> root.get(TaskRun_.startTime))
                );
            }
        }

        return specification;
    }
}
