package net.statemesh.web.rest.k8s;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import net.statemesh.service.dto.LineDTO;
import net.statemesh.service.dto.vllm.VllmChatRequestDTO;
import net.statemesh.service.k8s.TestChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "Test Chat", description = "Test chat sessions for model inference jobs")
public class TestChatResource {
    private final Logger log = LoggerFactory.getLogger(TestChatResource.class);
    private final TestChatService testChatService;
    private final ObjectMapper objectMapper;

    @Operation(summary = "Start chat session", description = "Start a test chat session for a job")
    @ApiResponse(responseCode = "200", description = "Chat session started")
    @GetMapping("/job/{jobId}")
    public ResponseEntity<Void> startChat(
        @Parameter(description = "Job ID") @PathVariable(name = "jobId") String jobId) {
        log.debug("REST request to start chat for job {}", jobId);
        testChatService.startChat(jobId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Stop chat session")
    @ApiResponse(responseCode = "200", description = "Chat session stopped")
    @DeleteMapping("/job/{jobId}")
    public ResponseEntity<Void> stopChat(
        @Parameter(description = "Job ID") @PathVariable(name = "jobId") String jobId) {
        log.debug("REST request to stop chat for job {}", jobId);
        testChatService.stopChat(jobId);
        return ResponseEntity.ok().build();
    }

    @MessageMapping("/message")
    public void sendChatMessage(@Payload String rawPayload) {
        try {
            JsonNode json = objectMapper.readTree(rawPayload);

            if (json.has("abort") && json.get("abort").asBoolean()) {
                String applicationId = json.get("applicationId").asText();
                log.info("Abort request received for {}", applicationId);
                testChatService.abortVllmStream(applicationId);
                return;
            }

            if (json.has("applicationId")) {
                VllmChatRequestDTO vllmRequest = objectMapper.readValue(rawPayload, VllmChatRequestDTO.class);
                testChatService.sendVllmMessage(vllmRequest);
            } else if (json.has("jobId")) {
                LineDTO testRequest = objectMapper.readValue(rawPayload, LineDTO.class);
                testChatService.sendMessage(testRequest);
            }

        } catch (Exception e) {
            log.error("❌ Failed to process message", e);
        }
    }
}
