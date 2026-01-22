import {
  Component, computed, effect, EventEmitter, input, OnDestroy, Output, Signal,
  ViewChild
} from '@angular/core';
import {MetricService} from '../../../../shared/service/k8s/metric.service';
import {takeUntil, debounceTime} from 'rxjs/operators';
import {lastValueFrom, Subject, Subscription} from 'rxjs';
import {CommonModule} from '@angular/common';
import {IApplication} from '../../../../shared/model/application.model';
import {ChartModule} from 'primeng/chart';
import {TooltipModule} from 'primeng/tooltip';
import {TagModule} from 'primeng/tag';
import {CardModule} from 'primeng/card';
import {PageLoadComponent} from '../../../../shared/components/page-load/page-load.component';
import {LayoutService} from '../../../../shared/service/theme/app-layout.service';
import {Dropdown, DropdownModule} from 'primeng/dropdown';
import {FormsModule} from '@angular/forms';
import {IModelWorkerMetric} from '../../../../shared/model/k8s/metric.model';
import {SseEvent, SseEventTypeTimeout} from '../../../../shared/model/k8s/event.model';
import {SelectItem} from 'primeng/api/selectitem';
import {Store} from '@ngxs/store';
import {Selectors} from '../../../../shared/state/selectors';
import {AppStatusWithResources, appStatusWithContainers} from '../../../../shared/model/k8s/app-status.model';
import {MODEL_COMPONENT_WORKER} from "../../../../config/constant/model.constants";

interface WorkerTimeSeriesData {
  requestsData?: any;
  tokensData?: any;
  timingData?: any;
  cacheData?: any;
}

interface WorkerCurrentValues {
  requestsRunning: number;
  requestsWaiting: number;
  kvCacheUsage: number;
  promptTokensPerSec: number;
  generationTokensPerSec: number;
  timeToFirstToken: number;
  timePerOutputToken: number;
  prefillTime: number;
  decodeTime: number;
  lastUpdated: Date;
}

@Component({
  selector: 'sm-worker-metrics',
  standalone: true,
  // Removed ChangeDetectionStrategy.OnPush to fix chart update issues
  imports: [
    CommonModule,
    ChartModule,
    TooltipModule,
    TagModule,
    CardModule,
    PageLoadComponent,
    DropdownModule,
    FormsModule
  ],
  templateUrl: './worker-metrics.component.html'
})
export class WorkerMetricsComponent implements OnDestroy {
  application = input.required<IApplication>();
  @ViewChild('containerDropdown') containerDropdown!: Dropdown;
  @Output() metricsTimeout = new EventEmitter<any>();

  status: Signal<AppStatusWithResources | null>;
  containers: Signal<SelectItem<string>[]>;

  selectedContainer: string;
  timeSeriesData: WorkerTimeSeriesData | null = null;
  currentValues: WorkerCurrentValues | null = null;

  chartOptions: any;
  loading = false;

  private subscription?: Subscription;
  private destroy$ = new Subject<void>();
  private dataBuffer: IModelWorkerMetric[] = [];
  private readonly MAX_DATA_POINTS = 50;

  constructor(
    private metricService: MetricService,
    private layoutService: LayoutService,
    private store: Store
  ) {
    this.initChartOptions();

    this.layoutService.configUpdate$.pipe(
      takeUntil(this.destroy$),
      debounceTime(50)
    ).subscribe(() => this.initChartOptions());

    this.status = computed(() => {
      const app = this.application();
      const resourceStatus = this.store.selectSignal(Selectors.resourceStatus(app.id + '-' + MODEL_COMPONENT_WORKER))();

      if (!resourceStatus) {
        return null;
      }

      return appStatusWithContainers(app, resourceStatus);
    });

    this.containers = computed(() => {
      const status = this.status();

      if (!status || !status.resourceStatus) {
        return [];
      }

      return status.resourceStatus
        .flatMap((rs, idx) =>
          rs.containerStatuses?.map(cs => ({
            label: `Replica ${idx}: ${cs.containerName}`,
            value: `${idx}/${rs.podName}/${cs.containerName}`
          })) || []
        );
    });

    effect(async () => {
      const containers = this.containers();
      // If no container is selected but containers are available, select the first one
      if (containers.length && !this.selectedContainer) {
        const firstContainer = containers[0].value;
        this.selectedContainer = firstContainer;
        await this.initMetrics(firstContainer);

        if (this.containerDropdown) {
          this.containerDropdown.updateModel(firstContainer);
        }
      }
      // If tab becomes active and we have a selected container but no active subscription
      else if (this.selectedContainer && !this.subscription) {
        await this.initMetrics(this.selectedContainer);
      }
    });
  }

