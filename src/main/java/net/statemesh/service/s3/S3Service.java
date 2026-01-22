package net.statemesh.service.s3;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import net.statemesh.k8s.KubernetesController;
import net.statemesh.k8s.util.ApiUtils;
import net.statemesh.service.ApplicationService;
import net.statemesh.service.dto.ApplicationDTO;
import net.statemesh.service.dto.S3CredentialsDTO;
import net.statemesh.service.util.S3Config;
import net.statemesh.service.util.S3EndpointParser;
import net.statemesh.service.util.StorageProvider;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

import static net.statemesh.k8s.util.K8SConstants.STORAGE_SECRET_ACCESS_KEY_KEY;
import static net.statemesh.k8s.util.K8SConstants.STORAGE_SECRET_BUCKET_KEY;
import static net.statemesh.k8s.util.NamingUtils.storageSecretName;
import static net.statemesh.k8s.util.NamingUtils.volumeName;

@Service
@RequiredArgsConstructor
public class S3Service {
    private final Logger log = LoggerFactory.getLogger(S3Service.class);

    private final KubernetesController kubernetesController;
    private final ApplicationService applicationService;

    public boolean validateCredentials(S3CredentialsDTO credentials) {
        if (secretExists(credentials)) {
            return true;
        }

        try {
            S3Config config = S3EndpointParser.parseEndpoint(credentials.getBucketUrl());

            MinioClient.Builder builder = MinioClient.builder()
                .endpoint(config.endpoint())
                .credentials(credentials.getAccessKey(), credentials.getSecretKey());

            if (config.provider() != StorageProvider.AWS) {
                builder.region(credentials.getRegion() != null ? credentials.getRegion() : config.region());
            } else {
                builder.region(config.region());
            }

            MinioClient minioClient = builder.build();
            minioClient.listBuckets();
            return true;
        } catch (Exception e) {
            log.error("Error validating S3 credentials with message {}", e.getMessage());
            return false;
        }
    }

    private boolean secretExists(S3CredentialsDTO credentials) {
        if (!StringUtils.isEmpty(credentials.getApplicationId()) && !StringUtils.isEmpty(credentials.getVolumeName())) {
            ApplicationDTO application = applicationService.findOne(credentials.getApplicationId()).orElse(null);

            if (application != null && application.getProject() != null
                && application.getProject().getCluster() != null
                && !StringUtils.isEmpty(application.getDeployedNamespace())) {

                Map<String, byte[]> secret = ApiUtils.readSecret(
                    this.kubernetesController.getClients()
                        .get(application.getProject().getZone().getZoneId())
                        .get(application.getProject().getCluster().getCid()),
                    application.getDeployedNamespace(),
                    storageSecretName(volumeName(credentials.getVolumeName()), Boolean.TRUE)
                );
                if (secret != null &&
                    secret.containsKey(STORAGE_SECRET_ACCESS_KEY_KEY) &&
                    secret.containsKey(STORAGE_SECRET_BUCKET_KEY)) {
                    final byte[] bucketUrl = secret.get(STORAGE_SECRET_BUCKET_KEY);
                    final byte[] accessKey = secret.get(STORAGE_SECRET_ACCESS_KEY_KEY);
                    if (bucketUrl != null && accessKey != null) {
                        if (new String(bucketUrl).equals(credentials.getBucketUrl()) &&
                            new String(accessKey).equals(credentials.getAccessKey())) {
                            return Boolean.TRUE;
                        }
                    }
                }
            }
        }

        return Boolean.FALSE;
    }
}
