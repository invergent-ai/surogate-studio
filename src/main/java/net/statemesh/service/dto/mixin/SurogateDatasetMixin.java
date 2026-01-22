package net.statemesh.service.dto.mixin;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.statemesh.service.dto.TrainingConfigDTO;

public abstract class SurogateDatasetMixin {
    @JsonProperty("text_field")
    private String textField;

    @JsonProperty("instruction_field")
    private String instructionField;

    @JsonProperty("input_field")
    private String inputField;

    @JsonProperty("output_field")
    private String outputField;

    @JsonProperty("system_prompt_type")
    private String systemPromptType;

    @JsonProperty("system_prompt_field")
    private String systemPromptField;

    @JsonProperty("system_prompt")
    private String systemPrompt;

    @JsonProperty("prompt_format")
    private String promptFormat;

    @JsonProperty("prompt_format_no_input")
    private String promptFormatNoInput;

    @JsonProperty("messages_field")
    private String messagesField;

    @JsonProperty("system_field")
    private String systemField;

    @JsonProperty("tools_field")
    private String toolsField;

    @JsonProperty("message_property_mappings")
    private TrainingConfigDTO.Dataset.MessagePropertyMapping messagePropertyMappings;
}
