package net.statemesh.service.dto.mixin;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.statemesh.service.dto.TrainingConfigDTO;

import java.util.List;

public abstract class TrainingConfigMixin {
    @JsonProperty("base_model")
    private String baseModel;

    @JsonProperty("use_ray")
    private Boolean useRay;

    @JsonProperty("ray_num_workers")
    private Integer rayNumWorkers;

    @JsonProperty("load_in_4bit")
    private Boolean loadIn4bit;

    @JsonProperty("lora_r")
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

    @JsonProperty("micro_batch_size")
    private Integer microBatchSize; // Better small for memorization tasks and/or small datasets

    @JsonProperty("gradient_accumulation_steps")
    private Integer gradientAccumulationSteps; // Better small for memorization tasks and/or small datasets

    @JsonProperty("train_on_inputs")
    private Boolean trainOnInputs; // Very important to be true for memorization tasks (and alpaca prompter)

    @JsonProperty("lr_scheduler")
    private String lrScheduler;

    @JsonProperty("learning_rate")
    private Double learningRate;

    @JsonProperty("warmup_ratio")
    private Double warmupRatio;

    @JsonProperty("warmup_steps")
    private Integer warmupSteps;

    @JsonProperty("val_set_size")
    private Double valSetSize;

    @JsonProperty("eval_steps")
    private Integer evalSteps;

    @JsonProperty("logging_steps")
    private Integer loggingSteps;

    @JsonProperty("gradient_checkpointing")
    private Boolean gradientCheckpointing; // Saves RAM but makes training slower

    @JsonProperty("gradient_checkpointing_kwargs")
    private TrainingConfigDTO.GradientCheckpointingKwargs gradientCheckpointingKwargs;

    @JsonProperty("max_steps")
    private Integer maxSteps;

    @JsonProperty("weight_decay")
    private Double weightDecay;

    @JsonProperty("sequence_len")
    private Integer sequenceLen;

    @JsonProperty("sample_packing")
    private Boolean samplePacking;

    @JsonProperty("merge_lora")
    private Boolean mergeLora;

    @JsonProperty("test_datasets")
    private List<TrainingConfigDTO.Dataset> testDatasets;
}
