package net.statemesh.service.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.statemesh.service.dto.RayJobDTO;
import net.statemesh.service.dto.SkyConfigDTO;
import net.statemesh.service.dto.TrainingConfigDTO;
import net.statemesh.service.dto.mixin.*;

import java.util.function.Consumer;

import static net.statemesh.config.Constants.USE_AXOLOTL_TRAINING_LIBRARY;

public class MixinUtil {
    public static void addSerializationMixins(ObjectMapper yamlMapper, RayJobDTO rayJobDTO) {
        yamlMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        if (USE_AXOLOTL_TRAINING_LIBRARY) {
            yamlMapper.addMixIn(TrainingConfigDTO.class, TrainingConfigMixin.class);
            yamlMapper.addMixIn(TrainingConfigDTO.Dataset.class, DatasetMixin.class);
            addValuesSerializationMixin(rayJobDTO.getTrainingConfigPojo());
        } else {
            yamlMapper.addMixIn(TrainingConfigDTO.class, SurogateTrainingConfigMixin.class);
            yamlMapper.addMixIn(TrainingConfigDTO.Dataset.class, SurogateDatasetMixin.class);
        }
    }

    public static void addDeserializationMixins(ObjectMapper yamlMapper) {
        if (USE_AXOLOTL_TRAINING_LIBRARY) {
            yamlMapper.addMixIn(TrainingConfigDTO.class, TrainingConfigMixin.class);
            yamlMapper.addMixIn(TrainingConfigDTO.Dataset.class, DatasetMixin.class);
        } else {
            yamlMapper.addMixIn(TrainingConfigDTO.class, SurogateTrainingConfigMixin.class);
            yamlMapper.addMixIn(TrainingConfigDTO.Dataset.class, SurogateDatasetMixin.class);
        }
    }

    public static void addSkyMixins(ObjectMapper yamlMapper) {
        yamlMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        yamlMapper.addMixIn(SkyConfigDTO.class, SkyConfigMixin.class);
        yamlMapper.addMixIn(SkyConfigDTO.Resources.class, ResourcesMixin.class);
    }

    private static void addValuesSerializationMixin(TrainingConfigDTO trainingConfigDTO) {
        if ("adamw_8bit".equals(trainingConfigDTO.getOptimizer())) {
            trainingConfigDTO.setOptimizer("adamw_torch_fused");
        }
        datasetMixins(trainingConfigDTO, MixinUtil::serializedDatasetType);
    }

    public static void addValuesDeserializationMixin(TrainingConfigDTO trainingConfigDTO) {
        if ("adamw_torch_fused".equals(trainingConfigDTO.getOptimizer())) {
            trainingConfigDTO.setOptimizer("adamw_8bit");
        }
        datasetMixins(trainingConfigDTO, MixinUtil::deserializedDatasetType);
    }

    private static void datasetMixins(TrainingConfigDTO trainingConfigDTO, Consumer<TrainingConfigDTO.Dataset> consumer) {
        if (trainingConfigDTO.getDatasets() == null || trainingConfigDTO.getDatasets().isEmpty()) {
            return;
        }
        trainingConfigDTO.getDatasets().forEach(consumer);

        if (trainingConfigDTO.getTestDatasets() == null || trainingConfigDTO.getTestDatasets().isEmpty()) {
            return;
        }
        trainingConfigDTO.getTestDatasets().forEach(consumer);
    }

    private static void serializedDatasetType(TrainingConfigDTO.Dataset dataset) {
        dataset.setType(
            switch (dataset.getType()) {
                case "text" -> "pretrain";
                case "instruction" -> "alpaca";
                case "conversation" -> "gpteacher";
                default -> dataset.getType();
            }
        );
    }

    private static void deserializedDatasetType(TrainingConfigDTO.Dataset dataset) {
        dataset.setType(
            switch (dataset.getType()) {
                case "pretrain" -> "text";
                case "alpaca" -> "instruction";
                case "gpteacher" -> "conversation";
                default -> dataset.getType();
            }
        );
    }
}
