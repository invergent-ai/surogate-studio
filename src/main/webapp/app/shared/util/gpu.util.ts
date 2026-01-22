import { IModelConfig } from '../model/model-settings';
import { NodeStats } from '../model/k8s/node-stats.model';
import { roundUpTo } from './display.util';

/**
 * vLLM-style perf model (clean, deduped)
 * - Block-based KV accounting (paged attention)
 * - Auto-batch from KV capacity and active users
 * - No P2P/NVLink: PCIe host-bounce only
 * - Compute / HBM / Interconnect legs
 * - Per-request prefill and decode timing using avgOutputLen
 */

// ----------------------------- Types and Tunables ----------------------------

type TensorPrecision = 'fp16' | 'bf16' | 'fp8' | 'fp4' | 'tf32' | 'int8';
type AccumulateType = 'fp16' | 'fp32';
type KvDataType = 'fp16/bf16' | 'fp8';
type ActivationDataType = 'fp32' | 'fp16/bf16' | 'fp8/int8';
type WeightDataType = 'fp32' | 'fp16/bf16' | 'fp8/int8' | 'fp4/int4';
export type GPUType = 'rtx6000pro' | 'rtx5090' | 'rtx4070ti';

const NON_TORCH_MEMORY_MB = 0.5 * 1024; // approx 1.5 GB per GPU for CUDA and framework overhead

const PARAMETER_DATA_TYPE_SIZE: Record<WeightDataType, number> = {
  'fp32': 4,
  'fp16/bf16': 2,
  'fp8/int8': 1,
  'fp4/int4': 0.5,
};

const ACTIVATION_DATA_TYPE_SIZE: Record<ActivationDataType, number> = {
  'fp32': 4,
  'fp16/bf16': 2,
  'fp8/int8': 1,
};

const KV_DATA_TYPE_SIZE: Record<KvDataType, number> = {
  'fp16/bf16': 2,
  'fp8': 1,
};

// Peak dense tensor throughput (TFLOPs/TOPS). Placeholder values; override if you can measure.
const TENSOR_PEAKS_DENSE: Record<GPUType, {
  fp4?: { acc_fp32?: number };
  fp8?: { acc_fp16?: number; acc_fp32?: number };
  fp16?: { acc_fp16?: number; acc_fp32?: number };
  bf16?: { acc_fp32?: number };
  tf32?: number;
  int8?: number;
}> = {
  rtx6000pro: {
    fp4: { acc_fp32: 2015.2 },
    fp8: { acc_fp16: 1007.6, acc_fp32: 1007.6 },
    fp16: { acc_fp16: 503.8, acc_fp32: 503.8 },
    bf16: { acc_fp32: 503.8 },
    tf32: 251.9,
    int8: 1007.6,
  },
  rtx5090: {
    fp4: { acc_fp32: 1676 },
    fp8: { acc_fp16: 838, acc_fp32: 419 },
    fp16: { acc_fp16: 419, acc_fp32: 209.5 },
    bf16: { acc_fp32: 209.5 },
    tf32: 104.8,
    int8: 838,
  },
  rtx4070ti: {
    fp8: { acc_fp16: 190, acc_fp32: 95 },
    fp16: { acc_fp16: 82, acc_fp32: 41 },
    bf16: { acc_fp32: 41 },
    tf32: 20,
    int8: 190,
  }
};

// Interconnect model (PCIe host-bounce only; no P2P/NVLink)
export type InterconnectOpts = {
  pcieWidth?: 16 | 8;
  pcieEfficiency?: number; // 0..1 baseline; extra penalties applied for host-bounce
  hostDDRBandwidthGBs?: number; // per-socket peak GB/s
  cpuSockets?: 1 | 2;
};

export const DEFAULT_INTERCONNECT_OPTS: InterconnectOpts = {
  pcieWidth: 16,
  pcieEfficiency: 0.85,
  hostDDRBandwidthGBs: 170,
  cpuSockets: 2,
};

// Utilization knobs
export type UtilizationOpts = {
  computeUtilization?: number; // decode often 0.35 to 0.55
  hbmUtilization?: number;
  interconnectUtilization?: number;
};

export const DEFAULT_UTILIZATION_OPTS: Required<UtilizationOpts> = {
  computeUtilization: 0.5,
  hbmUtilization: 0.8,
  interconnectUtilization: 0.8,
};

export type MathMode = {
  precision: TensorPrecision;
  accumulate: AccumulateType;
};

