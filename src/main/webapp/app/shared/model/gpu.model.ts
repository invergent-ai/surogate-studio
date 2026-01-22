export interface GpuCard {
  id: number;
  model?: string; // GPU model name
  gpuUsage?: number; // Percentage of GPU usage
  gpuMemory?: number; // Total GPU memory in Mb
  gpuMemoryUsage?: number; // GPU memory usage
  gpuMemoryFree?: number; // GPU memory usage
  gpuTemperature?: number; // Temperature in Celsius
  gpuPowerUsage?: number; // Power usage in Watts
}
