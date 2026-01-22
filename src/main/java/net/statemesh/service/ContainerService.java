    package net.statemesh.service;

    import java.util.LinkedList;
    import java.util.List;
    import java.util.Optional;
    import java.util.stream.Collectors;

    import lombok.RequiredArgsConstructor;
    import lombok.extern.slf4j.Slf4j;
    import net.statemesh.domain.Container;
    import net.statemesh.repository.ContainerRepository;
    import net.statemesh.service.dto.ContainerDTO;
    import net.statemesh.service.mapper.ContainerMapper;
    import net.statemesh.service.mapper.CycleAvoidingMappingContext;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;
    import org.springframework.stereotype.Service;
    import org.springframework.transaction.annotation.Transactional;

    /**
     * Service Implementation for managing {@link net.statemesh.domain.Container}.
     */
    @Service
    @Transactional
    @Slf4j
    @RequiredArgsConstructor
    public class ContainerService {
        private final ContainerRepository containerRepository;
        private final ContainerMapper containerMapper;

        /**
         * Save a container.
         *
         * @param containerDTO the entity to save.
         * @return the persisted entity.
         */
        public ContainerDTO save(ContainerDTO containerDTO) {
            log.debug("Request to save Container : {}", containerDTO);
            Container container = containerMapper.toEntity(containerDTO, new CycleAvoidingMappingContext());
            container = containerRepository.save(container);
            return containerMapper.toDto(container, new CycleAvoidingMappingContext());
        }

        /**
         * Update a container.
         *
         * @param containerDTO the entity to save.
         * @return the persisted entity.
         */
        public ContainerDTO update(ContainerDTO containerDTO) {
            log.debug("Request to update Container : {}", containerDTO);
            Container container = containerMapper.toEntity(containerDTO, new CycleAvoidingMappingContext());
            container = containerRepository.save(container);
            return containerMapper.toDto(container, new CycleAvoidingMappingContext());
        }

        /**
         * Partially update a container.
         *
         * @param containerDTO the entity to update partially.
         * @return the persisted entity.
         */
        public Optional<ContainerDTO> partialUpdate(ContainerDTO containerDTO) {
            log.debug("Request to partially update Container : {}", containerDTO);

            return containerRepository
                .findById(containerDTO.getId())
                .map(existingContainer -> {
                    containerMapper.partialUpdate(existingContainer, containerDTO);

                    return existingContainer;
                })
                .map(containerRepository::save)
                .map(o -> containerMapper.toDto(o, new CycleAvoidingMappingContext()));
        }

        /**
         * Get all the containers.
         *
         * @return the list of entities.
         */
        @Transactional(readOnly = true)
        public List<ContainerDTO> findAll() {
            log.debug("Request to get all Containers");
            return containerRepository.findAll().stream().map(o -> containerMapper.toDto(o, new CycleAvoidingMappingContext())).collect(Collectors.toCollection(LinkedList::new));
        }

        /**
         * Get one container by id.
         *
         * @param id the id of the entity.
         * @return the entity.
         */
        @Transactional(readOnly = true)
        public Optional<ContainerDTO> findOne(String id) {
            log.debug("Request to get Container : {}", id);
            return containerRepository.findById(id).map(o -> containerMapper.toDto(o, new CycleAvoidingMappingContext()));
        }

        @Transactional(readOnly = true)
        public List<ContainerDTO> findAllByApplication_Id(String applicationId) {
            log.debug("Request to get all Containers for application ID : {}", applicationId);
            return containerRepository.findAllByApplication_Id(applicationId)
                .stream()
                .map(o -> containerMapper.toDto(o, new CycleAvoidingMappingContext()))
                .collect(Collectors.toList());
        }

        /**
         * Delete the container by id.
         *
         * @param id the id of the entity.
         */
        public void delete(String id) {
            log.debug("Request to delete Container : {}", id);
            containerRepository.deleteById(id);
        }

        @Transactional(readOnly = true)
        public List<ContainerDTO> findAllByApplicationId(String applicationId) {
            log.debug("Request to get all Containers for podConfig ID : {}", applicationId);
            return containerRepository.findAllByApplication_Id(applicationId)
                .stream()
                .map(o -> containerMapper.toDto(o, new CycleAvoidingMappingContext()))
                .collect(Collectors.toCollection(LinkedList::new));
        }

        @Transactional(readOnly = true)
        public List<ContainerDTO> search(String query, String applicationId) {
            log.debug("Request to search Containers with query: {} for podConfig: {}", query, applicationId);
            if (query == null || query.trim().isEmpty()) {
                return findAllByApplicationId(applicationId);
            }

            String lowercaseQuery = query.toLowerCase();
            return containerRepository.findAllByApplication_Id(applicationId)
                .stream()
                .filter(container ->
                    container.getImageName().toLowerCase().contains(lowercaseQuery) ||
                        container.getType().toString().toLowerCase().contains(lowercaseQuery))
                .map(o -> containerMapper.toDto(o, new CycleAvoidingMappingContext()))
                .collect(Collectors.toCollection(LinkedList::new));
        }

    }
