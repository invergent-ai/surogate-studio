package net.statemesh.service;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.statemesh.domain.Annotation;
import net.statemesh.repository.AnnotationRepository;
import net.statemesh.service.dto.AnnotationDTO;
import net.statemesh.service.mapper.AnnotationMapper;
import net.statemesh.service.mapper.CycleAvoidingMappingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link net.statemesh.domain.Annotation}.
 */
@Service
@Transactional
public class AnnotationService {
    private final Logger log = LoggerFactory.getLogger(AnnotationService.class);

    private final AnnotationRepository annotationRepository;
    private final AnnotationMapper annotationMapper;

    public AnnotationService(AnnotationRepository annotationRepository, AnnotationMapper annotationMapper) {
        this.annotationRepository = annotationRepository;
        this.annotationMapper = annotationMapper;
    }

    /**
     * Save a annotation.
     *
     * @param annotationDTO the entity to save.
     * @return the persisted entity.
     */
    public AnnotationDTO save(AnnotationDTO annotationDTO) {
        log.debug("Request to save Annotation : {}", annotationDTO);
        Annotation annotation = annotationMapper.toEntity(annotationDTO, new CycleAvoidingMappingContext());
        annotation = annotationRepository.save(annotation);
        return annotationMapper.toDto(annotation, new CycleAvoidingMappingContext());
    }

    /**
     * Update a annotation.
     *
     * @param annotationDTO the entity to save.
     * @return the persisted entity.
     */
    public AnnotationDTO update(AnnotationDTO annotationDTO) {
        log.debug("Request to update Annotation : {}", annotationDTO);
        Annotation annotation = annotationMapper.toEntity(annotationDTO, new CycleAvoidingMappingContext());
        annotation = annotationRepository.save(annotation);
        return annotationMapper.toDto(annotation, new CycleAvoidingMappingContext());
    }

    /**
     * Partially update a annotation.
     *
     * @param annotationDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<AnnotationDTO> partialUpdate(AnnotationDTO annotationDTO) {
        log.debug("Request to partially update Annotation : {}", annotationDTO);

        return annotationRepository
            .findById(annotationDTO.getId())
            .map(existingAnnotation -> {
                annotationMapper.partialUpdate(existingAnnotation, annotationDTO);

                return existingAnnotation;
            })
            .map(annotationRepository::save)
            .map(o -> annotationMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    /**
     * Get all the annotations.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<AnnotationDTO> findAll() {
        log.debug("Request to get all Annotations");
        return annotationRepository.findAll().stream()
            .map(o -> annotationMapper.toDto(o, new CycleAvoidingMappingContext()))
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one annotation by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<AnnotationDTO> findOne(String id) {
        log.debug("Request to get Annotation : {}", id);
        return annotationRepository.findById(id).map(o -> annotationMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    /**
     * Delete the annotation by id.
     *
     * @param id the id of the entity.
     */
    public void delete(String id) {
        log.debug("Request to delete Annotation : {}", id);
        annotationRepository.deleteById(id);
    }
}
