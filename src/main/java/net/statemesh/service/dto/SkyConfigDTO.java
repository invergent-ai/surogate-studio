package net.statemesh.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SkyConfigDTO {
    private String name;
    private String workDir;
    private Integer numNodes;
    private Resources resources;
    private Map<String, String> envs;
    private Map<String, String> secrets;
    private Map<String, String> volumes;
    private Map<String, String> fileMounts;
    private String setup;
    private String run;
    private Map<String, Object> config;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Resources {
        private String infra;
        private String accelerators;
        private String acceleratorArgs;
        private String cpus;
        private String memory;
        private String instanceType;
        private Boolean useSpot;
        private Integer diskSize;
        private String diskTier;
        private String networkTier;
        private String imageId;
        private String ports;
        private Map<String, String> labels;
        private Autostop autostop;
        private List<AnyOf> anyOf;
        private List<Ordered> ordered;
        private String jobRecovery;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Autostop {
            private Integer idleMinutes;
            private Boolean down;
            private String waitFor;
            private String hook;
            private Integer hookTimeout;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class AnyOf {
            private String infra;
            private String accelerators;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Ordered {
            private String infra;
        }
    }

    public SkyConfigDTO withName(String name) {
        this.name = name;
        return this;
    }

    public SkyConfigDTO withWorkDir(String workDir) {
        this.workDir = workDir;
        return this;
    }

    public SkyConfigDTO withNumNodes(Integer numNodes) {
        this.numNodes = numNodes;
        return this;
    }

    public SkyConfigDTO withSetup(String setup) {
        this.setup = setup;
        return this;
    }

    public SkyConfigDTO withRun(String run) {
        this.run = run;
        return this;
    }

    public SkyConfigDTO withConfig(Map<String, Object> config) {
        this.config = config;
        return this;
    }

    public SkyConfigDTO withEnvs(Map<String, String> envs) {
        this.envs = envs;
        return this;
    }

    public SkyConfigDTO withFileMounts(Map<String, String> fileMounts) {
        this.fileMounts = fileMounts;
        return this;
    }

    public SkyConfigDTO withImageId(String imageId) {
        if (this.resources == null) {
            this.resources = new Resources();
        }
        this.resources.setImageId(imageId);
        return this;
    }

    public SkyConfigDTO withAutostop(Resources.Autostop autostop) {
        if (this.resources == null) {
            this.resources = new Resources();
        }
        this.resources.setAutostop(autostop);
        return this;
    }
}
