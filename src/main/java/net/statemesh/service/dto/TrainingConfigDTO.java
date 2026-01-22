package net.statemesh.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TrainingConfigDTO {
    private String baseModel;
    private List<String> plugins; // In Axolotl
    private Boolean useRay; // In Axolotl
    private Integer rayNumWorkers; // In Axolotl
    private Distributed distributed; // In Surogate
    private String adapter; // In Axolotl
    private Boolean loadIn4bit; // In Axolotl
    private Integer loraR;
    private Integer loraAlpha;
    private Double loraDropout;
    private List<String> loraTargetModules;
    private List<Dataset> datasets;
    private List<Dataset> testDatasets;
    private String outputDir;
    private String optimizer;
    private Integer numEpochs;
    private Integer microBatchSize; // Better small for memorization tasks and/or small datasets
    private Integer gradientAccumulationSteps; // Better small for memorization tasks and/or small datasets
    private Boolean trainOnInputs; // Very important to be true for memorization tasks (and alpaca prompter)
    private String learningRate;
    private Double valSetSize;
    private Integer evalSteps;
    private Integer loggingSteps;
    private Integer maxSteps;
    private Integer saveSteps;
    private Integer savesPerEpoch;
    private Double weightDecay;
    private Double maxGradNorm;
    private Integer sequenceLen;
    private Boolean samplePacking;
    private String lrScheduler;
    private Integer warmupSteps;
    private Double warmupRatio;
    private Integer cooldownSteps;
    private Double finalLrFraction;
    private Boolean gradientCheckpointing; // Saves RAM but makes training slower
    private GradientCheckpointingKwargs gradientCheckpointingKwargs; // In Axolotl
    private Integer skipQuantFirstLayers;
    private Integer skipQuantLastLayers;
    private Boolean debugTimeBreakdown;
    private Boolean debugMemoryBreakdown;
    private String recipe;
    private Integer zeroLevel;
    private Boolean lora;
    private Boolean qloraFp8;
    private Boolean qloraFp4;
    private Boolean qloraBnb;
    private Boolean recomputeLora;
    private Boolean mergeLora;
    private Boolean mergeIteratively;
    private Aim aim; // In Axolotl
    private String reportTo; // In Surogate
    private String aimExperiment; // In Surogate
    private String aimRepo; // In Surogate

    public TrainingConfigDTO withBaseModel(String baseModel) {
        this.baseModel = baseModel;
        return this;
    }

    public TrainingConfigDTO withPlugins(List<String> plugins) {
        this.plugins = plugins;
        return this;
    }

    public TrainingConfigDTO withUseRay(Boolean useRay) {
        this.useRay = useRay;
        return this;
    }

    public TrainingConfigDTO withRayNumWorkers(Integer rayNumWorkers) {
        this.rayNumWorkers = rayNumWorkers;
        return this;
    }

    public TrainingConfigDTO withDistributed(Distributed distributed) {
        this.distributed = distributed;
        return this;
    }

    public TrainingConfigDTO withAdapter(String adapter) {
        this.adapter = adapter;
        return this;
    }

    public TrainingConfigDTO withLoadIn4bit(Boolean loadIn4bit) {
        this.loadIn4bit = loadIn4bit;
        return this;
    }

    public TrainingConfigDTO withOutputDir(String outputDir) {
        this.outputDir = outputDir;
        return this;
    }

    public TrainingConfigDTO withDatasetsPath(String root) {
        if (this.datasets != null) {
            this.datasets.forEach(dataset ->
                dataset.setPath(root + dirName(dataset.repoId + "/" + dataset.ref))
            );
        }
        if (this.testDatasets != null) {
            this.testDatasets.forEach(dataset ->
                dataset.setPath(root + dirName(dataset.repoId + "/" + dataset.ref))
            );
        }
        return this;
    }

    public TrainingConfigDTO withAim(Aim aim) {
        this.aim = aim;
        return this;
    }

    public TrainingConfigDTO withReportTo(String reportTo) {
        this.reportTo = reportTo;
        return this;
    }

    public TrainingConfigDTO withAimExperiment(String aimExperiment) {
        this.aimExperiment = aimExperiment;
        return this;
    }

    public TrainingConfigDTO withAimRepo(String aimRepo) {
        this.aimRepo = aimRepo;
        return this;
    }

    public TrainingConfigDTO withGradientCheckpointingKwargs(GradientCheckpointingKwargs gradientCheckpointingKwargs) {
        this.gradientCheckpointingKwargs = gradientCheckpointingKwargs;
        return this;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Dataset {
        private String repoId;
        private String ref;
        private String path;
        private String type;
        private String subset;
        private String split;
        private String samples;
        private String textField; // In Surogate
        private String instructionField;
        private String inputField;
        private String outputField;
        private String systemPromptType; // In Surogate
        private String systemPromptField; // In Surogate
        private String systemPrompt;
        private String promptFormat;
        private String promptFormatNoInput;
        private String messagesField;
        private String systemField;
        private String toolsField;
        private MessagePropertyMapping messagePropertyMappings;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class MessagePropertyMapping {
            private String role;
            private String content;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GradientCheckpointingKwargs {
        @JsonProperty("use_reentrant")
        private Boolean useReentrant;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Aim {
        @JsonProperty("aim_enable")
        private Boolean aimEnable;
        @JsonProperty("aim_repo")
        private String aimRepo;
        @JsonProperty("aim_experiment")
        private String aimExperiment;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Distributed {
        @JsonProperty("ray_address")
        private String rayAddress;
        @JsonProperty("num_nodes")
        private Integer numNodes;
        @JsonProperty("gpus_per_node")
        private Integer gpusPerNode;
    }

    private String dirName(String dataset) {
        String b64 = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(dataset.getBytes(StandardCharsets.UTF_8));
        return "/dataset_" + b64;
    }
}
