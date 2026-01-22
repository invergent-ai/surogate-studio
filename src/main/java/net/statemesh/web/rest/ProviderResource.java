package net.statemesh.web.rest;

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
public class ProviderResource {
    private final Logger log = LoggerFactory.getLogger(ProviderResource.class);

    private final ProviderService providerService;

    @GetMapping("/active")
    public List<ProviderDTO> getActiveProviders() {
        log.debug("REST request to get all active providers");
        return providerService.findAllActive();
    }
}