export type AdvancedOpts = {
  // sequence lengths and batching
  decodePrefixLen?: number; // avg seen context during decode; if not set, uses avgPromptLen + avgGenSoFar
  promptLen?: number; // L used in prefill modeling
  batchEffective?: number; // manual override (disables auto when provided)
  autoBatch?: boolean; // default true: choose batch from KV pool and users
  maxNumSeqsCap?: number; // optional scheduler-like cap

  // KV modeling (block-based)
  kvIsShardedAcrossTP?: boolean; // default true
  kvBlockSize?: number; // paged attention block size; default 16
  kvFragmentation?: number; // >1.0; default 1.05
  avgPromptLen?: number; // average prompt len per active request
  avgGenSoFar?: number; // average generated tokens so far (during decode)
  activeUsers?: number; // active concurrent requests

  // Memory budgeting
  activationReserveGiB?: number; // small inference reserve; default 1 GiB
  quantOverheadW4?: number; // multiplicative overhead for W4 packing; default 1.10
  gpuMemoryUtilization?: number; // vLLM KV pool sizing (0..1), default 1.0

  // Interconnect and hardware
  interconnect?: InterconnectOpts; // override default (still no P2P)
  utilization?: UtilizationOpts; // override default
  gpuBandwidthGBsOverride?: number; // override HBM/GDDR bandwidth per GPU
  tensorPeaksOverrideTFlops?: number; // override peak TFLOPs for the chosen math

  // Generation length
  avgOutputLen?: number; // average number of tokens generated per request
};

// ------------------------------ Output Types ---------------------------------

/** Top-level report returned by modelPerformance(). */
export interface ModelPerfReport {
  /** Execution topology and scheduler state. */
  topology: TopologyReport;
  /** Parsed model hyperparameters and datatypes used in the math. */
  model: ModelMetadata;
  /** Memory accounting per GPU (post headroom), plus KV block details. */
  memory: MemoryReport;
  /** Decode path timing and throughput breakdown (per-token/step). */
  tokens_per_second: TPSReport;
  /** Prefill (prompt) timing numbers and throughput over the prompt length. */
  prefill: PrefillReport;
}

/** How the run is laid out across hardware. */
export interface TopologyReport {
  /** Number of visible/participating GPUs. */
  gpuCount: number;
  /** Tensor parallel degree t. */
  tensorParallelDegree: number;
  /** True if running on a single GPU or t == 1. */
  singleGPU: boolean;
  /** Effective number of sequences packed in the current decode step. */
  batchEffective?: number;
}

/** Model hyperparameters and chosen datatypes. */
export interface ModelMetadata {
  /** Total parameters (from safetensors) used for weight memory. */
  num_params: number;
  /** Weight datatype used for parameter bytes (e.g., fp4/int4, fp16/bf16). */
  weight_data_type: WeightDataType;
  /** Activation datatype used for on-card activation bytes. */
  activation_data_type: ActivationDataType;
  /** KV cache datatype. */
  kv_data_type: KvDataType;
  /** Hidden size (model dimension d). */
  hidden_size: number;
  /** FFN expansion size. */
  intermediate_size: number;
  /** Number of KV heads (after GQA/MQA). */
  num_key_value_heads: number;
  /** Head dimension. */
  head_dim: number;
  /** Number of transformer layers. */
  num_layers: number;
}

/** Memory report, all values are per GPU unless stated otherwise. */
export interface MemoryReport {
  /** Total required memory per GPU, after allocator headroom is applied (GiB). */
  perGPU_required_gb: number;
  /** Weights footprint per GPU (GiB). */
  perGPU_weights_gb: number;
  /** Small reserve for transient activations, graphs, etc. (GiB). */
  perGPU_activation_reserve_gb?: number;
  /** Estimated resident KV cache per GPU based on block model (GiB) for current avg settings. */
  perGPU_kv_cache_gb_estimated?: number;
  /** Non-framework overhead (CUDA driver, runtime, etc.) (GiB). */
  non_torch_overhead_gb: number;
  /** Fraction kept free by the allocator (e.g., 0.1 means 10% free). */
  allocator_headroom: number;

  /** KV paging block size in tokens. */
  kv_block_size?: number;
  /** Bytes per KV block for the local shard on one GPU. */
  kv_bytes_per_block?: number;
  /** Average prompt length used for capacity sizing (tokens). */
  avg_prompt_len?: number;
  /** Average generated tokens already present during decode (tokens). */
  avg_generated_so_far?: number;
  /** Active concurrent requests considered for auto-batching. */
  active_users_input?: number;
  /** Active users that actually fit this step (equals batchEffective). */
  effective_active_users?: number;
  /** Average KV bytes per user (rounded to blocks). */
  kv_bytes_per_avg_user?: number;
  /** Minimum capacity (in sequences) across GPUs by KV pool (with gpuMemoryUtilization). */
  capByKV_minAcrossGPUs?: number;
  /** Per-GPU sequence capacity array (by KV), one entry per GPU. */
  concurrencyByGpu: number[];

