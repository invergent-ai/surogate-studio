package net.statemesh.domain.enumeration;

public enum ModelRoutingStrategy {
    ROUND_ROBIN,
    SESSION,

    // best when many requests share a long, stable prefix (system prompt, tool schema, etc.).
    // It steers those requests to the instance that already holds that prefix’s KV, maximizing reuse.
    // Works great alongside vLLM’s built-in prefix cache.
    PREFIX_BASED,

    // best for RAG / multi-turn where reused text isn’t only a prefix (e.g., retrieved passages).
    // It routes toward instances (or LMCache backends) that are most likely to have any reusable KV blocks,
    // not just the front of the prompt. Complements LMCache’s non-prefix KV reuse.
    KV_BASED,

    // only for the Disaggregated Prefill/Decode deployment mode
    DISAGGREGATED_PREFILL
}
