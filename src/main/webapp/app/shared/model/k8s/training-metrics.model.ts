import {IMetric} from './metric.model';

export const NUM_ELEMS = 10000; // for fixed limit

export class TrainingChartData {
  lossData: any;
  learningRateData: any;
  gradNormData: any;
  tokensPerSecondPerGpuData: any;

  constructor() {
    this.initData();
  }

  public addDataPoints(metric: IMetric) {
    if (!metric || !metric.loss ||
      !metric.learningRate || !metric.gradNorm || !metric.tokensPerSecondPerGpu) {
      return;
    }

    // Prepare labels & values (sort)
    const sortedLossEntries = Object.entries(metric.loss)
      .map(([k, v]) => [Number(k), v] as [number, number])
      .sort(([a], [b]) => a - b);
    const sortedLoss = new Map(sortedLossEntries);
    const lossLabels = sortedLoss.keys();
    const lossValues = sortedLoss.values();

    const sortedEvalLossEntries = metric.evalLoss ? Object.entries(metric.evalLoss)
      .map(([k, v]) => [Number(k), v] as [number, number])
      .sort(([a], [b]) => a - b) : null;
    const sortedEvalLoss = sortedEvalLossEntries ? new Map(sortedEvalLossEntries) : null;
    // const evalLossLabels = sortedEvalLoss ? sortedEvalLoss.keys() : null;
    const evalLossValues = sortedEvalLoss ? sortedEvalLoss.values() : null;

    const sortedLearningRateEntries = Object.entries(metric.learningRate)
      .map(([k, v]) => [Number(k), v] as [number, number])
      .sort(([a], [b]) => a - b);
    const sortedLearningRate = new Map(sortedLearningRateEntries);
    const learningRateLabels = sortedLearningRate.keys();
    const learningRateValues = sortedLearningRate.values();

    const sortedGradNormEntries = Object.entries(metric.gradNorm)
      .map(([k, v]) => [Number(k), v] as [number, number])
      .sort(([a], [b]) => a - b);
    const sortedGradNorm = new Map(sortedGradNormEntries);
    const gradNormLabels = sortedGradNorm.keys();
    const gradNormValues = sortedGradNorm.values();

    const sortedTpsEntries = Object.entries(metric.tokensPerSecondPerGpu)
      .map(([k, v]) => [Number(k), v] as [number, number])
      .sort(([a], [b]) => a - b);
    const sortedTps = new Map(sortedTpsEntries);
    const tpsLabels = sortedTps.keys();
    const tpsValues = sortedTps.values();
    // Push labels
    this.lossData.labels.push(...lossLabels);
    this.learningRateData.labels.push(...learningRateLabels);
    this.gradNormData.labels.push(...gradNormLabels);
    this.tokensPerSecondPerGpuData.labels.push(...tpsLabels);
    // Push values
    this.lossData.datasets[0].data.push(...lossValues);
    if (evalLossValues) {
      this.lossData.datasets[1].data.push(...evalLossValues);
    }
    this.learningRateData.datasets[0].data.push(...learningRateValues);
    this.gradNormData.datasets[0].data.push(...gradNormValues);
    this.tokensPerSecondPerGpuData.datasets[0].data.push(...tpsValues);
    // Limit labels & values
    this.lossData.labels = this.lossData.labels.slice(-NUM_ELEMS);
    this.learningRateData.labels = this.learningRateData.labels.slice(-NUM_ELEMS);
    this.gradNormData.labels = this.gradNormData.labels.slice(-NUM_ELEMS);
    this.tokensPerSecondPerGpuData.labels = this.tokensPerSecondPerGpuData.labels.slice(-NUM_ELEMS);
    this.lossData.datasets[0].data = this.lossData.datasets[0].data.slice(-NUM_ELEMS);
    this.lossData.datasets[1].data = this.lossData.datasets[1].data.slice(-NUM_ELEMS);
    this.learningRateData.datasets[0].data = this.learningRateData.datasets[0].data.slice(-NUM_ELEMS);
    this.gradNormData.datasets[0].data = this.gradNormData.datasets[0].data.slice(-NUM_ELEMS);
    this.tokensPerSecondPerGpuData.datasets[0].data = this.tokensPerSecondPerGpuData.datasets[0].data.slice(-NUM_ELEMS);
    // Reload
    this.lossData = {...this.lossData};
    this.learningRateData = {...this.learningRateData};
    this.gradNormData = {...this.gradNormData};
    this.tokensPerSecondPerGpuData = {...this.tokensPerSecondPerGpuData};
  }

  private initData() {
    this.lossData = {
        labels: [] as string[],
        datasets: [
          {
            label: 'Loss',
            data: [] as number[],
            fill: true,
            animation: false,
            backgroundColor: 'rgba(10,10,10,0.1)',
            borderColor: '#000000',
            pointStyle: false,
          },
          {
            label: 'Eval loss',
            data: [] as number[],
            fill: false,
            animation: false,
            backgroundColor: 'rgba(10,10,10,0.1)',
            borderColor: '#95c5d6',
            pointStyle: false,
          }
        ]
      };
    this.learningRateData = {
        labels: [] as string[],
        datasets: [
          {
            label: 'Learning rate',
            data: [] as number[],
            fill: true,
            animation: false,
            backgroundColor: 'rgba(244,198,61,0.1)',
            borderColor: 'rgba(244,198,61)',
            pointStyle: false,
          }
        ]
      };
    this.gradNormData = {
      labels: [] as string[],
      datasets: [
        {
          label: 'Gradient normalization',
          data: [] as number[],
          fill: true,
          animation: false,
          backgroundColor: 'rgba(215,2,6,0.1)',
          borderColor: '#d70206',
          pointStyle: false,
        }
      ]
    };
    this.tokensPerSecondPerGpuData = {
      labels: [] as string[],
      datasets: [
        {
          label: 'Tokens per second per GPU',
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
