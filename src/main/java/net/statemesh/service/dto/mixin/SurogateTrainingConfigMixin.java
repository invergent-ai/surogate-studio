package net.statemesh.service.dto.mixin;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.statemesh.service.dto.TrainingConfigDTO;

import java.util.List;

public abstract class SurogateTrainingConfigMixin {
    @JsonProperty("model")
    private String baseModel;

    @JsonProperty("use_ray")
    private Boolean useRay;

    @JsonProperty("ray_num_workers")
    private Integer rayNumWorkers;

    @JsonProperty("load_in_4bit")
    private Boolean loadIn4bit;

    @JsonProperty("lora_rank")
    private Integer loraR;

    @JsonProperty("lora_alpha")
    private Integer loraAlpha;

    @JsonProperty("lora_dropout")
    private Double loraDropout;

    @JsonProperty("lora_target_modules")
    private List<String> loraTargetModules;

    @JsonProperty("output_dir")
    private String outputDir;

    @JsonProperty("num_epochs")
    private Integer numEpochs;

    @JsonProperty("per_device_train_batch_size")
    private Integer microBatchSize;

    @JsonProperty("gradient_accumulation_steps")
    private Integer gradientAccumulationSteps;

    @JsonProperty("loss_scale")
    private String lossScale;

    @JsonProperty("learning_rate")
    private Double learningRate;

    @JsonProperty("validation_split_ratio")
    private Double valSetSize;

    @JsonProperty("eval_steps")
    private Integer evalSteps;

    @JsonProperty("logging_steps")
    private Integer loggingSteps;

    @JsonProperty("max_steps")
    private Integer maxSteps;

    @JsonProperty("weight_decay")
    private Double weightDecay;

    @JsonProperty("max_grad_norm")
    private Double maxGradNorm;

    @JsonProperty("sequence_len")
    private Integer sequenceLen;

    @JsonProperty("sample_packing")
    private Boolean samplePacking;

    @JsonProperty("lr_scheduler_type")
    private String lrScheduler;

    @JsonProperty("warmup_steps")
    private Integer warmupSteps;

    @JsonProperty("warmup_ratio")
    private Double warmupRatio;

    @JsonProperty("cooldown_steps")
    private Integer cooldownSteps;

    @JsonProperty("final_lr_fraction")
    private Double finalLrFraction;

    @JsonProperty("recompute_block")
    private Boolean gradientCheckpointing;

    @JsonProperty("recompute_lora")
    private Boolean recomputeLora;

    @JsonProperty("skip_quant_first_layers")
    private Integer skipQuantFirstLayers;

    @JsonProperty("skip_quant_last_layers")
    private Integer skipQuantLastLayers;

    @JsonProperty("debug_time_breakdown")
    private Boolean debugTimeBreakdown;

    @JsonProperty("debug_memory_breakdown")
    private Boolean debugMemoryBreakdown;

    @JsonProperty("recipe")
    private String recipe;

    @JsonProperty("qlora_fp8")
    private Boolean qloraFp8;

    @JsonProperty("qlora_fp4")
    private Boolean qloraFp4;

    @JsonProperty("qlora_bnb")
    private Boolean qloraBnb;

    @JsonProperty("lora")
    private Boolean lora;

    @JsonProperty("merge_adapter")
    private Boolean mergeLora;

    @JsonProperty("zero_level")
    private Integer zeroLevel;

    @JsonProperty("report_to")
    private String reportTo;

    @JsonProperty("aim_experiment")
    private String aimExperiment;

    @JsonProperty("aim_repo")
    private String aimRepo;

    @JsonProperty("validation_datasets")
    private List<TrainingConfigDTO.Dataset> testDatasets;
}
