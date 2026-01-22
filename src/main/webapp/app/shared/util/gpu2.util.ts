import { IModelConfig } from '../model/model-settings';

// calculation according to https://apxml.com/posts/how-to-calculate-vram-requirements-for-an-llm

export type VramBreakdownEntry = { bytes: number; gib: number };

export interface VramBreakdown {
  weights: VramBreakdownEntry;
  kvCache: VramBreakdownEntry;
  activations: VramBreakdownEntry;
  overhead: VramBreakdownEntry;
  multiGpuOverhead: VramBreakdownEntry;
}

export interface VramEstimate {
  bytes: number;
  gib: number;
  breakdown: VramBreakdown;
}

export interface EstimateVramOptions {
  config: IModelConfig,

  /** Model size in parameters (e.g., 7e9 for "7B") */
  params: number;

  /** Bits per weight for the loaded model (e.g., 16 FP16, 8 INT8, 4 GGUF Q4). Default: 16 */
  weightBits?: number;

  /** Sequence length (tokens) per request. Default: 4096 */
  seqLen?: number;

  /** Tokens processed concurrently (micro-batch). Default: 1 */
  batchSize?: number;

  /** Transformer layers (n_layer) */
  nLayers: number;

  /** Total attention heads (n_head) */
  nHeads: number;

  /** KV heads for GQA/MQA. Defaults to nHeads (MHA) */
  nKvHeads?: number;

  /** Model hidden size (d_model). Provide this OR headDim. */
  hiddenSize?: number;

  /** Per-head dimension (d_head). If omitted, inferred as hiddenSize / nHeads. */
  headDim?: number;

  /** Bits per KV element (e.g., 16 FP16, 8 INT8, 4 Q4_K). Default: 16 */
  kvCacheBits?: number;

  /** Include an activation buffer heuristic. Default: true */
  includeActivations?: boolean;

  /** Heuristic multiplier for activations (relative to d_model). Default: 1.0 */
  activationFactor?: number;

  /** Extra bytes for runtime/temps/logits/allocator slack. Default: ~512 MiB */
  overheadBytes?: number;

  /**
   * Number of GPUs used for inference. If > 1, a multi-GPU overhead factor is applied.
   * Default: 1
   */
  numGpus?: number;

  /**
   * Effective utilization (usable fraction) when using multiple GPUs (tensor/pipe parallel),
   * e.g., 0.85 means ~15% overhead. Applied only when numGpus > 1.
   * Default: 0.85 for nvlink, 0.70 for PCIe (per ApX rule of thumb)
   */
  multiGpuUtilization?: number;
}

/**
 * Estimate LLM inference VRAM usage (in bytes & GiB).
 * total ≈ weights + KV cache (+ activations) (+ overhead)
 */
export function estimateVram(opts: EstimateVramOptions): VramEstimate {
  let {
    config,
    weightBits = 16,
    seqLen = 4096,
    batchSize = 1,
    kvCacheBits = 16,
    includeActivations = true,
    activationFactor = 1.0,
    overheadBytes = 512 * 1024 * 1024,
    numGpus = 1,
    multiGpuUtilization = 0.7,
  } = opts;

  const params = config.hfTotalSafetensors;
  const nLayers = getHFConfigValue(config.hfConfig, ['num_layers', 'n_layer', 'n_layers', 'num_hidden_layers']);
  const nHeads = getHFConfigValue(config.hfConfig, ['num_attention_heads', 'n_head', 'n_heads', 'num_heads'])
  const nKvHeads = getHFConfigValue(
    config.hfConfig,
    ['num_key_value_heads', 'n_head_kv', 'num_kv_heads', 'multi_query_group_num', 'kv_n_head'],
    nHeads
  );
  const hiddenSize = getHFConfigValue(config.hfConfig, ['hidden_size', 'dim', 'n_embd']);
  const headDim = getHFConfigValue(config.hfConfig, ['head_dim'], Math.ceil(hiddenSize / nHeads));

  if (!Number.isFinite(params) || params <= 0) {
    throw new Error("`hfTotalSafetensors` must be a positive number (total model parameters).");
  }
  if (!Number.isFinite(nLayers) || nLayers <= 0) {
    throw new Error("`nLayers` must be a positive number.");
  }
  if (!Number.isFinite(nHeads) || nHeads <= 0) {
    throw new Error("`nHeads` must be a positive number.");
  }
  if (numGpus > 1 && !(multiGpuUtilization > 0 && multiGpuUtilization <= 1)) {
    throw new Error("`multiGpuUtilization` must be in (0, 1] when `numGpus > 1`.");
  }

  // Determine d_head and d_model
  const dHead = headDim ?? (hiddenSize ? hiddenSize / nHeads : undefined);
  const dModel = hiddenSize ?? (dHead ? dHead * nHeads : undefined);

  if (!dHead || !dModel) {
    throw new Error(
      "Provide either `hiddenSize` (d_model) or `headDim` (d_head) so KV cache can be computed."
    );
  }

  const toGiB = (b: number) => b / (1024 ** 3);

  // 1) Weights
  const weightBytes = params * (weightBits / 8);

  // 2) KV cache
  // per-token per-layer = 2 (K & V) * nKvHeads * dHead * bytes_per_element
  const bytesPerKV = kvCacheBits / 8;
  const kvPerTokenPerLayer = 2 * nKvHeads * dHead * bytesPerKV;
  const kvBytes = batchSize * seqLen * nLayers * kvPerTokenPerLayer;

  // 3) Activations (heuristic)
  const bytesPerAct = 2; // assume FP16 activations by default
  const actBytes = includeActivations
    ? Math.max(0, activationFactor) * batchSize * seqLen * nLayers * dModel * bytesPerAct
    : 0;

  // Baseline GPU-resident tensors (before overheads)
  const baselineGpuResident = weightBytes + kvBytes + actBytes;

  // 4) Multi-GPU overhead — only when numGpus > 1.
  // Interpret utilization u as "usable fraction"; overhead multiplier = (1/u − 1).
  const multiGpuOverheadBytes =
    numGpus > 1 ? baselineGpuResident * (1 / multiGpuUtilization - 1) : 0;

  // 5) Software/runtime overhead
  const overhead = Math.max(0, overheadBytes);

  const totalBytes = baselineGpuResident + multiGpuOverheadBytes + overhead;

  const breakdown: VramBreakdown = {
    weights: { bytes: weightBytes, gib: toGiB(weightBytes) },
    kvCache: { bytes: kvBytes, gib: toGiB(kvBytes) },
    activations: { bytes: actBytes, gib: toGiB(actBytes) },
    multiGpuOverhead: { bytes: multiGpuOverheadBytes, gib: toGiB(multiGpuOverheadBytes) },
    overhead: { bytes: overhead, gib: toGiB(overhead) },
  };

  return { bytes: totalBytes, gib: toGiB(totalBytes), breakdown };
}

function getHFConfigValue(cfg: any, keys: string[], fallback?: number) {
  for (const k of keys) {
    if (cfg && cfg[k] != null) return cfg[k];
  }
  return fallback;
}

/* ---------- Example usage ----------
const est = estimateVram({
  params: 7e9,
  weightBits: 16,
  seqLen: 4096,
  batchSize: 1,
  nLayers: 32,
  nHeads: 32,
  hiddenSize: 4096, // d_head inferred as 128
  kvCacheBits: 16,
  includeActivations: true,
  activationFactor: 0.5,
});
console.log(est);
------------------------------------ */