  onContainerChange(event: any): void {
    if (event.value) {
      this.initMetrics(event.value);
    }
  }

  private handleMetricEvent(event: SseEvent<IModelWorkerMetric>): void {
    if (event.type === SseEventTypeTimeout) {
      this.metricsTimeout.emit();
      return;
    }

    if (event.type === 'metrics' && event.data) {
      this.updateMetrics(event.data);
    }
  }

  private updateMetrics(newMetric: IModelWorkerMetric): void {
    // Always update current values immediately for real-time display
    this.currentValues = {
      requestsRunning: newMetric.requestsRunning || 0,
      requestsWaiting: newMetric.requestsWaiting || 0,
      kvCacheUsage: newMetric.kvCacheUsage || 0,
      promptTokensPerSec: newMetric.promptTokensPerSec || 0,
      generationTokensPerSec: newMetric.generationTokensPerSec || 0,
      timeToFirstToken: newMetric.timeToFirstToken || 0,
      timePerOutputToken: newMetric.timePerOutputToken || 0,
      prefillTime: newMetric.prefillTime || 0,
      decodeTime: newMetric.decodeTime || 0,
      lastUpdated: new Date()
    };

    // Always add to buffer
    this.dataBuffer.push(newMetric);
    if (this.dataBuffer.length > this.MAX_DATA_POINTS) {
      this.dataBuffer.shift();
    }

    this.updateTimeSeriesCharts();
  }

  private updateTimeSeriesCharts(): void {
    if (!this.timeSeriesData) {
      this.timeSeriesData = {};
    }

    const timestamps = this.dataBuffer.map((_, index) =>
      new Date(Date.now() - (this.dataBuffer.length - index - 1) * 1000).toLocaleTimeString()
    );

    // Direct assignment for better compatibility with default change detection
    this.timeSeriesData.requestsData = {
      labels: timestamps,
      datasets: [{
        label: 'Running',
        data: this.dataBuffer.map(m => m.requestsRunning || 0),
        borderColor: '#42A5F5',
        backgroundColor: 'rgba(66, 165, 245, 0.1)',
        tension: 0.4,
        fill: false,
        animation: false
      }, {
        label: 'Waiting',
        data: this.dataBuffer.map(m => m.requestsWaiting || 0),
        borderColor: '#FFA726',
        backgroundColor: 'rgba(255, 167, 38, 0.1)',
        tension: 0.4,
        fill: false,
        animation: false
      }]
    };

    this.timeSeriesData.tokensData = {
      labels: timestamps,
      datasets: [{
        label: 'Prompt Tokens/sec',
        data: this.dataBuffer.map(m => m.promptTokensPerSec || 0),
        borderColor: '#26C6DA',
        backgroundColor: 'rgba(38, 198, 218, 0.1)',
        tension: 0.4,
        fill: false,
        animation: false
      }, {
        label: 'Generation Tokens/sec',
        data: this.dataBuffer.map(m => m.generationTokensPerSec || 0),
        borderColor: '#AB47BC',
        backgroundColor: 'rgba(171, 71, 188, 0.1)',
        tension: 0.4,
        fill: false,
        animation: false
      }]
    };

    this.timeSeriesData.timingData = {
      labels: timestamps,
      datasets: [{
        label: 'Time to First Token (ms)',
        data: this.dataBuffer.map(m => m.timeToFirstToken || 0),
        borderColor: '#66BB6A',
        backgroundColor: 'rgba(102, 187, 106, 0.1)',
        tension: 0.4,
        fill: false,
        animation: false
      }, {
        label: 'Time per Output Token (ms)',
        data: this.dataBuffer.map(m => m.timePerOutputToken || 0),
        borderColor: '#EF5350',
        backgroundColor: 'rgba(239, 83, 80, 0.1)',
        tension: 0.4,
        fill: false,
        animation: false
      }]
    };

    this.timeSeriesData.cacheData = {
      labels: timestamps,
      datasets: [{
        label: 'KV Cache Usage (%)',
        data: this.dataBuffer.map(m => (m.kvCacheUsage || 0) * 100),
        borderColor: '#FF7043',
        backgroundColor: 'rgba(255, 112, 67, 0.1)',
        tension: 0.4,
        fill: true,
        animation: false
      }]
    };
  }

