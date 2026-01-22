import { IGpuMetric } from './metric.model';
import dayjs from 'dayjs/esm';

export const TIME_FORMAT = 'HH:mm:ss';
export const NUM_ELEMS = 60;

export class GpuChartData {
  usageData: any;
  memoryData: any;
  temperatureData: any;
  powerData: any;

  constructor() {
    this.initData();
  }

  public addDataPoints(metric: IGpuMetric) {
    if (!metric) {
      return;
    }

    const getFirstSeries = (obj: any) => {
      if (!obj) return null;
      const keys = Object.keys(obj);
      if (keys.length === 0) return null;
      return obj[keys[0]];
    };

    const usageSeries = getFirstSeries(metric.gpuUsage);
    if (usageSeries) {
      const sortedEntries: [number, number][] = Object.entries(usageSeries)
        .map(([k, v]) => [Number(k) * 1000, Number(v)] as [number, number])
        .sort((a, b) => a[0] - b[0]);

      const gpuUsageTimestamps = sortedEntries.map(([ts, _]) =>
        dayjs(ts).format(TIME_FORMAT)
      );
      const gpuUsageValues = sortedEntries.map(([_, val]) => val);

      this.usageData.labels = gpuUsageTimestamps;
      this.usageData.datasets[0].data = gpuUsageValues;
    }

    const memUsageSeries = getFirstSeries(metric.gpuMemoryUsage);
    const memTotalSeries = getFirstSeries(metric.gpuMemory);
    if (memUsageSeries && memTotalSeries) {
      const sortedEntries: [number, number][] = Object.entries(memUsageSeries)
        .map(([k, v]) => [Number(k) * 1000, Number(v)] as [number, number])
        .sort((a, b) => a[0] - b[0]);

      const gpuMemoryTimestamps = sortedEntries.map(([ts, _]) =>
        dayjs(ts).format(TIME_FORMAT)
      );
      const gpuMemoryValues = sortedEntries.map(([_, val]) => val);
      const gpuTotalMemoryValues = Object.values(memTotalSeries).map(v => Number(v));

      const gpuMemoryPercentValues = gpuMemoryValues.map((value, index) => {
        const total = gpuTotalMemoryValues[index] as number;
        return total ? (value / total * 100) : 0;
      });

      this.memoryData.labels = gpuMemoryTimestamps;
      this.memoryData.datasets[0].data = gpuMemoryPercentValues;
    }

    const tempSeries = getFirstSeries(metric.gpuTemperature);
    if (tempSeries) {
      const sortedEntries: [number, number][] = Object.entries(tempSeries)
        .map(([k, v]) => [Number(k) * 1000, Number(v)] as [number, number])
        .sort((a, b) => a[0] - b[0]);

      const gpuTemperatureTimestamps = sortedEntries.map(([ts, _]) =>
        dayjs(ts).format(TIME_FORMAT)
      );
      const gpuTemperatureValues = sortedEntries.map(([_, val]) => val);

      this.temperatureData.labels = gpuTemperatureTimestamps;
      this.temperatureData.datasets[0].data = gpuTemperatureValues;
    }

    const powerSeries = getFirstSeries(metric.gpuPowerUsage);
    if (powerSeries) {
      const sortedEntries = Object.entries(powerSeries)
        .map(([k, v]) => [Number(k) * 1000, Number(v)])
        .sort((a, b) => a[0] - b[0]);

      const gpuPowerTimestamps = sortedEntries.map(([ts]) =>
        dayjs(ts).format(TIME_FORMAT)
      );
      const gpuPowerValues = sortedEntries.map(([_, val]) => val);
      this.powerData.labels = gpuPowerTimestamps;
      this.powerData.datasets[0].data = gpuPowerValues;
    }

    this.usageData.labels = this.usageData.labels.slice(-NUM_ELEMS);
    this.memoryData.labels = this.memoryData.labels.slice(-NUM_ELEMS);
    this.temperatureData.labels = this.temperatureData.labels.slice(-NUM_ELEMS);
    this.powerData.labels = this.powerData.labels.slice(-NUM_ELEMS);

    this.usageData.datasets[0].data = this.usageData.datasets[0].data.slice(-NUM_ELEMS);
    this.memoryData.datasets[0].data = this.memoryData.datasets[0].data.slice(-NUM_ELEMS);
    this.temperatureData.datasets[0].data = this.temperatureData.datasets[0].data.slice(-NUM_ELEMS);
    this.powerData.datasets[0].data = this.powerData.datasets[0].data.slice(-NUM_ELEMS);

    this.usageData = { ...this.usageData };
    this.memoryData = { ...this.memoryData };
    this.temperatureData = { ...this.temperatureData };
    this.powerData = { ...this.powerData };
  }

  private initData() {
    this.usageData = {
      labels: [] as string[],
      datasets: [
        {
          label: 'GPU Usage (%)',
          data: [] as number[],
          fill: true,
          animation: false,
          backgroundColor: 'rgba(215,2,6,0.1)',
          borderColor: '#d70206',
          pointStyle: true,
        }
      ]
    };
    this.memoryData = {
      labels: [] as string[],
      datasets: [
        {
          label: 'Memory Usage (%)',
          data: [] as number[],
          fill: true,
          animation: false,
          backgroundColor: 'rgba(244,198,61,0.1)',
          borderColor: 'rgba(244,198,61)',
          pointStyle: true,
        }
      ]
    };
    this.temperatureData = {
      labels: [] as string[],
      datasets: [
        {
          label: 'Temperature (°C)',
          data: [] as number[],
          fill: true,
          animation: false,
          backgroundColor: 'rgba(0, 123, 255, 0.1)',
          borderColor: '#007bff',
          pointStyle: true,
        }
      ]
    };
    this.powerData = {
      labels: [] as string[],
      datasets: [
        {
          label: 'Power Usage (W)',
          data: [] as number[],
          fill: true,
          animation: false,
          backgroundColor: 'rgba(40, 167, 69, 0.1)',
          borderColor: '#28a745',
          pointStyle: true,
        }
      ]
    }
  }
}



