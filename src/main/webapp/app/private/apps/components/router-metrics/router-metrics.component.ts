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
import {IModelRouterMetric} from '../../../../shared/model/k8s/metric.model';
import {SseEvent, SseEventTypeTimeout} from '../../../../shared/model/k8s/event.model';
import {SelectItem} from 'primeng/api/selectitem';
import {Store} from '@ngxs/store';
import {Selectors} from '../../../../shared/state/selectors';
import {AppStatusWithResources, appStatusWithContainers} from '../../../../shared/model/k8s/app-status.model';
import {MODEL_COMPONENT_ROUTER} from "../../../../config/constant/model.constants";

interface RouterTimeSeriesData {
  requestsPerSecData?: any;
  latencyData?: any;
  errorRateData?: any;
  systemResourcesData?: any;
  performanceData?: any;
  latencyLoadData?: any;
  healthData?: any;
}

interface RouterCurrentValues {
  requestsPerSec: number;
  avgLatency: number;
  activeConnections: number;
  workersHealthy: number;
  averageWorkerLoad: number;
  cpuUsage: number;
  memoryUsage: number;
  diskUsage: number;
  errorRate: number;
  timeoutRate: number;
  lastUpdated: Date;
}

@Component({
  selector: 'sm-router-metrics',
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
  templateUrl: './router-metrics.component.html'
})
export class RouterMetricsComponent implements OnDestroy {
  application = input.required<IApplication>();
  @ViewChild('containerDropdown') containerDropdown!: Dropdown;
  @Output() metricsTimeout = new EventEmitter<any>();

  status: Signal<AppStatusWithResources | null>;
  containers: Signal<SelectItem<string>[]>;

  selectedContainer: string;
  timeSeriesData: RouterTimeSeriesData | null = null;
  currentValues: RouterCurrentValues | null = null;

  chartOptions: any;
  loading = false;

