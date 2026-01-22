package net.statemesh.k8s.task.application.storage;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.ApiResponse;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1StorageClass;
import io.kubernetes.client.openapi.models.V1StorageClassList;
import net.statemesh.k8s.exception.SkippedExistsException;
import net.statemesh.k8s.task.*;
import net.statemesh.k8s.util.ApiStub;
import net.statemesh.service.dto.VolumeDTO;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

import static net.statemesh.k8s.util.K8SConstants.*;
import static net.statemesh.k8s.util.NamingUtils.*;

public class StorageClassTask extends BaseMutationTask<Void> {
    private final Logger log = LoggerFactory.getLogger(StorageClassTask.class);
    private final VolumeDTO volume;

    public StorageClassTask(ApiStub apiStub,
                            TaskConfig taskConfig,
                            String namespace,
                            VolumeDTO volume) {
        super(apiStub, taskConfig, namespace);
        this.volume = volume;
    }

    @Override
    protected void execute(TaskResult.TaskResultBuilder<Void> taskResult) throws ApiException, SkippedExistsException {
        log.info("Create {} StorageClass {} if not exists",
            !StringUtils.isEmpty(volume.getBucketUrl()) ? "private" : "StateMesh",
            storageClassName(volume.getName(), !StringUtils.isEmpty(volume.getBucketUrl())));

        if (!storageClassExists(storageClassName(volume.getName(), !StringUtils.isEmpty(volume.getBucketUrl())))) {
            log.debug("Create StorageClass for volume {}", volume.getName());
            getApiStub().getApiClient().execute(
                getApiStub().getCustomApi().createClusterCustomObject(
                        STORAGE_CLASS_GROUP, API_VERSION, STORAGE_CLASS_PLURAL,
                        new V1StorageClass()
                            .metadata(
                                new V1ObjectMeta()
                                    .name(storageClassName(
                                        volume.getName(), !StringUtils.isEmpty(volume.getBucketUrl())))
                            )
                            .provisioner(STORAGE_CLASS_PROVISIONER)
                            .parameters(
                                Map.of(
                                    STORAGE_CLASS_NODE_PUBLISH_SECRET_KEY, storageSecretName(
                                        volume.getName(), !StringUtils.isEmpty(volume.getBucketUrl())),
                                    STORAGE_CLASS_NODE_PUBLISH_NAMESPACE_KEY, getNamespace(),
                                    STORAGE_CLASS_PROVISIONER_SECRET_KEY, storageSecretName(
                                        volume.getName(), !StringUtils.isEmpty(volume.getBucketUrl())),
                                    STORAGE_CLASS_PROVISIONER_NAMESPACE_KEY, getNamespace()
                                )
                            )
                            .reclaimPolicy(STORAGE_CLASS_RECLAIM_POLICY)
                            .volumeBindingMode(STORAGE_CLASS_VOLUME_BINDING_MODE)
                    )
                    .buildCall(null),
                V1StorageClass.class
            );
        } else {
            log.debug("Skipping StorageClass for volume {} creation as it exists", volume.getName());
            throw  new SkippedExistsException();
        }
    }

    @Override
    protected boolean isReady() throws ApiException {
        log.debug("## StorageClass :: {} :: wait poll step", getNamespace());
        return storageClassExists(storageClassName(volume.getName(), !StringUtils.isEmpty(volume.getBucketUrl())));

    }

    protected boolean storageClassExists(String scName) throws ApiException {
        try {
            ApiResponse<V1StorageClassList> scs = getApiStub().getApiClient().execute(
                getApiStub().getCustomApi()
                    .listClusterCustomObject(STORAGE_CLASS_GROUP, API_VERSION, STORAGE_CLASS_PLURAL)
                    .buildCall(null),
                V1StorageClassList.class
            );
            return scs.getData().getItems().stream()
                .anyMatch(sc -> scName.equals(
                    Objects.requireNonNull(sc.getMetadata()).getName()));
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                return false;
            }
            throw e;
        }
    }
}
