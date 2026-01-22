export interface IRayClusterShape {
  numNodes?: number;
  gpusPerWorker?: number;
  headGpus?: number; // (Optional) Must be set if test vllm chat is needed or useHeadAsWorker = true
  useHeadAsWorker?: boolean; // (Optional) Can be true only if numNodes > 1 and headGpus >= gpusPerWorker
  testVllmTp?: number; // (Optional) Must be set if test vllm chat is needed. Must be <= headGpus
}

