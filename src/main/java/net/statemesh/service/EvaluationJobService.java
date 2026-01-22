package net.statemesh.service;

import net.statemesh.domain.EvaluationBenchmark;
import net.statemesh.domain.EvaluationJob;
import net.statemesh.repository.EvaluationJobRepository;
import net.statemesh.service.dto.EvaluationJobDTO;
import net.statemesh.service.mapper.CycleAvoidingMappingContext;
import net.statemesh.service.mapper.EvaluationJobMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class EvaluationJobService {

    private final EvaluationJobRepository evaluationJobRepository;
    private final EvaluationJobMapper evaluationJobMapper;

    public EvaluationJobService(EvaluationJobRepository evaluationJobRepository, EvaluationJobMapper evaluationJobMapper) {
        this.evaluationJobRepository = evaluationJobRepository;
        this.evaluationJobMapper = evaluationJobMapper;
    }

    public EvaluationJobDTO save(EvaluationJobDTO dto) {
        EvaluationJob job = evaluationJobMapper.toEntity(dto, new CycleAvoidingMappingContext());
        if (job.getBenchmarks() != null) {
            for (EvaluationBenchmark b : job.getBenchmarks()) {
                b.setEvaluationJob(job);
            }
        }
        job = evaluationJobRepository.save(job);
        return evaluationJobMapper.toDto(job, new CycleAvoidingMappingContext());
    }

    public EvaluationJobDTO update(EvaluationJobDTO dto) {
        EvaluationJob job = evaluationJobMapper.toEntity(dto, new CycleAvoidingMappingContext());
        if (job.getBenchmarks() != null) {
            for (EvaluationBenchmark b : job.getBenchmarks()) {
                b.setEvaluationJob(job);
            }
        }
        job = evaluationJobRepository.save(job);
        return evaluationJobMapper.toDto(job, new CycleAvoidingMappingContext());
    }

    @Transactional
    public Page<EvaluationJobDTO> findAll(Pageable pageable) {
        return evaluationJobRepository.findAll(pageable).map(job -> evaluationJobMapper.toDto(job, new CycleAvoidingMappingContext()));
    }

    @Transactional
    public Optional<EvaluationJobDTO> findOne(Long id) {
        return evaluationJobRepository.findById(id).map(job -> evaluationJobMapper.toDto(job, new CycleAvoidingMappingContext()));
    }

    public void delete(Long id) {
        evaluationJobRepository.deleteById(id);
    }
}
