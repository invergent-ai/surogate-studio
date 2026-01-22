package net.statemesh.service;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.statemesh.domain.VolumeMount;
import net.statemesh.repository.VolumeMountRepository;
import net.statemesh.service.dto.VolumeMountDTO;
import net.statemesh.service.mapper.CycleAvoidingMappingContext;
import net.statemesh.service.mapper.VolumeMountMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link net.statemesh.domain.VolumeMount}.
 */
@Service
@Transactional
public class VolumeMountService {
    private final Logger log = LoggerFactory.getLogger(VolumeMountService.class);

    private final VolumeMountRepository volumeMountRepository;
    private final VolumeMountMapper volumeMountMapper;

    public VolumeMountService(VolumeMountRepository volumeMountRepository, VolumeMountMapper volumeMountMapper) {
        this.volumeMountRepository = volumeMountRepository;
        this.volumeMountMapper = volumeMountMapper;
    }

    /**
     * Save a volumeMount.
     *
     * @param volumeMountDTO the entity to save.
     * @return the persisted entity.
     */
    public VolumeMountDTO save(VolumeMountDTO volumeMountDTO) {
        log.debug("Request to save VolumeMount : {}", volumeMountDTO);
        VolumeMount volumeMount = volumeMountMapper.toEntity(volumeMountDTO, new CycleAvoidingMappingContext());
        volumeMount = volumeMountRepository.save(volumeMount);
        return volumeMountMapper.toDto(volumeMount, new CycleAvoidingMappingContext());
    }

    /**
     * Update a volumeMount.
     *
     * @param volumeMountDTO the entity to save.
     * @return the persisted entity.
     */
    public VolumeMountDTO update(VolumeMountDTO volumeMountDTO) {
        log.debug("Request to update VolumeMount : {}", volumeMountDTO);
        VolumeMount volumeMount = volumeMountMapper.toEntity(volumeMountDTO, new CycleAvoidingMappingContext());
        volumeMount = volumeMountRepository.save(volumeMount);
        return volumeMountMapper.toDto(volumeMount, new CycleAvoidingMappingContext());
    }

    /**
     * Partially update a volumeMount.
     *
     * @param volumeMountDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<VolumeMountDTO> partialUpdate(VolumeMountDTO volumeMountDTO) {
        log.debug("Request to partially update VolumeMount : {}", volumeMountDTO);

        return volumeMountRepository
            .findById(volumeMountDTO.getId())
            .map(existingVolumeMount -> {
                volumeMountMapper.partialUpdate(existingVolumeMount, volumeMountDTO);

                return existingVolumeMount;
            })
            .map(volumeMountRepository::save)
            .map(o -> volumeMountMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    /**
     * Get all the volumeMounts.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<VolumeMountDTO> findAll() {
        log.debug("Request to get all VolumeMounts");
        return volumeMountRepository.findAll().stream()
            .map(o -> volumeMountMapper.toDto(o, new CycleAvoidingMappingContext()))
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one volumeMount by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<VolumeMountDTO> findOne(String id) {
        log.debug("Request to get VolumeMount : {}", id);
        return volumeMountRepository.findById(id)
            .map(o -> volumeMountMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    /**
     * Delete the volumeMount by id.
     *
     * @param id the id of the entity.
     */
    public void delete(String id) {
        log.debug("Request to delete VolumeMount : {}", id);
        volumeMountRepository.deleteById(id);
    }
}
