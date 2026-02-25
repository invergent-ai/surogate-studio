package net.statemesh.web.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import net.statemesh.service.ProviderService;
import net.statemesh.service.dto.ProviderDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/provider")
@RequiredArgsConstructor
@Tag(name = "Provider", description = "Provider management")
public class ProviderResource {
    private final Logger log = LoggerFactory.getLogger(ProviderResource.class);

    private final ProviderService providerService;

    @Operation(summary = "Get all active providers")
    @ApiResponse(responseCode = "200", description = "List of active providers returned")
    @GetMapping("/active")
    public List<ProviderDTO> getActiveProviders() {
        log.debug("REST request to get all active providers");
        return providerService.findAllActive();
    }
}
