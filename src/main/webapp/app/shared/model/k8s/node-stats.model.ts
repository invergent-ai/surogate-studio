import {NodeStatus} from "../enum/node-status.model";

export type GpuMetricMap = Record<string, number>;

export interface NodeStats {
  rxMbps?: number;
  txMbps?: number;
  activeApps?: number;
  totalApps?: number;
  towerHeight?: number;
  status?: NodeStatus;
  uptime?: number;
  gpuCount?: number;
  gpuModel?: string;
  gpuMemory?: GpuMetricMap;
  gpuMemoryUsage?: GpuMetricMap;
}