  getRequestsSeverity(): 'success' | 'info' | 'warning' | 'danger' {
    if (!this.currentValues) return 'info';
    const total = this.currentValues.requestsRunning + this.currentValues.requestsWaiting;
    if (total > 10) return 'warning';
    if (total > 5) return 'info';
    return 'success';
  }

  getCacheSeverity(): 'success' | 'info' | 'warning' | 'danger' {
    if (!this.currentValues) return 'info';
    const usage = this.currentValues.kvCacheUsage * 100;
    if (usage > 90) return 'danger';
    if (usage > 70) return 'warning';
    if (usage > 50) return 'info';
    return 'success';
  }

  async refreshConnection(): Promise<void> {
    if (this.selectedContainer) {
      await this.initMetrics(this.selectedContainer);
    }
  }

  async initMetrics(selected: string): Promise<void> {
    if (this.selectedContainer && this.subscription) {
      const [replica, podName, containerName] = this.selectedContainer.split('/');
      this.subscription.unsubscribe();
      try {
        await lastValueFrom(this.metricService.stopModelWorkerMetrics(
          this.application().id,
          podName,
          containerName
        ));
      } catch (error) {
        console.warn('Error stopping previous worker metrics:', error);
      }
    }

    this.selectedContainer = selected;
    const [replica, podName, containerName] = selected.split('/');

    // Clear previous data
    this.dataBuffer = [];
    this.currentValues = null;
    this.timeSeriesData = null;

    try {
      this.subscription = this.metricService
        .connectToModelWorkerMetrics(this.application().id, podName, containerName)
        .pipe(
          takeUntil(this.destroy$)
          // No debouncing - process all SSE events immediately
        )
        .subscribe({
          next: (event) => this.handleMetricEvent(event),
          error: (error) => {
            console.error('Worker metrics subscription error:', error);
          }
        });
    } catch (error) {
      console.error('Failed to establish worker metrics connection:', error);
    }
  }

  private initChartOptions(): void {
    const documentStyle = getComputedStyle(document.documentElement);

    this.chartOptions = {
      responsive: true,
      maintainAspectRatio: false,
      interaction: {
        intersect: false,
        mode: 'index'
      },
      plugins: {
        legend: {
          labels: {
            color: documentStyle.getPropertyValue('--text-color'),
            usePointStyle: true,
            boxHeight: 8
          }
        }
      },
      scales: {
        x: {
          ticks: {
            color: documentStyle.getPropertyValue('--text-color-secondary'),
            maxTicksLimit: 8
          },
          grid: {
            color: documentStyle.getPropertyValue('--surface-border'),
            drawBorder: false
          }
        },
        y: {
          beginAtZero: true,
          ticks: {
            color: documentStyle.getPropertyValue('--text-color-secondary')
          },
          grid: {
            color: documentStyle.getPropertyValue('--surface-border'),
            drawBorder: false
          }
        }
      }
    };
  }

  async ngOnDestroy(): Promise<void> {
    if (this.subscription) {
      this.subscription.unsubscribe();
    }

    this.destroy$.next();
    this.destroy$.complete();

    if (this.selectedContainer) {
      const [replica, podName, containerName] = this.selectedContainer.split('/');
      try {
        await lastValueFrom(this.metricService.stopModelWorkerMetrics(
          this.application().id,
          podName,
          containerName
        ));
      } catch (error) {
        console.warn('Error stopping worker metrics on destroy:', error);
      }
    }
  }
}
