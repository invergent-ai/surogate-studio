package net.statemesh.service;

import lombok.RequiredArgsConstructor;
import net.statemesh.domain.AppTemplate;
import net.statemesh.repository.AppTemplateRepository;
import net.statemesh.service.dto.AppTemplateDTO;
import net.statemesh.service.mapper.AppTemplateMapper;
import net.statemesh.service.mapper.CycleAvoidingMappingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service Implementation for managing {@link net.statemesh.domain.AppTemplate}.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class AppTemplateService {
    private final Logger log = LoggerFactory.getLogger(AppTemplateService.class);

    private final AppTemplateRepository appTemplateRepository;
    private final AppTemplateMapper appTemplateMapper;

    /**
     * Save an app template.
     */
    public AppTemplateDTO save(AppTemplateDTO appTemplateDTO) {
        log.debug("Request to save app template : {}", appTemplateDTO);
        AppTemplate appTemplate = appTemplateMapper.toEntity(appTemplateDTO, new CycleAvoidingMappingContext());
        appTemplate = appTemplateRepository.save(appTemplate);
        return appTemplateMapper.toDto(appTemplate, new CycleAvoidingMappingContext());
    }

    /**
     * Update an app template.
     */
    public AppTemplateDTO update(AppTemplateDTO appTemplateDTO) {
        log.debug("Request to update app template : {}", appTemplateDTO);
        AppTemplate appTemplate = appTemplateMapper.toEntity(appTemplateDTO, new CycleAvoidingMappingContext());
        appTemplate = appTemplateRepository.save(appTemplate);
        return appTemplateMapper.toDto(appTemplate, new CycleAvoidingMappingContext());
    }

    /**
     * Get all the app templates.
     */
    @Transactional(readOnly = true)
    public List<AppTemplateDTO> findAll() {
        log.debug("Request to get all app templates");
        return appTemplateRepository.findAll().stream()
            .map(c -> appTemplateMapper.toDto(c, new CycleAvoidingMappingContext()))
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get all app templates with filtering and sorting.
     */
    @Transactional(readOnly = true)
    public List<AppTemplateDTO> findAllWithFilters(String search, String category, String providerId, String sortBy, String sortOrder) {
        log.debug("Request to get app templates with filters - search: {}, category: {}, providerId: {}, sortBy: {}, sortOrder: {}",
            search, category, providerId, sortBy, sortOrder);

        List<AppTemplate> templates = appTemplateRepository.findWithFilters(search, category, providerId);

        List<AppTemplateDTO> templateDTOs = templates.stream()
            .map(template -> appTemplateMapper.toDto(template, new CycleAvoidingMappingContext()))
            .collect(Collectors.toList());

        return sortTemplates(templateDTOs, sortBy, sortOrder);
    }

    @Transactional(readOnly = true)
    public List<String> findAllCategories() {
        log.debug("Request to get all categories");
        return appTemplateRepository.findAllDistinctCategories();
    }

    /**
     * Get one app template by id.
     */
    @Transactional(readOnly = true)
    public Optional<AppTemplateDTO> findOne(String id) {
        log.debug("Request to get app template : {}", id);
        return appTemplateRepository.findById(id)
            .map(c -> appTemplateMapper.toDto(c, new CycleAvoidingMappingContext()));
    }

    /**
     * Delete the app template by id.
     */
    public void delete(String id) {
        log.debug("Request to delete app template: {}", id);
        appTemplateRepository.deleteById(id);
    }

    private List<AppTemplateDTO> sortTemplates(List<AppTemplateDTO> templates, String sortBy, String sortOrder) {
        if (templates.isEmpty()) {
            return templates;
        }

        Comparator<AppTemplateDTO> comparator = getComparator(sortBy);
        if ("desc".equalsIgnoreCase(sortOrder)) {
            comparator = comparator.reversed();
        }

        return templates.stream()
            .sorted(comparator)
            .collect(Collectors.toList());
    }

    private Comparator<AppTemplateDTO> getComparator(String sortBy) {
        return switch (sortBy.toLowerCase()) {
            case "name" -> Comparator.comparing(template ->
                template.getName() != null ? template.getName().toLowerCase() : "");
            case "category" -> Comparator.comparing(template ->
                template.getCategory() != null ? template.getCategory().toLowerCase() : "");
            default ->
                // Default to name sorting
                Comparator.comparing(template ->
                    template.getName() != null ? template.getName().toLowerCase() : "");
        };
    }
}