  /**
   * Worst-case memory requirement for this config (maxContextSize).
   * This ignores gpuMemoryUtilization and available memory; it computes what is needed.
   */
  worst_case?: {
    /** Prompt length used (maxContextSize). */
    prompt_len: number;
    /** KV cache bytes per GPU (GiB) required to hold users x prompt_len at once. */
    perGPU_kv_cache_gb: number;
    /** Total required per GPU including weights, overhead, reserve, headroom (GiB). */
    perGPU_required_gb: number;
    /** For each GPU: does available memory meet the requirement (true/false). */
    fits_available_perGPU: boolean[];
    /** For each GPU: shortfall (GiB), 0 if it fits. */
    deficit_gb_perGPU: number[];
  };
}

/** Decode timing/throughput per step plus end-to-end time components. */
export interface TPSReport {
  /** Math mode used: precision and accumulate type. */
  math_mode: MathMode;
  /** Peak dense tensor throughput used for modeling (TFLOPs). */
  peakTensorTFlopsDense: number;
  /** Effective TFLOPs after utilization; floors applied to avoid division by zero. */
  effTFLOPs: number;
  /** Per-token total FLOPs counting 2 flops per MAC, across all layers. */
  flopsPerToken_total_2flop: number;
  /** Per-GPU share of FLOPs per token under tensor parallel t. */
  flopsPerToken_perGPU: number;
  /** Compute time per generated token (ms) on the current GPU. */
  compute_ms_per_token: number | null;
  /** Total on-card bytes moved per token on one GPU (activations + KV) across all layers. */
  totalOnCardBytesPerTokenPerGPU: number;
  /** Peak memory bandwidth (GB/s). */
  hbmGBs_peak: number;
  /** Effective memory bandwidth after utilization; floors applied. */
  hbmGBs_eff: number;
  /** Memory time per token (ms). */
  hbm_ms_per_token: number | null;
  /** Interconnect time per token (ms); 0 on single GPU; host-bounce model if multi-GPU. */
  interconnect_ms_per_token: number | null;
  /** Collective latency per token (ms) due to NCCL kernel launches, etc. */
  collective_latency_ms_per_token: number;
  /** Fixed per-token floor capturing kernel launch and scheduling overheads (ms). */
  perToken_fixed_latency_ms: number;
  /** Which leg dominates (compute, hbm, interconnect) based on ms. */
  dominant_leg: { name: 'compute' | 'hbm' | 'interconnect'; ms: number };
  /** Aggregate decode tokens/sec across all active sequences in this step. */
  conservative_tps: number;

  /** Average number of tokens you plan to generate per request (used for latency estimate). */
  avg_output_len: number;
  /** Per-request prefill latency in seconds (single stream). */
  prefill_time_seconds: number | null;
  /** Sum of prefill seconds across the batched streams (GPU-seconds), useful for throughput math. */
  prefill_time_seconds_aggregate?: number | null;
  /** Per-request decode latency in seconds: avg_output_len / per-stream TPS. */
  decode_time_seconds: number | null;
  /** End-to-end latency in seconds: prefill + decode. */
  gen_time_seconds: number | null;
}

/** Prefill timing totals and prompt-throughput metrics. */
export interface PrefillReport {
  /** Prompt length actually used for prefill modeling (tokens). */
  seq_len_used: number;
  /** Sum of compute+HBM(+interconnect) across the batched streams (seconds). */
  conservative_seconds_prefill: number;
  /** HBM contribution to prefill time (seconds). */
  hbm_seconds_prefill: number | null;
  /** Interconnect contribution during prefill (seconds); 0 on single GPU. */
  interconnect_seconds_prefill: number | null;
  /** Prompt tokens/sec over the entire batch (aggregate throughput). */
  conservative_prefill_tps: number;
}

// ------------------------------ Main API ------------------------------------

