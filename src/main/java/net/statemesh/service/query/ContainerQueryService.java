package net.statemesh.service.query;

import net.statemesh.domain.Container;
import net.statemesh.domain.Container_;
import net.statemesh.domain.Application_;
import net.statemesh.repository.ContainerRepository;
import net.statemesh.service.criteria.ContainerCriteria;
import net.statemesh.service.dto.ContainerDTO;
import net.statemesh.service.mapper.ContainerMapper;
import net.statemesh.service.mapper.CycleAvoidingMappingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;
import jakarta.persistence.criteria.JoinType;

@Service
@Transactional(readOnly = true)
public class ContainerQueryService extends QueryService<Container> {
    private final Logger log = LoggerFactory.getLogger(ContainerQueryService.class);

    private final ContainerRepository containerRepository;
    private final ContainerMapper containerMapper;

    public ContainerQueryService(ContainerRepository containerRepository,
                                 ContainerMapper containerMapper) {
        this.containerRepository = containerRepository;
        this.containerMapper = containerMapper;
    }

    @Transactional(readOnly = true)
    public Page<ContainerDTO> findByCriteria(ContainerCriteria criteria, Pageable page) {
        log.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Container> specification = createSpecification(criteria);
        return containerRepository.findAll(specification, page).map(o -> containerMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    protected Specification<Container> createSpecification(ContainerCriteria criteria) {
        Specification<Container> specification = Specification.where(null);
        if (criteria != null) {
            if (criteria.getApplicationId() != null) {
                specification = specification.and(
                    buildSpecification(
                        criteria.getApplicationId(),
                        root -> root.join(Container_.application, JoinType.LEFT).get(Application_.id)
                    )
                );
            }

            // Only filter by imageName
            if (criteria.getImageName() != null) {
                specification = specification.and(buildStringSpecification(criteria.getImageName(), Container_.imageName));
            }
        }
        return specification;
    }
}
