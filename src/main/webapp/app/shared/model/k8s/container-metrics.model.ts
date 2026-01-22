import { IMetric } from './metric.model';
import dayjs from 'dayjs/esm';
import { uniq } from 'lodash';

export const TIME_FORMAT = 'HH:mm:ss';
export const NUM_ELEMS = 60;

export class ChartData {
  cpuData: any;
  memoryData: any;
  networkData: any;

  constructor() {
    this.initData();
  }

  public addDataPoints(metric: IMetric) {
    if (!metric) {
      return;
    }

    const cpuTimestamps = Object.keys(metric.cpu)
      .map(key => Number(key)*1000)
      .sort((a, b) => a-b)
      .map(key => dayjs(key).format(TIME_FORMAT));
    const cpuValues = Object.values(metric.cpu);

    const memoryTimestamps = Object.keys(metric.memory)
      .map(key => Number(key)*1000)
      .sort((a, b) => a-b)
      .map(key => dayjs(key).format(TIME_FORMAT));

    const memoryValues = Object.values(metric.memory)
      .map(value => Number(value) / (1024 * 1024));

    let networkTimestamps = uniq([...Object.keys(metric.networkIn), ...Object.keys(metric.networkOut)])
      .map(key => Number(key)*1000)
      .sort((a, b) => a-b)
      .map(key => dayjs(key).format(TIME_FORMAT));

    const networkInValues = Object.values(metric.networkIn);
    const networkOutValues = Object.values(metric.networkOut);

    // this.times.push(created);
    this.cpuData.labels.push(...cpuTimestamps);
    this.memoryData.labels.push(...memoryTimestamps);
    this.networkData.labels.push(...networkTimestamps);

    this.cpuData.datasets[0].data.push(...cpuValues);
    this.memoryData.datasets[0].data.push(...memoryValues);
    this.networkData.datasets[0].data.push(...networkInValues);
    this.networkData.datasets[1].data.push(...networkOutValues);

    this.cpuData.labels = this.cpuData.labels.slice(-NUM_ELEMS);
    this.memoryData.labels = this.memoryData.labels.slice(-NUM_ELEMS);
    this.networkData.labels = this.networkData.labels.slice(-NUM_ELEMS);
    this.cpuData.datasets[0].data = this.cpuData.datasets[0].data.slice(-NUM_ELEMS);
    this.memoryData.datasets[0].data = this.memoryData.datasets[0].data.slice(-NUM_ELEMS);
    this.networkData.datasets[0].data = this.networkData.datasets[0].data.slice(-NUM_ELEMS);
    this.networkData.datasets[1].data = this.networkData.datasets[1].data.slice(-NUM_ELEMS);

    this.cpuData = {...this.cpuData};
    this.memoryData = {...this.memoryData};
    this.networkData = {...this.networkData};
  }

  private initData() {
    this.cpuData = {
        labels: [] as string[],
        datasets: [
          {
            label: 'CPU',
            data: [] as number[],
            fill: true,
            animation: false,
            backgroundColor: 'rgba(215,2,6,0.1)',
            borderColor: '#d70206',
            pointStyle: false,
          }
        ]
      };
    this.memoryData = {
        labels: [] as string[],
        datasets: [
          {
            label: 'Memory (MB)',
            data: [] as number[],
            fill: true,
            animation: false,
            backgroundColor: 'rgba(244,198,61,0.1)',
            borderColor: 'rgba(244,198,61)',
            pointStyle: false,
          }
        ]
      };
    this.networkData = {
      labels: [] as string[],
      datasets: [
        {
          label: 'Network OUT (bytes)',
          data: [] as number[],
          fill: true,
          animation: false,
          backgroundColor: 'rgba(215,2,6,0.1)',
          borderColor: '#d70206',
          pointStyle: false,
        },
        {
          label: 'Network IN (bytes)',
          data: [] as number[],
          fill: true,
          animation: false,
          backgroundColor: 'rgba(135,237,130,0.1)',
          borderColor: 'rgba(135,237,130)',
          pointStyle: false,
        }
      ]
    };
  }
}