export function estimatePerformance(
  modelConfig: IModelConfig,
  availableGpuMemoryBytes: number[],
  availableGpuModel: GPUType,
  kv_data_type: KvDataType = 'fp16/bf16',
  weight_data_type: WeightDataType = 'fp4/int4',
  activation_data_type: ActivationDataType = 'fp16/bf16',
  advanced: AdvancedOpts = {},
  math: MathMode = { precision: 'bf16', accumulate: 'fp32' }
): ModelPerfReport {
  const availableGpuCount = availableGpuMemoryBytes.length;

  // -------------------- Read model hyperparameters ---------------------------
  const num_params = modelConfig.hfTotalSafetensors;
  const num_attention_heads = getHFConfigValue(modelConfig.hfConfig, [
    'num_attention_heads', 'n_head', 'n_heads', 'num_heads',
  ]);
  if (!num_attention_heads) throw Error('Number of attention heads not found');

  const num_key_value_heads = getHFConfigValue(
    modelConfig.hfConfig,
    ['num_key_value_heads', 'n_head_kv', 'num_kv_heads', 'multi_query_group_num', 'kv_n_head'],
    num_attention_heads
  );
  const hidden_size = getHFConfigValue(modelConfig.hfConfig, ['hidden_size', 'dim', 'n_embd']);
  if (!hidden_size) throw Error('Hidden size not found');

  const head_dim = getHFConfigValue(modelConfig.hfConfig, ['head_dim'], Math.ceil(hidden_size / num_attention_heads));
  const num_layers = getHFConfigValue(modelConfig.hfConfig, ['num_layers', 'n_layer', 'n_layers', 'num_hidden_layers']);
  if (!num_layers) throw Error('Number of layers not found');

  const intermediate_size = modelConfig.hfConfig.intermediate_size;

  // ------------------------ Topology and sequence lengths --------------------
  const t = Math.max(1, modelConfig.enablePartitioning ? modelConfig.partitions : 1);
  const singleGPU = availableGpuCount === 1 || t === 1;

  const L_prompt = Math.max(1, advanced.promptLen ?? modelConfig.maxContextSize);

  // ------------------------ Memory (per GPU) ---------------------------------
  const quantOverhead = weight_data_type === 'fp4/int4' ? (advanced.quantOverheadW4 ?? 1.1) : 1.0;
  const model_weight_total_bytes = num_params * PARAMETER_DATA_TYPE_SIZE[weight_data_type] * quantOverhead;
  const model_weight_per_gpu = model_weight_total_bytes / t;

  const activations_reserve_bytes_per_gpu = GiB(advanced.activationReserveGiB ?? 1);
  const non_torch_bytes_per_gpu = MiB(NON_TORCH_MEMORY_MB);
  const allocator_headroom = 0.9; // keep 10 percent free

  // ------------------------ KV block model and auto-batch --------------------
  const kv_dtype_size = KV_DATA_TYPE_SIZE[kv_data_type];
  const kv_is_sharded = advanced.kvIsShardedAcrossTP ?? true;
  const kv_divisor = kv_is_sharded ? t : 1;
  const kv_block = Math.max(1, advanced.kvBlockSize ?? 16);
  const kv_frag = Math.max(1, advanced.kvFragmentation ?? 1.05);

  const kvHeadsLocal = num_key_value_heads / kv_divisor;
  const BYTES_PER_BLOCK = 2 * kvHeadsLocal * head_dim * num_layers * kv_dtype_size * kv_block;

  const maxConcUsers = 1;
  const activeUsersInput = Math.max(1, advanced.activeUsers ?? maxConcUsers);
  const avgPromptLen = Math.max(1, advanced.avgPromptLen ?? Math.min(1024, L_prompt));
  const avgGenSoFar = Math.max(0, advanced.avgGenSoFar ?? 0);
  // FIX: previous code used Math.max(advanced.avgOutputLen, 100) which yields NaN if advanced.avgOutputLen is undefined.
  // Provide a safe default of 100 when not specified.
  const avgOutputLen = advanced.avgOutputLen != null ? Math.max(1, advanced.avgOutputLen) : 100;

  const fixed_per_gpu_preHeadroom = (model_weight_per_gpu + non_torch_bytes_per_gpu + activations_reserve_bytes_per_gpu);
  const fixed_per_gpu = fixed_per_gpu_preHeadroom / allocator_headroom;

  const blocksPerAvgUser = Math.ceil(avgPromptLen / kv_block) * kv_frag;
  const kv_bytes_per_avg_user = BYTES_PER_BLOCK * blocksPerAvgUser;

  const kvCapacitySeqsByGpu = availableGpuMemoryBytes.map((avail) => {
    const usable_for_kv = Math.max(0, avail - fixed_per_gpu);
    return Math.max(0, Math.floor(usable_for_kv / kv_bytes_per_avg_user));
  });
  const capByKV = Math.max(1, Math.min(...kvCapacitySeqsByGpu));

  const schedulerCap = Math.max(1, advanced.maxNumSeqsCap ?? Number.POSITIVE_INFINITY);
  const batchManual = advanced.batchEffective && advanced.batchEffective > 0 ? Math.floor(advanced.batchEffective) : undefined;
  const requestedBatchCap = maxConcUsers;
  const targetUsers = Math.max(1, Math.min(activeUsersInput, maxConcUsers));

  const useAuto = advanced.autoBatch ?? (advanced.batchEffective == null);
  const batchEffective = useAuto
    ? Math.max(1, Math.min(targetUsers, capByKV, requestedBatchCap, schedulerCap))
    : Math.max(1, Math.min(batchManual as number, targetUsers, schedulerCap));

  const effectiveActiveUsers = Math.max(1, Math.min(targetUsers, batchEffective));
  const totalResidentTokens = effectiveActiveUsers * (avgPromptLen + avgGenSoFar);

  const totalBlocks = Math.ceil(totalResidentTokens / kv_block) * kv_frag;
  const total_kv_cache_memory_per_gpu = BYTES_PER_BLOCK * totalBlocks;

  const required_gpu_memory_bytes_per_gpu =
    (model_weight_per_gpu + non_torch_bytes_per_gpu + activations_reserve_bytes_per_gpu + total_kv_cache_memory_per_gpu) /
    allocator_headroom;

  // Worst-case requirement for the declared config (maxContextSize), independent of gpuMemoryUtilization
  const wc_users = 1;
  const wc_L = Math.max(1, modelConfig.maxContextSize);
  const wc_totalTokens = wc_users * wc_L;
  const wc_totalBlocks = Math.ceil(wc_totalTokens / kv_block) * kv_frag;
  const wc_kv_bytes_per_gpu = BYTES_PER_BLOCK * wc_totalBlocks;
  const wc_required_bytes_per_gpu =
    (model_weight_per_gpu + non_torch_bytes_per_gpu + activations_reserve_bytes_per_gpu + wc_kv_bytes_per_gpu) /
    allocator_headroom;

  const fits_available_perGPU = availableGpuMemoryBytes.map((avail) => avail >= wc_required_bytes_per_gpu);
  const deficit_gb_perGPU = availableGpuMemoryBytes.map((avail) => {
    const d = wc_required_bytes_per_gpu - avail;
    return d > 0 ? d / GiB(1) : 0;
  });

  const concurrencyByGpu = kvCapacitySeqsByGpu;

  // ---------------- FLOPs per token (decode) ---------------------------------
  const d = hidden_size;
  const ffn = intermediate_size;
  const N = num_layers;

  const decodePrefixLenEff = Math.max(1, advanced.decodePrefixLen ?? (avgPromptLen + avgGenSoFar));

  const MAC_IS_2FLOPS = true;
  const flopsPerToken_total_MAC1 = N * (4 * d * d + 4 * d * ffn + 4 * d * decodePrefixLenEff);
  const flopsPerToken_total_2flop = MAC_IS_2FLOPS ? flopsPerToken_total_MAC1 * 2 : flopsPerToken_total_MAC1;
  const flopsPerToken_perGPU = flopsPerToken_total_2flop / t;

  const utilization = { ...DEFAULT_UTILIZATION_OPTS, ...(advanced.utilization ?? {}) };
  const peakTensorTFlopsDenseDefault = getGPUTensorThroughputDense(availableGpuModel, math.precision, math.accumulate);
  const peakTensorTFlopsDense = advanced.tensorPeaksOverrideTFlops ?? peakTensorTFlopsDenseDefault;

  const effTFLOPs = peakTensorTFlopsDense > 0 ? peakTensorTFlopsDense * utilization.computeUtilization : 0;
  const compute_ms_per_token = effTFLOPs > 0 ? (flopsPerToken_perGPU / (effTFLOPs * 1e12)) * 1e3 : null;

  // ---------------- HBM per token (decode) -----------------------------------
  const hbmGBs_peak_default = getGPUBandwidth(availableGpuModel);
  const hbmGBs_peak = advanced.gpuBandwidthGBsOverride ?? hbmGBs_peak_default;
  const hbmGBs_eff = hbmGBs_peak * utilization.hbmUtilization;

  const bytes_per_act_dtype = ACTIVATION_DATA_TYPE_SIZE[activation_data_type];
  const ACT_BYTES_PER_LAYER_PER_TOKEN_FACTOR = 8; // coarse knob

  const kvReadBytes_decode = 2 * kvHeadsLocal * head_dim * decodePrefixLenEff * kv_dtype_size; // read entire prefix
  const kvWriteBytes_decode = 2 * kvHeadsLocal * head_dim * kv_dtype_size; // write new token
  const actBytes_decode = ACT_BYTES_PER_LAYER_PER_TOKEN_FACTOR * (hidden_size / t) * bytes_per_act_dtype;

  const perLayerOnCardBytesPerTokenPerGPU_decode = kvReadBytes_decode + kvWriteBytes_decode + actBytes_decode;
  const totalOnCardBytesPerTokenPerGPU_decode = perLayerOnCardBytesPerTokenPerGPU_decode * N;

  const hbm_ms_per_token = hbmGBs_eff > 0 ? (totalOnCardBytesPerTokenPerGPU_decode / (hbmGBs_eff * GiB(1))) * 1e3 : null;

  // ---------------- Interconnect per token (decode) [NO P2P] -----------------
  let interconnect_ms_per_token: number | null = 0;
  let collective_latency_ms_per_token = 0;
  const perTokenMsFloor = 3.0; // fixed kernel/launch/queueing floor per token
  const perCollectiveUs = 25; // NCCL latency per collective

  if (!singleGPU) {
    const S_local_decode = batchEffective * (hidden_size / t) * bytes_per_act_dtype; // one token per step
    const collectivesPerLayerFwd = 2;
    const collectivesPerToken = collectivesPerLayerFwd * N;
    collective_latency_ms_per_token = (perCollectiveUs * collectivesPerToken) / 1000.0;

    const perCollectiveFactorNoP2P = (2 * (t - 1)) / t * 2; // ring times host-bounce
    const perLayerBytesDecodePerGPU_NoP2P = perCollectiveFactorNoP2P * S_local_decode * collectivesPerLayerFwd;
    const totalDecodeBytesPerTokenPerGPU = perLayerBytesDecodePerGPU_NoP2P * N;

    const effGBsPerGPU_raw = estimateNoP2PEffectiveGBsPerGPU(availableGpuCount, { ...DEFAULT_INTERCONNECT_OPTS, ...(advanced.interconnect ?? {}) });
    const effGBsPerGPU = effGBsPerGPU_raw * utilization.interconnectUtilization;
    interconnect_ms_per_token = (totalDecodeBytesPerTokenPerGPU / (effGBsPerGPU * GiB(1))) * 1e3;
  } else {
    interconnect_ms_per_token = 0;
  }

  // ---------------- Combine into decode tokens/sec ---------------------------
  const latency_ms_per_token = perTokenMsFloor + collective_latency_ms_per_token;
  const compute_ms = compute_ms_per_token ?? 0;
  const hbm_ms = hbm_ms_per_token ?? 0;
  const ic_ms = interconnect_ms_per_token ?? 0;

  const compute_ms_per_step = compute_ms * batchEffective;
  const hbm_ms_per_step = hbm_ms * batchEffective;
  const interconnect_ms_per_step = ic_ms * batchEffective;

  const conservative_ms_decode_agg = compute_ms_per_step + hbm_ms_per_step + interconnect_ms_per_step + latency_ms_per_token;
  const conservative_tps_aggregate = conservative_ms_decode_agg > 0 ? (1000 * batchEffective) / conservative_ms_decode_agg : null;

  const legs: Array<{ name: 'compute' | 'hbm' | 'interconnect'; ms: number }> = [
    { name: 'compute', ms: compute_ms },
    { name: 'hbm', ms: hbm_ms },
    { name: 'interconnect', ms: ic_ms },
  ];
  legs.sort((a, b) => b.ms - a.ms);
  const dominant_leg = legs[0];

  // ------------------------ Prefill (prompt) modeling ------------------------
  // OLD (overestimated): const L = Math.max(1, advanced.avgPromptLen ?? L_prompt);
  // Use the already computed avgPromptLen (capped at 1024 unless user overrides) to avoid O(L^2) blow-up at max context (e.g. 128k).
  const L = avgPromptLen;
  const flopsPrefill_total_MAC1 = N * ((4 * d * d + 4 * d * ffn) * L + 4 * d * (L * (L + 1)) / 2);
  const flopsPrefill_total = MAC_IS_2FLOPS ? flopsPrefill_total_MAC1 * 2 : flopsPrefill_total_MAC1;
  const flopsPrefill_perGPU = flopsPrefill_total / t;
  const compute_seconds_prefill = effTFLOPs > 0 ? flopsPrefill_perGPU / (effTFLOPs * 1e12) : null;

  // Prefill KV and act traffic (linear in L, not triangular per-token)
  const kvRead_sum = 2 * kvHeadsLocal * head_dim * kv_dtype_size * L; // read K and V once
  const kvWrite_sum = 2 * kvHeadsLocal * head_dim * kv_dtype_size * L; // write K and V once
  const act_sum = ACT_BYTES_PER_LAYER_PER_TOKEN_FACTOR * (hidden_size / t) * bytes_per_act_dtype * L;
  const perLayerOnCardBytesPrefillPerGPU = (kvRead_sum + kvWrite_sum + act_sum) * kv_frag;
  const totalOnCardBytesPrefillPerGPU = perLayerOnCardBytesPrefillPerGPU * N;

  const hbm_seconds_prefill = hbmGBs_eff > 0 ? totalOnCardBytesPrefillPerGPU / (hbmGBs_eff * GiB(1)) : null;

  // Interconnect: no-P2P forward collectives during prefill
  let interconnect_seconds_prefill: number | null = 0;
  let latency_seconds_prefill = 0;
  if (!singleGPU) {
    const local_shard_elems_prefill = batchEffective * L * (hidden_size / t);
    const S_local_prefill = local_shard_elems_prefill * bytes_per_act_dtype;

    const collectivesPerLayerFwd = 2;
    const perCollectiveFactorNoP2P = (2 * (t - 1)) / t * 2; // ring times host-bounce
    const perLayerBytesPerGPU_NoP2P = perCollectiveFactorNoP2P * S_local_prefill * collectivesPerLayerFwd;
    const totalPrefillBytesPerGPU = perLayerBytesPerGPU_NoP2P * N;

    const effGBsPerGPU_raw = estimateNoP2PEffectiveGBsPerGPU(availableGpuCount, { ...DEFAULT_INTERCONNECT_OPTS, ...(advanced.interconnect ?? {}) });
    const effGBsPerGPU = effGBsPerGPU_raw * utilization.interconnectUtilization;
    interconnect_seconds_prefill = totalPrefillBytesPerGPU / (effGBsPerGPU * GiB(1));

    const total_collectives_prefill = collectivesPerLayerFwd * N; // not per token during prefill
    latency_seconds_prefill = (perCollectiveUs * total_collectives_prefill) / 1e6;
  } else {
    interconnect_seconds_prefill = 0;
  }

  // Prefill totals and throughput
  const compute_seconds_prefill_agg = (compute_seconds_prefill ?? 0) * batchEffective;
  const hbm_seconds_prefill_agg = (hbm_seconds_prefill ?? 0) * batchEffective;
  const interconnect_seconds_prefill_agg = (interconnect_seconds_prefill ?? 0) * batchEffective;

  const conservative_seconds_prefill_agg =
    compute_seconds_prefill_agg + hbm_seconds_prefill_agg + interconnect_seconds_prefill_agg + (latency_seconds_prefill ?? 0);

  const conservative_seconds_prefill_single =
    (compute_seconds_prefill ?? 0) + (hbm_seconds_prefill ?? 0) + (interconnect_seconds_prefill ?? 0) + (latency_seconds_prefill ?? 0);

  const conservative_prefill_tps_aggregate =
    conservative_seconds_prefill_agg > 0 ? (batchEffective * L) / conservative_seconds_prefill_agg : null;

  // ------------------------------ Output -------------------------------------
  const required_gpu_memory_gb_per_gpu = required_gpu_memory_bytes_per_gpu / GiB(1);

  const est_prefill_sys_seconds = conservative_seconds_prefill_single; // per-request
  const tps_per_stream = (conservative_tps_aggregate ?? 0) > 0 && batchEffective > 0 ? (conservative_tps_aggregate as number) / batchEffective : 0;
  const est_decode_sys_seconds = tps_per_stream > 0 ? avgOutputLen / tps_per_stream : Infinity;
  const est_total_seconds = est_prefill_sys_seconds + est_decode_sys_seconds;

  const safeRound = (x: number | null | undefined, p = 0) => (x ?? 0) > 0 ? roundUpTo(x as number, p) : 0;

  const safeRoundOrNull = (x: number | null | undefined, p = 0) => {
    if (x == null || !isFinite(x as number)) return null;
    return roundUpTo(x as number, p);
  };

  const safeFiniteOrNull = (x: number | null | undefined) => {
    if (x == null || !isFinite(x as number)) return null;
    return x as number;
  };

  // The report fields below are documented by ModelPerfReport and sub-interfaces above.
  return {
    topology: {
      gpuCount: availableGpuCount,
      tensorParallelDegree: t,
      singleGPU,
      batchEffective,
    },

    model: {
      num_params,
      weight_data_type,
      activation_data_type,
      kv_data_type,
      hidden_size,
      intermediate_size,
      num_key_value_heads,
      head_dim,
      num_layers,
    },

    memory: {
      perGPU_required_gb: roundUpTo(required_gpu_memory_gb_per_gpu, 1),
      perGPU_weights_gb: model_weight_per_gpu / GiB(1),
      perGPU_activation_reserve_gb: activations_reserve_bytes_per_gpu / GiB(1),
      perGPU_kv_cache_gb_estimated: total_kv_cache_memory_per_gpu / GiB(1),
      non_torch_overhead_gb: non_torch_bytes_per_gpu / GiB(1),
      allocator_headroom: 0.9,
      kv_block_size: kv_block,
      kv_bytes_per_block: BYTES_PER_BLOCK,
      avg_prompt_len: avgPromptLen,
      avg_generated_so_far: avgGenSoFar,
      active_users_input: activeUsersInput,
      effective_active_users: effectiveActiveUsers,
      kv_bytes_per_avg_user: kv_bytes_per_avg_user,
      capByKV_minAcrossGPUs: capByKV,
      concurrencyByGpu,
      worst_case: {
        prompt_len: wc_L,
        perGPU_kv_cache_gb: wc_kv_bytes_per_gpu / GiB(1),
        perGPU_required_gb: safeRoundOrNull(wc_required_bytes_per_gpu / GiB(1), 1),
        fits_available_perGPU,
        deficit_gb_perGPU,
      },
    },

    tokens_per_second: {
      math_mode: math,
      peakTensorTFlopsDense,
      effTFLOPs,
      flopsPerToken_total_2flop,
      flopsPerToken_perGPU,
      compute_ms_per_token,
      totalOnCardBytesPerTokenPerGPU: totalOnCardBytesPerTokenPerGPU_decode,
      hbmGBs_peak,
      hbmGBs_eff,
      hbm_ms_per_token,
      interconnect_ms_per_token,
      collective_latency_ms_per_token,
      perToken_fixed_latency_ms: perTokenMsFloor,
      dominant_leg,
      conservative_tps: safeRound(conservative_tps_aggregate, 0),
      avg_output_len: avgOutputLen,
      prefill_time_seconds: safeRoundOrNull(est_prefill_sys_seconds, 0),
      prefill_time_seconds_aggregate: safeRoundOrNull(conservative_seconds_prefill_agg, 0),
      decode_time_seconds: safeRoundOrNull(est_decode_sys_seconds, 0),
      gen_time_seconds: safeRoundOrNull(est_total_seconds, 0),
    },

    prefill: {
      seq_len_used: L,
      conservative_seconds_prefill: roundUpTo(conservative_seconds_prefill_agg, 0),
      hbm_seconds_prefill,
      interconnect_seconds_prefill,
      conservative_prefill_tps: safeRound(conservative_prefill_tps_aggregate, 0),
    },
  };
}

