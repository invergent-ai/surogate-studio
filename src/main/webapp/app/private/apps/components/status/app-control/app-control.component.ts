import { Component, computed, effect, Input, input, signal, Signal } from '@angular/core';
import { IApplication } from '../../../../../shared/model/application.model';
import SharedModule from '../../../../../shared/shared.module';
import { TooltipModule } from 'primeng/tooltip';
import { ControlService } from '../../../../../shared/service/k8s/control.service';
import { MessageService } from 'primeng/api';
import { ConfirmPopupModule } from 'primeng/confirmpopup';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { ReactiveFormsModule } from '@angular/forms';
import { ApplicationStatus, ResourceStatusStage } from '../../../../../shared/model/enum/application-status.model';
import { ApplicationMode } from '../../../../../shared/model/enum/application-mode.model';
import { appStatusWithContainers, AppStatusWithResources } from '../../../../../shared/model/k8s/app-status.model';
import { InputGroupModule } from 'primeng/inputgroup';
import { InputGroupAddonModule } from 'primeng/inputgroupaddon';
import { finalize } from 'rxjs/operators';
import { toObservable } from '@angular/core/rxjs-interop';
import { Selectors } from '../../../../../shared/state/selectors';
import { Store } from '@ngxs/store';

@Component({
  standalone: true,
  selector: 'sm-app-control',
  templateUrl: './app-control.component.html',
  imports: [
    SharedModule,
    TooltipModule,
    ConfirmPopupModule,
    OverlayPanelModule,
    ReactiveFormsModule,
    InputGroupModule,
    InputGroupAddonModule
  ]
})
export class AppControlComponent {
  application = input.required<IApplication>();
  @Input()
  component: string;

  replicas: number = 1;
  loadingStart = signal(false);
  loadingStop = signal(false);
  loadingRestart = signal(false);
  loadingScale = signal(false);

  loading: Signal<boolean>;
  startDisabled: Signal<boolean>;
  stopDisabled: Signal<boolean>;
  disabled: Signal<boolean>;
  status: Signal<AppStatusWithResources>;

  constructor(private controlService: ControlService,
              private messageService: MessageService,
              private store: Store
  ) {
    this.status = computed(() => {
      const appId = this.application()?.mode === ApplicationMode.MODEL ?
        `${this.application()?.id}-${this.component}`:
        this.application()?.id;
      const appStatus = this.store.selectSignal(Selectors.resourceStatus(appId))();
      return appStatusWithContainers(this.application(), appStatus);
    });

    this.loading = computed(() => {
      if (!this.status()) {
        return true;
      }
      const resourcesPending = this.status().resourceStatus
        .map(rs => rs.stage === ResourceStatusStage.INITIALIZING)
        .reduce((r1, r2) => r1 || r2, false) || false;
      return this.loadingStart() || this.loadingStop() || this.loadingRestart() || this.loadingScale() || resourcesPending;
    });

    // Active stages represent resources that are present / in-flight so starting should be disabled in these cases
    const activeStages: ResourceStatusStage[] = [
      ResourceStatusStage.RUNNING,
      ResourceStatusStage.INITIALIZING,
      ResourceStatusStage.WAITING,
      ResourceStatusStage.RESTARTING,
      ResourceStatusStage.DEGRADED
    ];

    this.startDisabled = computed(() => {
      const s = this.status();
      if (!s) {
        return true;
      }
      // Only allow start when application is in a stable state that can transition to running
      if (s.status !== ApplicationStatus.DEPLOYED && s.status !== ApplicationStatus.INITIALIZED) {
        return true; // e.g. DEPLOYING / BUILDING / CREATED / DELETING / ERROR
      }
      // Disable start if ANY active resource stage exists (not just RUNNING) - means it's already (re)starting
      const anyActive = s.resourceStatus.some(rs => activeStages.includes(rs.stage));
      return anyActive;
    });

    this.stopDisabled = computed(() => {
      const s = this.status();
      if (!s) {
        return true;
      }
      // Allow stopping while DEPLOYING as long as there are active resources (e.g. RESTARTING)
      if (s.status !== ApplicationStatus.DEPLOYED && s.status !== ApplicationStatus.INITIALIZED && s.status !== ApplicationStatus.DEPLOYING) {
        return true; // other states shouldn't allow stop (BUILDING/CREATED/DELETING/ERROR)
      }
      // Enable stop if there is at least one active resource (includes RUNNING / INITIALIZING / WAITING / RESTARTING / DEGRADED)
      const anyActive = s.resourceStatus.some(rs => activeStages.includes(rs.stage));
      return !anyActive; // disabled only when no active resources to stop
    });

    this.disabled = computed(() => !this.status() || (this.status().status !== ApplicationStatus.DEPLOYED && this.status().status !== ApplicationStatus.INITIALIZED));

    toObservable(this.application).subscribe(app => {
      this.replicas = app.replicas || 1;
    });
  }

  async startApp() {
    this.loadingStart.set(true);
    this.controlService.startApplication(this.application()?.id, this.component)
      .pipe(finalize(() => this.loadingStart.set(false)))
      .subscribe({
        error: this.handleError.bind(this)
      });
  }

  async stopApp() {
    this.loadingStop.set(true);
    this.controlService.stopApplication(this.application()?.id, this.component)
      .pipe(finalize(() => this.loadingStop.set(false)))
      .subscribe({
        error: this.handleError.bind(this)
      });
  }

  async restartApp() {
    this.loadingRestart.set(true);
    this.controlService.restartApplication(this.application()?.id, this.component)
      .pipe(finalize(() => this.loadingRestart.set(false)))
      .subscribe({
        error: this.handleError.bind(this)
      });
  }

  async scaleApp() {
    this.loadingScale.set(true);
    this.controlService.scaleApplication(this.application()?.id, this.replicas)
      .pipe(finalize(() => this.loadingScale.set(false)))
      .subscribe({
        error: this.handleError.bind(this)
      });
  }

  private handleError(error: any): void {
    this.messageService.add({
      severity: 'error',
      summary: 'Error',
      detail: error?.message || 'An error occurred'
    });
  }

  protected readonly ApplicationMode = ApplicationMode;
}
