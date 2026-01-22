package net.statemesh.service.dto.mixin;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.statemesh.service.dto.TrainingConfigDTO;

public abstract class DatasetMixin {
    @JsonProperty("field_instruction") // type.field_instruction
    private String instructionField;

    @JsonProperty("field_input") // type.field_input
    private String inputField;

    @JsonProperty("field_output") // type.field_output
    private String outputField;

    @JsonProperty("system_prompt")
    private String systemPrompt;

    @JsonProperty("format") // type.format
    private String promptFormat;

    @JsonProperty("no_input_format") // type.no_input_format
    private String promptFormatNoInput;

    @JsonProperty("field_messages") // field_messages
    private String messagesField;

    @JsonProperty("field_system") // type.field_system
    private String systemField;

    @JsonProperty("field_tools") // field_tools
    private String toolsField;

    @JsonProperty("message_property_mappings")
    private TrainingConfigDTO.Dataset.MessagePropertyMapping messagePropertyMappings;
}
