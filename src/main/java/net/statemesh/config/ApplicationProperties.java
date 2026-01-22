package net.statemesh.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class ApplicationProperties {
    private String clientUrl;
    private String serverUrl;
    private Boolean k8sAccessMode;
    private Long nodeReservationDuration;
    private Boolean checkNodeResourceUpdates;
    private final RateLimiter rateLimiter = new RateLimiter();
    private final Metrics metrics = new Metrics();
    private final Pipeline pipeline = new Pipeline();
    private final Job job = new Job();
    private final K8sTask k8sTask = new K8sTask();
    private final Storage storage = new Storage();
    private final Token token = new Token();
    private final Profile profile = new Profile();
    private final Mail mail = new Mail();
    private final LakeFs lakeFs = new LakeFs();

    @Getter
    @Setter
    public static class RateLimiter {
        private Long rateLimitTokens;
        private Long smidRateLimitTokens;
    }

    @Getter
    @Setter
    public static class Metrics {
        private Long logsWatchTimeout;
        private Long shellWatchTimeout;
        private Long metricsPollInterval;
        private Long metricsWaitTimeout;
        private Long statusPollInterval;
        private Long statusWaitTimeout;
    }

    @Getter
    @Setter
    public static class Pipeline {
        private Long cicdPipelineWaitTimeout;
        private boolean cicdPipelineAutopublish;
    }

    @Getter
    @Setter
    public static class Job {
        private String updateNodeStatusSchedule;
        private String deleteTerminatingSchedule;
        private String unpayingAppsDeletionSchedule;
        private String unpayingAppsUnlockSchedule;
        private String cordonUnpayingNodesSchedule;
        private String uncordonUnpayingNodesSchedule;
        private boolean terminatingPodsDeleteFinalizers;
    }

    @Getter
    @Setter
    public static class K8sTask {
        private Integer resourceOperationPollInterval;
        private Integer resourceOperationWaitTimeout;
        private Integer resourceOperationWatchTimeout;
        private Double requestVsLimitsCoefficientCpu;
        private Double requestVsLimitsCoefficientMemory;
    }

    @Getter
    @Setter
    public static class Storage {
        private String smStorageBucketUrl;
        private String smStorageAccessKey;
        private String smStorageAccessSecret;
    }

    @Getter
    @Setter
    public static class Token {
        private Integer ttl;
    }

    @Getter
    @Setter
    public static class Profile {
        private List<String> datacenters;
        private List<RayCluster> rayClusters;
    }

    @Getter
    @Setter
    public static class RayCluster {
        private String name;
        private String cid;
        private String url;
        private String aimUrl;
    }

    @Getter
    @Setter
    public static class Mail {
        private String from;
    }

    @Getter
    @Setter
    public static class LakeFs {
        private String key;
        private String secret;
        private String endpoint;
        private String endpointInternal;
        private String s3Endpoint;
        private String masterKey;
    }
}
