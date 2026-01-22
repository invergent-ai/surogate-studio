package net.statemesh.service.k8s.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.kubernetes.client.openapi.ApiException;
import jakarta.persistence.EntityNotFoundException;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.config.ApplicationProperties;
import net.statemesh.domain.enumeration.ApplicationStatus;
import net.statemesh.k8s.KubernetesController;
import net.statemesh.k8s.flow.CreateModelFlow;
import net.statemesh.k8s.util.ResourceStatus;
import net.statemesh.service.ApplicationService;
import net.statemesh.service.ContainerService;
import net.statemesh.service.RayJobService;
import net.statemesh.service.TaskRunService;
import net.statemesh.service.dto.ApplicationDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.FutureUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

import static net.statemesh.config.Constants.*;
import static net.statemesh.config.K8Timeouts.READ_STATUS_TIMEOUT_SECONDS;

@Service("modelStatusService")
@Slf4j
public class ModelStatusService extends AppStatusService {

    public ModelStatusService(ApplicationService applicationService,
                              ContainerService containerService,
                              TaskRunService taskRunService,
                              RayJobService rayJobService,
                              KubernetesController kubernetesController,
                              ApplicationProperties applicationProperties,
                              MessageSource messageSource,
                              @Qualifier("statusScheduler")ThreadPoolTaskScheduler taskScheduler,
                              ObjectMapper objectMapper) {
        super(applicationService, containerService, taskRunService, rayJobService,
            kubernetesController, applicationProperties, messageSource, taskScheduler, objectMapper);
    }

    @Override
    protected void onTick(String... emId) throws InterruptedException, TimeoutException {
        Context rootCtx, routerCtx, workerCtx, cacheCtx;
        try {
            rootCtx = buildAppContext(emId[0], null, null);
            routerCtx = buildAppContext(CreateModelFlow.scaffoldRouterApplication(rootCtx.application(), objectMapper), null, null);
            workerCtx = buildAppContext(CreateModelFlow.scaffoldWorkerApplication(rootCtx.application(), objectMapper), null, null);
            cacheCtx = buildAppContext(CreateModelFlow.scaffoldCacheApplication(rootCtx.application(), objectMapper), null, null);
        } catch (EntityNotFoundException ex) {
            completeEmitters(true, "deleted", emId);
            return;
        }

        if (ApplicationStatus.BUILDING.equals(rootCtx.application().getStatus())) {
            log.trace("Application {} is BUILDING during poll; skipping this cycle", emId[0]);
            return; // skip until out of BUILDING; we chose not to auto-cancel
        }

        try {
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                var routerTask = FutureUtils.callAsync(() -> getAppStatus(routerCtx), executor);
                var workerTask = FutureUtils.callAsync(() -> getAppStatus(workerCtx), executor);
                var cacheTask = FutureUtils.callAsync(() -> getAppStatus(cacheCtx), executor);

                CompletableFuture.allOf(routerTask, workerTask, cacheTask)
                    .get(READ_STATUS_TIMEOUT_SECONDS * 3, TimeUnit.SECONDS);

                var routerResult = routerTask.get();
                var workerResult = workerTask.get();
                var cacheResult = cacheTask.get();

                List<ResourceStatus> routerStatuses = List.of(), workerStatuses = List.of(), cacheStatuses = List.of();
                if (routerResult != null) {
                    routerStatuses = updateAppStatusFromResources(routerCtx.application(), routerResult.getValue());
                }
                if (workerResult != null) {
                    workerStatuses = updateAppStatusFromResources(workerCtx.application(), workerResult.getValue());
                }
                if (cacheResult != null) {
                    cacheStatuses = updateAppStatusFromResources(cacheCtx.application(), cacheResult.getValue());
                }

                updateRootAppStatus(rootCtx.application(), routerCtx.application(), workerCtx.application(), cacheCtx.application());

                try {
                    sendEvent("modelstatus", ModelStatus.builder()
                            .applicationId(rootCtx.application().getId())
                            .status(rootCtx.application().getStatus())
                            .router(AppStatus.builder()
                                .applicationId(rootCtx.application().getId() + "-" + MODEL_COMPONENT_ROUTER)
                                .status(routerCtx.application().getStatus())
                                .resourceStatus(routerStatuses)
                                .build())
                            .worker(AppStatus.builder()
                                .applicationId(rootCtx.application().getId() + "-" + MODEL_COMPONENT_WORKER)
                                .status(workerCtx.application().getStatus())
                                .resourceStatus(workerStatuses)
                                .build())
                            .cache(AppStatus.builder()
                                .applicationId(rootCtx.application().getId() + "-" + MODEL_COMPONENT_CACHE)
                                .status(cacheCtx.application().getStatus())
                                .resourceStatus(cacheStatuses)
                                .build())
                            .build(),
                        emId);
                } catch (Exception e) {
                    log.error("Failed to send status update to SSE for application {}", emId, e);
                }
            }
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof ApiException)) {
                throw new RuntimeException(e);
            } else {
                log.trace("Error reading app status with message {}", e.getMessage());
            }
        }
    }

    private void updateRootAppStatus(ApplicationDTO rootApp, ApplicationDTO... childApps) {
        if (ApplicationStatus.DELETING.equals(rootApp.getStatus())) {
            Arrays.stream(childApps).forEach(childApp -> childApp.setStatus(ApplicationStatus.DELETING));
        } else if (Arrays.stream(childApps).anyMatch(app -> ApplicationStatus.ERROR.equals(app.getStatus()))) {
            // any error dominates
            rootApp.setStatus(ApplicationStatus.ERROR);
            applicationService.updateStatus(rootApp.getId(), ApplicationStatus.ERROR);
        } else if (Arrays.stream(childApps).anyMatch(app -> ApplicationStatus.INITIALIZED.equals(app.getStatus()))) {
            // any initialized -> initialized
            rootApp.setStatus(ApplicationStatus.INITIALIZED);
            applicationService.updateStatus(rootApp.getId(), ApplicationStatus.INITIALIZED);
        } else if (Arrays.stream(childApps).anyMatch(app -> ApplicationStatus.DEPLOYING.equals(app.getStatus()))) {
            // any deploying -> deploying
            rootApp.setStatus(ApplicationStatus.DEPLOYING);
            applicationService.updateStatus(rootApp.getId(), ApplicationStatus.DEPLOYING);
            return;
        } else if (Arrays.stream(childApps).anyMatch(app -> ApplicationStatus.DEPLOYED.equals(app.getStatus()))) {
            // any deployed -> deployed
            rootApp.setStatus(ApplicationStatus.DEPLOYED);
            applicationService.updateStatus(rootApp.getId(), ApplicationStatus.DEPLOYED);
        } else if (Arrays.stream(childApps).allMatch(app -> ApplicationStatus.CREATED.equals(app.getStatus()))) {
            // common statuses
            if (!ApplicationStatus.DEPLOYING.equals(rootApp.getStatus())) {
                rootApp.setStatus(ApplicationStatus.CREATED);
                applicationService.updateStatus(rootApp.getId(), ApplicationStatus.CREATED);
            } else {
                Arrays.stream(childApps).forEach(childApp -> childApp.setStatus(ApplicationStatus.DEPLOYING));
            }
        } else {
            rootApp.setStatus(null);
            applicationService.updateStatus(rootApp.getId(), null);
        }
    }

    @Data
    @Builder
    static class ModelStatus {
        String applicationId;
        ApplicationStatus status;
        AppStatus router;
        AppStatus worker;
        AppStatus cache;
    }
}
