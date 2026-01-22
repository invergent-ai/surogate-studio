import { Component, computed, effect, EventEmitter, input, OnDestroy, Output, Signal } from '@angular/core';
import { MessageService, SelectItemGroup } from 'primeng/api';
import { MetricService } from '../../../../shared/service/k8s/metric.service';
import { takeUntil } from 'rxjs/operators';
import { lastValueFrom, Subject, Subscription } from 'rxjs';
import { CommonModule } from '@angular/common';
import { IApplication } from '../../../../shared/model/application.model';
import { ChartData } from '../../../../shared/model/k8s/container-metrics.model';
import { ChartModule } from 'primeng/chart';
import { TooltipModule } from 'primeng/tooltip';
import { TagModule } from 'primeng/tag';
import { PageLoadComponent } from '../../../../shared/components/page-load/page-load.component';
import { LayoutService } from '../../../../shared/service/theme/app-layout.service';
import { AppStatusWithResources, appStatusWithContainers } from '../../../../shared/model/k8s/app-status.model';
import { DropdownModule } from 'primeng/dropdown';
import { FormsModule } from '@angular/forms';
import { IMetric } from '../../../../shared/model/k8s/metric.model';
import { SseEvent, SseEventTypeTimeout } from '../../../../shared/model/k8s/event.model';
import { SelectItem } from 'primeng/api/selectitem';
import { Store } from '@ngxs/store';
import { Selectors } from '../../../../shared/state/selectors';
import { CardModule } from 'primeng/card';

@Component({
  selector: 'sm-metrics',
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
  templateUrl: './metrics.component.html'
})
export class MetricsComponent implements OnDestroy {
  application = input.required<IApplication>();

  @Output() error = new EventEmitter<any>();
  @Output() metricsTimeout = new EventEmitter<any>();

  status: Signal<AppStatusWithResources>;
  containers: Signal<SelectItem<string>[]>;

  selectedContainer: string;
  metrics: ChartData;
  lineOptions: any;
  loading = false;

  private subscription?: Subscription;
  private destroy$ = new Subject<void>();

  constructor(private metricService: MetricService,
              private messageService: MessageService,
              private layoutService: LayoutService,
              private store: Store) {
    this.initStyle();
    this.layoutService.configUpdate$.pipe(
      takeUntil(this.destroy$)
    ).subscribe(_ => this.initStyle());

    this.status = computed(() => appStatusWithContainers(
      this.application(),
      this.store.selectSignal(Selectors.resourceStatus(this.application().id))()
    ));
    this.containers = computed(() => this.status().resourceStatus.flatMap((rs, idx) => {
      return rs.containerStatuses.map(cs => {
        return {
          label: `Replica ${idx}: ${cs.containerName}`,
          value: `${idx}/${rs.podName}/${cs.containerId}`
        } as SelectItem<string>;
      });
    }));

    effect(async () => {
      if (this.containers().length && !this.selectedContainer) {
        await this.initMetrics(this.containers()[0].value);
      }
    });
  }

  async initMetrics(selected: string) {
    if (this.selectedContainer && this.subscription) {
      const [replica, podName, containerId] = this.selectedContainer.split('/');
      this.subscription.unsubscribe();
      await lastValueFrom(this.metricService.stopMetrics(this.application().id, podName, containerId));
    }

    this.selectedContainer = selected;
    const [replica, podName, containerId] = selected.split('/');

    this.subscription = this.metricService.connectToMetrics(this.application().id, podName, containerId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (event: SseEvent<IMetric>) => {
          if (event.type === SseEventTypeTimeout) {
            this.metricsTimeout.emit();
            return
          } else if (event.type === 'metrics') {
            this.metrics = new ChartData();
            this.metrics.addDataPoints(event.data);
          }
        },
        error: this.handleError.bind(this)
      });
  }

  public async refreshConnection() {
    await this.initMetrics(this.selectedContainer);
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

    this.status().resourceStatus.forEach(rs =>
      rs.containerStatuses.forEach(async cs => {
        await lastValueFrom(this.metricService.stopMetrics(this.application().id, rs.podName, cs.containerId));
      }
    ));
  }

  protected readonly menubar = menubar;
}