// ------------------------------- Utilities ----------------------------------

function estimateNoP2PEffectiveGBsPerGPU(
  gpuCount: number,
  {
    pcieWidth = 16,
    pcieEfficiency = 0.85,
    hostDDRBandwidthGBs = 170,
    cpuSockets = 1,
  }: InterconnectOpts = {}
): number {
  const pcieSustained = pcieTheoreticalGBsPerDir(pcieWidth) * pcieEfficiency;
  const pcieBounceEffective = (pcieSustained / 2) * 0.85; // two hops plus overhead

  const gpusPerSocket = cpuSockets === 2 ? Math.max(1, Math.ceil(gpuCount / 2)) : gpuCount;
  const hostPerGpu = (hostDDRBandwidthGBs / gpusPerSocket) * 0.85; // 15 percent stack overhead
  const hostBounceEffective = hostPerGpu / 2; // read + write through DRAM

  return Math.max(1, Math.min(pcieBounceEffective, hostBounceEffective));
}

function pcieTheoreticalGBsPerDir(width: number): number {
  // Gen5 x16 approx 64 GB/s per direction; scale by width
  return 64 * (width / 16);
}

function getGPUBandwidth(gpuModel: string): number {
  if (!gpuModel) return 0;
  // Conservative defaults; prefer overriding via AdvancedOpts.gpuBandwidthGBsOverride
  const bandwidthMap: Record<string, number> = {
    rtx5090: 1150,
    rtx6000pro: 960,
    rtx4070ti: 504,
  };
  return bandwidthMap[gpuModel.toLowerCase()] ?? 0;
}

