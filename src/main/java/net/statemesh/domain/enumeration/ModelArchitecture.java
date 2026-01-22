package net.statemesh.domain.enumeration;

public enum ModelArchitecture {
    // ex: Qwen/Qwen3-0.6B, Qwen/Qwen3-14B
    Qwen3ForCausalLM,

    // ex: Qwen/Qwen3-235B-A22B-Instruct-2507, Qwen/Qwen3-30B-A3B-Thinking-2507
    Qwen3MoeForCausalLM,

    // ex: Qwen/Qwen3-Next-80B-A3B-Instruct
    Qwen3NextForCausalLM,

    // ex: google/gemma-3-270m, google/gemma-3-4b-it
    Gemma3ForCausalLM,

    // ex: mistralai/Mistral-Small-3.2-24B-Instruct-2506
    MistralForCausalLM,

    // ex: zai-org/GLM-4.5, zai-org/GLM-4.5-Air
    Glm4MoeForCausalLM
}
