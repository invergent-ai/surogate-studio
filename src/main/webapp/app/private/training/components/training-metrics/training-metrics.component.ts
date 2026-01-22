import {Component, effect, EventEmitter, input, OnDestroy, Output} from '@angular/core';
import {MessageService} from 'primeng/api';
import {MetricService} from '../../../../shared/service/k8s/metric.service';
import {takeUntil} from 'rxjs/operators';
import {lastValueFrom, Subject, Subscription} from 'rxjs';
import {CommonModule} from '@angular/common';
import {ChartModule} from 'primeng/chart';
import {TooltipModule} from 'primeng/tooltip';
import {TagModule} from 'primeng/tag';
import {PageLoadComponent} from '../../../../shared/components/page-load/page-load.component';
import {LayoutService} from '../../../../shared/service/theme/app-layout.service';
import {DropdownModule} from 'primeng/dropdown';
import {FormsModule} from '@angular/forms';
import {IMetric} from '../../../../shared/model/k8s/metric.model';
import {SseEvent, SseEventTypeTimeout} from '../../../../shared/model/k8s/event.model';
import {CardModule} from 'primeng/card';
import {TrainingChartData} from "../../../../shared/model/k8s/training-metrics.model";
import {IJob} from "../../../../shared/model/job.model";

@Component({
  selector: 'sm-training-metrics',
  standalone: true,
  imports: [
    CommonModule,
    ChartModule,
    TooltipModule,
    TagModule,
    PageLoadComponent,
    DropdownModule,
    FormsModule,
    CardModule
  ],
  templateUrl: './training-metrics.component.html'
})
export class TrainingMetricsComponent implements OnDestroy {
  rayJob = input.required<IJob>();
  @Output() error = new EventEmitter<any>();
  @Output() metricsTimeout = new EventEmitter<any>();

  metrics: TrainingChartData;
  lineOptions: any;
  loading = false;

  private subscription?: Subscription;
  private destroy$ = new Subject<void>();

  constructor(private metricService: MetricService,
              private messageService: MessageService,
              private layoutService: LayoutService) {
    this.initStyle();
    this.layoutService.configUpdate$.pipe(
      takeUntil(this.destroy$)
    ).subscribe(_ => this.initStyle());


    effect(async () => {
      if (this.rayJob()) {
        await this.initMetrics();
      }
    });
  }

  async initMetrics() {
    if (this.subscription) {
      this.subscription.unsubscribe();
      await lastValueFrom(this.metricService.stopRayJobMetrics([this.rayJob().id]));
    }

    this.subscription = this.metricService.connectToRayJobMetrics([this.rayJob().id])
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (event: SseEvent<IMetric>) => {
          if (event.type === SseEventTypeTimeout) {
            this.metricsTimeout.emit();
            return
          } else if (event.type === 'metrics') {
            this.metrics = new TrainingChartData();
            this.metrics.addDataPoints(event.data);
          }
        },
        error: this.handleError.bind(this)
      });
  }

  public async refreshConnection() {
    await this.initMetrics();
  }

  private handleError(error: any): void {
    this.messageService.add({
      severity: 'error',
      summary: 'Error',
      detail: error.message || 'An error occurred'
    });
  }

  initStyle() {
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
          }
        },
        y: {
          ticks: {
            color: textColorSecondary
          },
          grid: {
            color: surfaceBorder,
            drawBorder: false
          }
        },
      }
    };
  }

  async ngOnDestroy() {
    if (this.subscription) {
      this.subscription.unsubscribe();
    }

    this.destroy$.next();
    this.destroy$.complete();

    await lastValueFrom(this.metricService.stopRayJobMetrics([this.rayJob().id]));
  }
}
