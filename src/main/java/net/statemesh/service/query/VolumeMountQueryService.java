package net.statemesh.service.query;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.config.Constants;
import net.statemesh.domain.*;
import net.statemesh.repository.VolumeMountRepository;
import net.statemesh.service.criteria.VolumeMountCriteria;
import net.statemesh.service.dto.VolumeMountDTO;
import net.statemesh.service.mapper.CycleAvoidingMappingContext;
import net.statemesh.service.mapper.VolumeMountMapper;
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
public class VolumeMountQueryService extends QueryService<VolumeMount> {
    private final VolumeMountRepository volumeMountRepository;
    private final VolumeMountMapper volumeMountMapper;

    @Transactional(readOnly = true)
    public Page<VolumeMountDTO> findByCriteria(VolumeMountCriteria criteria, Pageable page, String username) {
        final Specification<VolumeMount> specification = createSpecification(criteria, username);
        return volumeMountRepository.findAll(specification, page)
            .map(o -> volumeMountMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    protected Specification<VolumeMount> createSpecification(VolumeMountCriteria criteria, String username) {
        Specification<VolumeMount> specification = Specification.where(null);

        /*
         * Default filtering
         */
        if (!StringUtils.isEmpty(username)) {
            specification = specification.and(
                buildSpecification(
                    new StringFilter().setEquals(username),
                    root -> root
                        .join(VolumeMount_.volume, JoinType.LEFT)
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
                    .join(VolumeMount_.volume, JoinType.LEFT)
                    .join(Volume_.project, JoinType.LEFT)
                    .join(Project_.organization, JoinType.LEFT)
                    .get(Organization_.id)
            )
        );

        if (criteria != null) {
            if (criteria.getProjectId() != null && criteria.getProjectId().getEquals() != null) {
                specification = specification.and(
                    buildSpecification(criteria.getProjectId(),
                        (Root<VolumeMount> root) -> root
                            .join(VolumeMount_.volume, JoinType.LEFT)
                            .join(Volume_.project, JoinType.LEFT)
                            .get(Project_.id))
                );
            }

            if (criteria.getVolumeId() != null && criteria.getVolumeId().getEquals() != null) {
                specification = specification.and(
                    buildSpecification(criteria.getVolumeId(),
                        (Root<VolumeMount> root) -> root
                            .join(VolumeMount_.volume, JoinType.LEFT)
                            .get(Volume_.id))
                );
            }
        }

        return specification;
    }
}
