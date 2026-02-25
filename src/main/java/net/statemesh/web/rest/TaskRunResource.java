package net.statemesh.web.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Task Run", description = "Task run management")
public class TaskRunResource {
    private final TaskRunQueryService taskQueryService;
    private final TaskRunService taskRunService;

    @Operation(summary = "Save a task run")
    @ApiResponse(responseCode = "200", description = "Task run saved")
    @PostMapping("")
    public ResponseEntity<TaskRunDTO> save(@RequestBody TaskRunDTO taskRunDTO,
                                           Principal principal) {
        TaskRunDTO result = taskRunService.save(taskRunDTO, principal.getName());
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Cancel a task run")
    @ApiResponse(responseCode = "200", description = "Task run cancelled")
    @DeleteMapping("/cancel/{id}")
    public void cancel(@PathVariable("id") String taskId) {
        taskRunService.cancel(taskId);
    }

    @Operation(summary = "Query task runs by criteria")
    @ApiResponse(responseCode = "200", description = "List of task runs returned")
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

    @Operation(summary = "Submit a task run for execution")
    @ApiResponse(responseCode = "200", description = "Task run submitted")
    @PostMapping("/submit")
    public ResponseEntity<TaskRunDTO> submitTask(@RequestBody TaskRunDTO task, Principal principal) {
        return ResponseEntity.ok(taskRunService.submit(task, principal.getName()));
    }

    @Operation(summary = "Redeploy a task run")
    @ApiResponse(responseCode = "200", description = "Task run redeployed")
    @PostMapping("/redeploy")
    public ResponseEntity<TaskRunDTO> redeploy(@RequestBody TaskRunDTO task, Principal principal) {
        return ResponseEntity.ok(taskRunService.redeploy(task, principal.getName()));
    }

    @Operation(summary = "Delete a task run")
    @ApiResponse(responseCode = "204", description = "Task run deleted")
    @DeleteMapping("/delete/{id}")
    public void delete(@PathVariable("id") String taskId, Principal principal) {
        taskRunService.delete(taskId, principal.getName());
    }

    @Operation(summary = "Get a task run by ID")
    @ApiResponse(responseCode = "200", description = "Task run returned")
    @ApiResponse(responseCode = "404", description = "Task run not found")
    @GetMapping("/{id}")
    public ResponseEntity<TaskRunDTO> getTaskRun(@PathVariable("id") String taskId) {
        Optional<TaskRunDTO> taskRunDTO = taskRunService.findOne(taskId);
        return ResponseUtil.wrapOrNotFound(taskRunDTO);
    }
}
