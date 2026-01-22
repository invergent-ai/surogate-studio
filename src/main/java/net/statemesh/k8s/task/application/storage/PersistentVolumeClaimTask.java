package net.statemesh.k8s.task.application.storage;

import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.*;
import net.statemesh.k8s.exception.SkippedExistsException;
import net.statemesh.k8s.task.*;
import net.statemesh.k8s.util.ApiStub;
import net.statemesh.service.dto.VolumeDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

import static net.statemesh.k8s.util.K8SConstants.*;
import static net.statemesh.k8s.util.NamingUtils.pvcName;

public class PersistentVolumeClaimTask extends BaseMutationTask<Void> {
    private final Logger log = LoggerFactory.getLogger(PersistentVolumeClaimTask.class);

    private final VolumeDTO volume;
    private final String storageClassName;

    public PersistentVolumeClaimTask(ApiStub apiStub,
                                     TaskConfig taskConfig,
                                     String namespace,
                                     VolumeDTO volume,
                                     String storageClassName) {
        super(apiStub, taskConfig, namespace);
        this.volume = volume;
        this.storageClassName = storageClassName;
    }

    @Override
    protected void execute(TaskResult.TaskResultBuilder<Void> taskResult) throws ApiException, SkippedExistsException {
        log.info("Create PVC {} if not exists", pvcName(volume.getName()));

        if (!pvcExists(pvcName(volume.getName()))) {
            log.debug("Create PVC for volume {}", pvcName(volume.getName()));
            getApiStub().getCoreV1Api()
                .createNamespacedPersistentVolumeClaim(
                    getNamespace(),
                    new V1PersistentVolumeClaim()
                        .metadata(
                            new V1ObjectMeta()
                                .name(pvcName(volume.getName()))
                        )
                        .spec(
                            new V1PersistentVolumeClaimSpec()
                                .accessModes(PVC_ACCESS_MODES)
                                .volumeMode(PVC_VOLUME_MODE)
                                .storageClassName(storageClassName)
                                .resources(new V1VolumeResourceRequirements()
                                    .requests(Map.of(
                                        PVC_RESOURCE_KEY,
                                        Quantity.fromString(volume.getSize() + VOLUME_SIZE_UNIT)
                                    ))
                                )
                        )
                )
                .execute();
        } else {
            log.debug("Skipping PVC {} creation as it exists", pvcName(volume.getName()));
            throw new SkippedExistsException();
        }
    }

    @Override
    protected boolean isReady() throws ApiException {
        log.debug("## PVC :: {} :: wait poll step", getNamespace());
        return pvcExists(pvcName(volume.getName()));
    }

    protected boolean pvcExists(String pvcName) throws ApiException {
        try {
            V1PersistentVolumeClaimList pvcs =
                getApiStub().getCoreV1Api().listNamespacedPersistentVolumeClaim(getNamespace()).execute();
            return pvcs.getItems().stream()
                .anyMatch(pvc -> pvcName.equals(
                    Objects.requireNonNull(pvc.getMetadata()).getName()));
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                return false;
            }
            throw e;
        }
    }
}
