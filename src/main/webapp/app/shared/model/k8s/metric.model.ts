export interface IMetric {
  // Apps
  cpu: any;
  memory: any;
  disk: any;
  networkIn: any;
  networkOut: any;
  // Training
  epoch: any;
  gradNorm: any;
  learningRate: any;
  loss: any;
  evalLoss: any;
  tokensPerSecondPerGpu: any;
  // Meta
  type?: string;
  error?: string;
  created: string;
}

export interface IGpuMetric {
  gpuUsage: any;
  gpuMemoryUsage: any;
  gpuMemoryFree: any;
  gpuTemperature: any;
  gpuPowerUsage: any;
  gpuMemory: any;
  type?: string;
  error?: string;
  created: string;
}


export interface IModelWorkerMetric {
  requestsRunning?: number;
  requestsWaiting?: number;
  kvCacheUsage?: number;
  promptTokensPerSec?: number;
  generationTokensPerSec?: number;
  timeToFirstToken?: number;
  timePerOutputToken?: number;
  prefillTime?: number;
  decodeTime?: number;
  timestamp?: number;
}

export interface IModelRouterMetric {
  requestsPerSec?: number;
  avgLatency?: number;
  activeConnections?: number;
  workersHealthy?: number;
  averageWorkerLoad?: number;
  errorRate?: number;
  timeoutRate?: number;
  timestamp?: number;
  cpuUsage?: number;
  memoryUsage?: number;
  diskUsage?: number;
}
