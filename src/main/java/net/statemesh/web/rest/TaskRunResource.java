package net.statemesh.web.rest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.service.TaskRunService;
import net.statemesh.service.criteria.TaskRunCriteria;
import net.statemesh.service.dto.TaskRunDTO;
import net.statemesh.service.query.TaskRunQueryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Slf4j
public class TaskRunResource {
    private final TaskRunQueryService taskQueryService;
    private final TaskRunService taskRunService;
    private Optional<TaskRunDTO> taskRunDTO;

    @PostMapping("")
    public ResponseEntity<TaskRunDTO> save(@RequestBody TaskRunDTO taskRunDTO,
                                           Principal principal) {
        TaskRunDTO result = taskRunService.save(taskRunDTO, principal.getName());
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/cancel/{id}")
    public void cancel(@PathVariable("id") String taskId) {
        taskRunService.cancel(taskId);
    }


    @GetMapping
    public ResponseEntity<List<TaskRunDTO>> queryTasks(
        TaskRunCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        Principal principal
    ) {
        Page<TaskRunDTO> resultPage = taskQueryService.findByCriteria(criteria, pageable, principal.getName());
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), resultPage);
        return ResponseEntity.ok().headers(headers).body(resultPage.getContent());
    }

    @PostMapping("/submit")
    public ResponseEntity<TaskRunDTO> submitTask(@RequestBody TaskRunDTO task, Principal principal) {
        return ResponseEntity.ok(taskRunService.submit(task, principal.getName()));
    }

    @PostMapping("/redeploy")
    public ResponseEntity<TaskRunDTO> redeploy(@RequestBody TaskRunDTO task, Principal principal) {
        return ResponseEntity.ok(taskRunService.redeploy(task, principal.getName()));
    }

    @DeleteMapping("/delete/{id}")
    public void delete(@PathVariable("id") String taskId, Principal principal) {
        taskRunService.delete(taskId, principal.getName());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskRunDTO> getTaskRun(@PathVariable("id") String taskId) {
        Optional<TaskRunDTO> taskRunDTO = taskRunService.findOne(taskId);
        return ResponseUtil.wrapOrNotFound(taskRunDTO);
    }
}
