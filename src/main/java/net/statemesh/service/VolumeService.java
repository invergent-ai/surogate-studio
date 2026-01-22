package net.statemesh.service;

import lombok.RequiredArgsConstructor;
import net.statemesh.domain.Volume;
import net.statemesh.repository.ProjectRepository;
import net.statemesh.repository.VolumeRepository;
import net.statemesh.service.dto.VolumeDTO;
import net.statemesh.service.mapper.CycleAvoidingMappingContext;
import net.statemesh.service.mapper.VolumeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service Implementation for managing {@link net.statemesh.domain.Volume}.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class VolumeService {
    private final Logger log = LoggerFactory.getLogger(VolumeService.class);

    private final VolumeRepository volumeRepository;
    private final VolumeMapper volumeMapper;
    private final ProjectRepository projectRepository;

    /**
     * Save a volume.
     *
     * @param volumeDTO the entity to save.
     * @return the persisted entity.
     */
    public VolumeDTO save(VolumeDTO volumeDTO) {
        log.debug("Request to save Volume : {}", volumeDTO);
        Volume volume = volumeMapper.toEntity(volumeDTO, new CycleAvoidingMappingContext());
        volume = volumeRepository.save(volume);
        return volumeMapper.toDto(volume, new CycleAvoidingMappingContext());
    }

    /**
     * Update a volume.
     *
     * @param volumeDTO the entity to save.
     * @return the persisted entity.
     */
    public VolumeDTO update(VolumeDTO volumeDTO) {
        log.debug("Request to update Volume : {}", volumeDTO);
        Volume volume = volumeMapper.toEntity(volumeDTO, new CycleAvoidingMappingContext());
        volume = volumeRepository.save(volume);
        return volumeMapper.toDto(volume, new CycleAvoidingMappingContext());
    }

    /**
     * Partially update a volume.
     *
     * @param volumeDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<VolumeDTO> partialUpdate(VolumeDTO volumeDTO) {
        log.debug("Request to partially update Volume : {}", volumeDTO);

        return volumeRepository
            .findById(volumeDTO.getId())
            .map(existingVolume -> {
                if (existingVolume.getProject() != null && volumeDTO.getProject() != null &&
                    !existingVolume.getProject().getId().equals(volumeDTO.getProject().getId())) {
                    projectRepository.findById(volumeDTO.getProject().getId()).ifPresent(existingVolume::setProject);
                    volumeDTO.setProject(null);
                }
                volumeMapper.partialUpdate(existingVolume, volumeDTO);

                return existingVolume;
            })
            .map(volumeRepository::save)
            .map(o -> volumeMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    /**
     * Get all the volumes.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<VolumeDTO> findAll() {
        log.debug("Request to get all Volumes");
        return volumeRepository.findAll().stream()
            .map(o -> volumeMapper.toDto(o, new CycleAvoidingMappingContext())).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one volume by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<VolumeDTO> findOne(String id) {
        log.debug("Request to get Volume : {}", id);
        return volumeRepository.findById(id)
            .map(o -> volumeMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    /**
     * Delete the volume by id.
     *
     * @param id the id of the entity.
     */
    public void delete(String id) {
        log.debug("Request to delete Volume : {}", id);
        volumeRepository.deleteById(id);
    }
}
