import {
  Component,
  computed,
  EventEmitter,
  input,
  Input,
  OnDestroy,
  OnInit,
  Output,
  Signal,
  ViewChild
} from '@angular/core';
import { IApplication } from '../../../../shared/model/application.model';
import SharedModule from '../../../../shared/shared.module';
import { VirtualScrollerModule } from 'primeng/virtualscroller';
import { LogsComponent } from './logs/logs.component';
import { TerminalComponent } from './terminal/terminal.component';
import { TooltipModule } from 'primeng/tooltip';
import { DropdownModule } from 'primeng/dropdown';
import { NgIf } from '@angular/common';
import { TagModule } from 'primeng/tag';
import {
  ApplicationStatus,
  ResourceStatusStage,
  ContainerStatusStage
} from '../../../../shared/model/enum/application-status.model';
import { MessageService } from 'primeng/api';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { AppStatusWithResources, appStatusWithContainers, ContainerStatus } from '../../../../shared/model/k8s/app-status.model';
import { containerName } from '../../../../shared/util/naming.util';
import { BehaviorSubject, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ciCdError } from '../../../../shared/util/error.util';
import { FileComponent } from './file/file.component';
import { ApplicationMode } from '../../../../shared/model/enum/application-mode.model';
import { SkeletonModule } from 'primeng/skeleton';
import { Info, LucideAngularModule, Server } from 'lucide-angular';
import { PageLoadComponent } from '../../../../shared/components/page-load/page-load.component';
import { Store } from '@ngxs/store';
import { AppState } from '../../../../shared/state/app-state';
import { Selectors } from '../../../../shared/state/selectors';
import { CardModule } from 'primeng/card';

@Component({
  standalone: true,
  selector: 'sm-app-status',
  templateUrl: './app-status.component.html',
  imports: [
    SharedModule,
    VirtualScrollerModule,
    LogsComponent,
    TerminalComponent,
    TooltipModule,
    DropdownModule,
    NgIf,
    TagModule,
    OverlayPanelModule,
    FileComponent,
    SkeletonModule,
    LucideAngularModule,
    PageLoadComponent,
    CardModule
  ]
})
export class AppStatusComponent implements OnInit, OnDestroy {
  application = input.required<IApplication>();
  @Input()
  component: string;

  @Input() set statusReset(statusReset: BehaviorSubject<boolean>) {
    statusReset.asObservable()
      .pipe(takeUntil(this.destroy$))
      .subscribe(reset => {
        if (reset) {
          this.showShell = {};
          this.showUpload = {};
        }
      });
  }

  @Output() logsTimeout = new EventEmitter<any>();
  @Output() logsDisconnect = new EventEmitter<any>();
  @Output() terminalTimeout = new EventEmitter<any>();
  @Output() terminalDisconnect = new EventEmitter<any>();
  @ViewChild('logs') logs: LogsComponent;
  @ViewChild('terminal') terminal: TerminalComponent;

  destroy$ = new Subject<void>();
  showLogs: Record<string, boolean> = {};
  showShell: Record<string, boolean> = {};
  showUpload: Record<string, boolean> = {};

  status: Signal<AppStatusWithResources>;

  constructor(private messageService: MessageService, private store: Store) {
    this.status = computed(() => {
      const appId = this.application()?.mode === ApplicationMode.MODEL ?
        `${this.application()?.id}-${this.component}`:
        this.application()?.id;
      const appStatus = this.store.selectSignal(Selectors.resourceStatus(appId))();
      return appStatusWithContainers(this.application(), appStatus);
    });
  }

  ngOnInit() {
    this.showLogs = {};
  }

  toggleLogs(podName: string, cs: ContainerStatus) {
    if (!this.showLogs[podName+cs.containerId] && !this.validateContainerStage(cs.stage)) {
      return;
    }
    this.showLogs[podName+cs.containerId] = !this.showLogs[podName+cs.containerId];
  }

  toggleTerminal(podName: string, cs: ContainerStatus) {
    if (!this.showShell[podName+cs.containerId] && !this.validateContainerStage(cs.stage)) {
      return;
    }
    this.showShell[podName+cs.containerId] = !this.showShell[podName+cs.containerId];
  }

  toggleUpload(podName: string, cs: ContainerStatus) {
    if (!this.showUpload[podName+cs.containerId] && !this.validateContainerStage(cs.stage)) {
      return;
    }
    this.showUpload[podName+cs.containerId] = !this.showUpload[podName+cs.containerId];
  }

  private validateContainerStage(stage: ContainerStatusStage): boolean {
    if (stage === ContainerStatusStage.NOT_DEPLOYED) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Container not deployed',
        detail: 'This container is not deployed'
      });
      return false;
    } else if (stage !== ContainerStatusStage.RUNNING) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Container not running',
        detail: 'This container is not running'
      });
      return false;
    }

    return true;
  }

  handleLogsTimeout() {
    this.logsTimeout.emit();
  }

  handleLogsDisconnect(error: any) {
    this.logsDisconnect.emit(error);
  }

  handleTerminalTimeout() {
    this.terminalTimeout.emit();
  }

  handleTerminalDisconnect(error: any) {
    this.terminalDisconnect.emit(error);
  }

  public refreshConnection() {
    if (this.logs) {
      this.logs.refreshConnection();
    }
    if (this.terminal) {
      this.terminal.refreshConnection();
    }
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  readonly AppStatusStage = ResourceStatusStage;
  readonly ContainerStatusStage = ContainerStatusStage;
  readonly ApplicationStatus = ApplicationStatus;
  protected readonly ApplicationMode = ApplicationMode;
  protected readonly Server = Server;
  protected readonly Info = Info;
  protected readonly containerName = containerName;
}
