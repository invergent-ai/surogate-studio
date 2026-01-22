import { Component, effect, input, Input, OnChanges, OnDestroy, OnInit, SimpleChanges } from '@angular/core';
import { PageLoadComponent } from '../../../../shared/components/page-load/page-load.component';
import { ChartModule } from 'primeng/chart';
import { GpuChartData } from '../../../../shared/model/k8s/gpu-metrics.model';
import { MetricService } from '../../../../shared/service/k8s/metric.service';
import { lastValueFrom, Subject, Subscription } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { displayWarning } from '../../../../shared/util/success.util';
import { displayError } from '../../../../shared/util/error.util';
import { Store } from '@ngxs/store';
import { SseEvent, SseEventTypeTimeout } from '../../../../shared/model/k8s/event.model';
import { IGpuMetric, IMetric } from '../../../../shared/model/k8s/metric.model';
import { ChartData } from '../../../../shared/model/k8s/container-metrics.model';
import { CardModule } from 'primeng/card';

@Component({
  standalone: true,
  selector: 'sm-gpu-stats',
  templateUrl: 'gpu-stats.component.html',
  imports: [
    PageLoadComponent,
    ChartModule,
    CardModule
  ]
})
export class GpuStatsComponent implements OnDestroy {
  nodeId = input.required<string>();
  gpuId = input.required<number>();

  gpuMetrics?: GpuChartData;
  lineOptions: any;
  lineOptionsWithMax100: any;
  lineOptionsWithMax600: any;
  subscription?: Subscription;
  destroy$ = new Subject<void>();

  constructor(
    private metricService: MetricService,
    private store: Store,
  ) {
    const documentStyle = getComputedStyle(document.documentElement);
    const textColorSecondary = documentStyle.getPropertyValue('--text-color-secondary');
    const surfaceBorder = documentStyle.getPropertyValue('--surface-border');

    this.lineOptions = {
      plugins: {
        legend: {
          display: false
        }
      },
      scales: {
        x: {
          ticks: {
            color: textColorSecondary
          },
          grid: {
            color: surfaceBorder,
            drawBorder: false
          },
        },
        y: {
          ticks: {
            color: textColorSecondary
          },
          grid: {
            color: surfaceBorder,
            drawBorder: false
          },
          min: 0
        },
      }
    };
    this.lineOptionsWithMax100 = {...this.lineOptions, scales: { y: { min: 0, max: 100 }}};
    this.lineOptionsWithMax600 = {...this.lineOptions, scales: { y: { min: 0, max: 500 }}};

    effect(async () => {
      if (this.nodeId() && this.gpuId() >= 0) {
        await this.initMetrics();
      }
    });
  }

  async initMetrics() {
    if (this.subscription) {
      this.subscription.unsubscribe();
      await lastValueFrom(this.metricService.stopGpuMetrics(this.nodeId(), this.gpuId()));
    }

    this.subscription = this.metricService.connectToGpuMetrics(this.nodeId(), this.gpuId())
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (event: SseEvent<IGpuMetric>) => {
          if (event.type === SseEventTypeTimeout) {
            this.handleMetricsTimeout();
          } else if (event.type === 'metrics') {
            this.gpuMetrics = new GpuChartData();
            this.gpuMetrics.addDataPoints(event.data);
          }
        },
        error: this.handleMetricsError.bind(this)
      });
  }

  public handleMetricsTimeout(): void {
    displayWarning(this.store, 'Connection terminated due to inactivity. Click the warning icon to reconnect.');
  }

  private handleMetricsError(error: any): void {
    displayError(this.store, error.message || 'An error occurred');
  }

  async ngOnDestroy() {
    if (this.subscription) {
      this.subscription.unsubscribe();
    }

    this.destroy$.next();
    this.destroy$.complete();

    await lastValueFrom(this.metricService.stopGpuMetrics(this.nodeId(), this.gpuId()));

  }
}