  private subscription?: Subscription;
  private destroy$ = new Subject<void>();
  private dataBuffer: IModelRouterMetric[] = [];
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
      const resourceStatus = this.store.selectSignal(Selectors.resourceStatus(app.id + '-' + MODEL_COMPONENT_ROUTER))();

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
      if (containers.length && !this.selectedContainer) {
        const firstContainer = containers[0].value;
        this.selectedContainer = firstContainer;
        await this.initMetrics(firstContainer);

        if (this.containerDropdown) {
          this.containerDropdown.updateModel(firstContainer);
        }
      } else if (this.selectedContainer && !this.subscription) {
        await this.initMetrics(this.selectedContainer);
      }
    });
  }

  onContainerChange(event: any): void {
    if (event.value) {
      this.initMetrics(event.value);
    }
  }

  // Formatting helper methods
  formatNumber(value: number, decimals: number = 0): string {
    if (value === null || value === undefined || isNaN(value)) {
      return '0';
    }
    return value.toFixed(decimals);
  }

  formatLatency(latencyInSeconds: number): string {
    if (!latencyInSeconds || latencyInSeconds === 0) {
      return '0ms';
    }

    const ms = latencyInSeconds * 1000;
    if (ms < 1000) {
      return `${ms.toFixed(0)}ms`;
    } else {
      return `${(ms / 1000).toFixed(2)}s`;
    }
  }

  formatPercentage(value: number): string {
    if (value === null || value === undefined || isNaN(value)) {
      return '0%';
    }
    return `${value.toFixed(1)}%`;
  }

  // Severity assessment methods
  getResourceSeverity(usage: number): 'success' | 'info' | 'warning' | 'danger' {
    if (usage > 90) return 'danger';
    if (usage > 75) return 'warning';
    if (usage > 50) return 'info';
    return 'success';
  }

  getWorkerLoadSeverity(): 'success' | 'info' | 'warning' | 'danger' {
    if (!this.currentValues) return 'info';
    const load = this.currentValues.averageWorkerLoad;
    if (load > 5.0) return 'danger';  // High QPS per worker
    if (load > 2.0) return 'warning';
    if (load > 0.5) return 'info';
    return 'success';
  }

  getConnectionsSeverity(): 'success' | 'info' | 'warning' | 'danger' {
    if (!this.currentValues) return 'info';
    const connections = this.currentValues.activeConnections;
    if (connections > 1000) return 'warning';
    if (connections > 500) return 'info';
    return 'success';
  }

  getLatencySeverity(): 'success' | 'info' | 'warning' | 'danger' {
    if (!this.currentValues) return 'info';
    const latencyMs = this.currentValues.avgLatency * 1000;
    if (latencyMs > 5000) return 'danger';    // > 5 seconds
    if (latencyMs > 2000) return 'warning';   // > 2 seconds
    if (latencyMs > 1000) return 'info';      // > 1 second
    return 'success';
  }

  getThroughputSeverity(): 'success' | 'info' | 'warning' | 'danger' {
    if (!this.currentValues) return 'info';
    const rps = this.currentValues.requestsPerSec;
    if (rps === 0) return 'warning';
    if (rps > 10) return 'success';
    if (rps > 1) return 'info';
    return 'warning';
  }

  private handleMetricEvent(event: SseEvent<IModelRouterMetric>): void {
    if (event.type === SseEventTypeTimeout) {
      this.metricsTimeout.emit();
      return;
    }

    if (event.type === 'metrics' && event.data) {
      this.updateMetrics(event.data);
    }
  }

  private updateMetrics(newMetric: IModelRouterMetric): void {
    // Always update current values immediately for real-time display
    this.currentValues = {
      requestsPerSec: newMetric.requestsPerSec || 0,
      avgLatency: newMetric.avgLatency || 0,
      activeConnections: newMetric.activeConnections || 0,
      workersHealthy: newMetric.workersHealthy || 0,
      averageWorkerLoad: newMetric.averageWorkerLoad || 0,
      cpuUsage: (newMetric as any).cpuUsage || 0,
      memoryUsage: (newMetric as any).memoryUsage || 0,
      diskUsage: (newMetric as any).diskUsage || 0,
      errorRate: newMetric.errorRate || 0,
      timeoutRate: newMetric.timeoutRate || 0,
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

    // Performance metrics chart
    this.timeSeriesData.performanceData = {
      labels: timestamps,
      datasets: [{
        label: 'Requests/sec',
        data: this.dataBuffer.map(m => m.requestsPerSec || 0),
        borderColor: '#42A5F5',
        backgroundColor: 'rgba(66, 165, 245, 0.1)',
        tension: 0.4,
        fill: false,
        yAxisID: 'y',
        animation: false
      }, {
        label: 'Active Connections',
        data: this.dataBuffer.map(m => m.activeConnections || 0),
        borderColor: '#26A69A',
        backgroundColor: 'rgba(38, 166, 154, 0.1)',
        tension: 0.4,
        fill: false,
        yAxisID: 'y1',
        animation: false
      }]
    };

    // Latency & Worker Load chart
    this.timeSeriesData.latencyLoadData = {
      labels: timestamps,
      datasets: [{
        label: 'Avg Latency (ms)',
        data: this.dataBuffer.map(m => (m.avgLatency || 0) * 1000),
        borderColor: '#FFA726',
        backgroundColor: 'rgba(255, 167, 38, 0.1)',
        tension: 0.4,
        fill: false,
        yAxisID: 'y',
        animation: false
      }, {
        label: 'Worker Load (QPS)',
        data: this.dataBuffer.map(m => m.averageWorkerLoad || 0),
        borderColor: '#EC407A',
        backgroundColor: 'rgba(236, 64, 122, 0.1)',
        tension: 0.4,
        fill: false,
        yAxisID: 'y1',
        animation: false,
      }]
    };

    // System resources chart
    this.timeSeriesData.systemResourcesData = {
      labels: timestamps,
      datasets: [{
        label: 'CPU Usage (%)',
        data: this.dataBuffer.map(m => (m as any).cpuUsage || 0),
        borderColor: '#66BB6A',
        backgroundColor: 'rgba(102, 187, 106, 0.1)',
        tension: 0.4,
        fill: false,
        animation: false,
      }, {
        label: 'Memory Usage (%)',
        data: this.dataBuffer.map(m => (m as any).memoryUsage || 0),
        borderColor: '#AB47BC',
        backgroundColor: 'rgba(171, 71, 188, 0.1)',
        tension: 0.4,
        fill: false,
        animation: false,
      }, {
        label: 'Disk Usage (%)',
        data: this.dataBuffer.map(m => (m as any).diskUsage || 0),
        borderColor: '#FF7043',
        backgroundColor: 'rgba(255, 112, 67, 0.1)',
        tension: 0.4,
        fill: false,
        animation: false,
      }]
    };

    // Error rates chart
    this.timeSeriesData.errorRateData = {
      labels: timestamps,
      datasets: [{
        label: 'Error Rate (%)',
        data: this.dataBuffer.map(m => (m.errorRate || 0) * 100),
        borderColor: '#EF5350',
        backgroundColor: 'rgba(239, 83, 80, 0.1)',
        tension: 0.4,
        fill: false,
        animation: false,
      }, {
        label: 'Timeout Rate (%)',
        data: this.dataBuffer.map(m => (m.timeoutRate || 0) * 100),
        borderColor: '#FF7043',
        backgroundColor: 'rgba(255, 112, 67, 0.1)',
        tension: 0.4,
        fill: false,
        animation: false,
      }]
    };

    // Health metrics chart
    this.timeSeriesData.healthData = {
      labels: timestamps,
      datasets: [{
        label: 'Healthy Workers',
        data: this.dataBuffer.map(m => m.workersHealthy || 0),
        borderColor: '#4CAF50',
        backgroundColor: 'rgba(76, 175, 80, 0.1)',
        tension: 0.4,
        fill: false,
        stepped: true, // Since worker count is discrete
        animation: false,
      }]
    };
  }

  async refreshConnection(): Promise<void> {
    if (this.selectedContainer) {
      await this.initMetrics(this.selectedContainer);
    }
  }

  public initDualAxisChartOptions(): any {
    const documentStyle = getComputedStyle(document.documentElement);

    return {
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
          type: 'linear',
          display: true,
          position: 'left',
          beginAtZero: true,
          ticks: {
            color: documentStyle.getPropertyValue('--text-color-secondary')
          },
          grid: {
            color: documentStyle.getPropertyValue('--surface-border'),
            drawBorder: false
          }
        },
        y1: {
          type: 'linear',
          display: true,
          position: 'right',
          beginAtZero: true,
          ticks: {
            color: documentStyle.getPropertyValue('--text-color-secondary')
          },
          grid: {
            drawOnChartArea: false,
          }
        }
      }
    };
  }

  private handleError(error: any): void {
    console.error('Router metrics error:', error);

    if (error.message && error.message.includes('SSE ErrorEvent')) {
      console.warn('SSE connection failed, possibly due to network issues or server unavailability');
      this.metricsTimeout.emit();
    }
  }

  async initMetrics(selected: string): Promise<void> {
    // Clean up existing connection if any
    if (this.selectedContainer && this.subscription) {
      const [replica, podName, containerName] = this.selectedContainer.split('/');
      this.subscription.unsubscribe();
      try {
        await lastValueFrom(this.metricService.stopModelRouterMetrics(
          this.application().id,
          podName,
          containerName
        ));
      } catch (error) {
        console.warn('Error stopping previous router metrics:', error);
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
        .connectToModelRouterMetrics(this.application().id, podName, containerName)
        .pipe(
          takeUntil(this.destroy$)
          // No debouncing - process all SSE events immediately
        )
        .subscribe({
          next: (event) => this.handleMetricEvent(event),
          error: (error) => this.handleError(error)
        });
    } catch (error) {
      console.error('Failed to establish router metrics connection:', error);
      this.handleError(error);
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
        await lastValueFrom(this.metricService.stopModelRouterMetrics(
          this.application().id,
          podName,
          containerName
        ));
      } catch (error) {
        console.warn('Error stopping router metrics on destroy:', error);
      }
    }
  }
}
