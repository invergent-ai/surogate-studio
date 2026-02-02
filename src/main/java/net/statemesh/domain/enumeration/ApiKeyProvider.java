package net.statemesh.domain.enumeration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ApiKeyProvider {
    // LLM Providers
    OPENAI("openai"),
    ANTHROPIC("anthropic"),
    OPENROUTER("openrouter"),
    AZURE("azure"),
    VLLM("vllm"),
    OLLAMA("ollama"),

    // Cloud Providers
    AWS("aws"),
    GCP("gcp"),
    OCI("oci"),
    RUNPOD("runpod");

    private final String value;

    ApiKeyProvider(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ApiKeyProvider fromValue(String value) {
        for (ApiKeyProvider provider : values()) {
            if (provider.value.equalsIgnoreCase(value) || provider.name().equalsIgnoreCase(value)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("Unknown provider: " + value);
    }
}
