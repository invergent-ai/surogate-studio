package net.statemesh.service.query;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.config.Constants;
import net.statemesh.domain.*;
import net.statemesh.domain.enumeration.VolumeType;
import net.statemesh.repository.VolumeRepository;
import net.statemesh.service.criteria.VolumeCriteria;
import net.statemesh.service.criteria.filter.VolumeTypeFilter;
import net.statemesh.service.dto.VolumeDTO;
import net.statemesh.service.mapper.CycleAvoidingMappingContext;
import net.statemesh.service.mapper.VolumeMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;
import tech.jhipster.service.filter.StringFilter;

import java.util.List;

@Service
@Transactional(readOnly = true)
@Slf4j
@RequiredArgsConstructor
public class VolumeQueryService extends QueryService<Volume> {
    private final VolumeRepository volumeRepository;
    private final VolumeMapper volumeMapper;

    @Transactional(readOnly = true)
    public Page<VolumeDTO> findByCriteria(VolumeCriteria criteria, Pageable page, String username) {
        final Specification<Volume> specification = createSpecification(criteria, username);
        return volumeRepository.findAll(specification, page)
            .map(o -> volumeMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    protected Specification<Volume> createSpecification(VolumeCriteria criteria, String username) {
        Specification<Volume> specification = Specification.where(null);

        /*
         * Default filtering
         */
        if (!StringUtils.isEmpty(username)) {
            specification = specification.and(
                buildSpecification(
                    new StringFilter().setEquals(username),
                    root -> root
                        .join(Volume_.project, JoinType.LEFT)
                        .join(Project_.user, JoinType.LEFT)
                        .get(User_.login)
                )
            );
        }

        specification = specification.and(
            buildSpecification(
                new StringFilter().setEquals(Constants.STATE_MESH_ORGANIZATION),
                root -> root
                    .join(Volume_.project, JoinType.LEFT)
                    .join(Project_.organization, JoinType.LEFT)
                    .get(Organization_.id)
            )
        );

        if (criteria != null) {
            Specification<Volume> nameSpec = Specification.where(null);

            // Add name
            if (criteria.getName() != null && criteria.getName().getContains() != null) {
                nameSpec = nameSpec.or(buildStringSpecification(criteria.getName(), Volume_.name));
            }

            // Only add the nameOrAliasSpec if it contains any conditions
            specification = specification.and(nameSpec);

            // Apply the Project ID criteria using AND
            if (criteria.getProjectId() != null && criteria.getProjectId().getEquals() != null) {
                specification = specification.and(
                    buildSpecification(criteria.getProjectId(),
                        (Root<Volume> root) -> root.join(Volume_.project, JoinType.LEFT).get(Project_.id))
                );
            }
        }

        specification = specification.and(
            buildSpecification(
                new VolumeTypeFilter().setIn(List.of(VolumeType.PERSISTENT, VolumeType.TEMPORARY)),
                root -> root.get(Volume_.type)
            )
        );

        return specification;
    }
}