function getGPUTensorThroughputDense(
  gpuModel: GPUType,
  precision: TensorPrecision,
  accumulate: AccumulateType
): number {
  const e = TENSOR_PEAKS_DENSE[gpuModel];
  if (!e) return 0;
  switch (precision) {
    case 'fp16':
      return accumulate === 'fp32' ? (e.fp16?.acc_fp32 ?? 0) : (e.fp16?.acc_fp16 ?? 0);
    case 'bf16':
      return e.bf16?.acc_fp32 ?? 0;
    case 'fp8':
      return accumulate === 'fp32' ? (e.fp8?.acc_fp32 ?? 0) : (e.fp8?.acc_fp16 ?? 0);
    case 'fp4':
      return e.fp4?.acc_fp32 ?? 0;
    case 'tf32':
      return e.tf32 ?? 0;
    case 'int8':
      return e.int8 ?? 0;
    default:
      return 0;
  }
}

function getHFConfigValue(cfg: any, keys: string[], fallback?: number) {
  for (const k of keys) {
    if (cfg && cfg[k] != null) return cfg[k];
  }
  return fallback;
}

export function availableGpuMemoryFromNodeStats(
  stats: NodeStats,
  units: 'bytes' | 'MiB' | 'GiB' = 'MiB'
): number[] {
  const result: number[] = [];
  if (!stats) {
    return [];
  }

  const mul = units === 'bytes' ? 1 : (units === 'GiB' ? 1024 * 1024 * 1024 : 1024 * 1024);
  for (let i = 0; i < stats.gpuCount; i++) {
    const key = String(i);
    const availableGpuMem = (stats as any).gpuMemoryFree[key];
    result.push(availableGpuMem * mul);
  }
  return result;
}

function MiB(val: number) {
  return val * 1024 * 1024;
}

function GiB(val: number) {
  return val * 1024 * 1024 * 1024;
}
