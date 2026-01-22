import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { debounceTime, distinctUntilChanged, lastValueFrom, Subject, Subscription } from 'rxjs';
import { ciCdError, displayError } from '../../shared/util/error.util';
import { IApplication } from '../../shared/model/application.model';
import { PageComponent } from '../../shared/components/page/page.component';
import { PageLoadComponent } from '../../shared/components/page-load/page-load.component';
import { NgIf } from '@angular/common';
import { TabViewModule } from 'primeng/tabview';
import { AppStatusComponent } from './components/status/app-status.component';
import { AppConfigComponent } from './components/config/app-config.component';
import { MetricsComponent } from './components/metrics/metrics.component';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { ChipsModule } from 'primeng/chips';
import { TagModule } from 'primeng/tag';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { Store } from '@ngxs/store';
import { ConfirmPopupModule } from 'primeng/confirmpopup';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TooltipModule } from 'primeng/tooltip';
import { takeUntil } from 'rxjs/operators';
import { StatusService } from '../../shared/service/k8s/status.service';
import { ApplicationStatus, ResourceStatusStage } from '../../shared/model/enum/application-status.model';
import { AppStatusWithResources } from '../../shared/model/k8s/app-status.model';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { Selectors } from '../../shared/state/selectors';
import { InputNumberModule } from 'primeng/inputnumber';
import { RippleModule } from 'primeng/ripple';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { IProject } from '../../shared/model/project.model';
import { DropdownModule } from 'primeng/dropdown';
import { CardModule } from 'primeng/card';
import { ToolbarModule } from 'primeng/toolbar';
import { MessagesModule } from 'primeng/messages';
import { displayWarning } from '../../shared/util/success.util';
import { ApplicationMode } from '../../shared/model/enum/application-mode.model';
import { AppStatusAction } from '../../shared/state/actions';
import { SseEvent, SseEventTypeTimeout } from '../../shared/model/k8s/event.model';
import { AppHeaderCardComponent } from './components/app-header-card.component';
import { MessageModule } from 'primeng/message';
import { Message } from 'primeng/api';
import { LayoutService } from '../../shared/service/theme/app-layout.service';

@Component({
  standalone: true,
  templateUrl: './app.page.html',
  styleUrls: ['./app.page.scss'],
  imports: [
    PageComponent,
    PageLoadComponent,
    NgIf,
    TabViewModule,
    AppStatusComponent,
    AppConfigComponent,
    MetricsComponent,
    FormsModule,
    ChipsModule,
    TagModule,
    ConfirmDialogModule,
    ConfirmPopupModule,
    ProgressSpinnerModule,
    TooltipModule,
    OverlayPanelModule,
    InputNumberModule,
    RippleModule,
    InputTextareaModule,
    DropdownModule,
    CardModule,
    ToolbarModule,
    MessagesModule,
    ReactiveFormsModule,
    AppHeaderCardComponent,
    AppHeaderCardComponent,
    MessageModule
  ]
})
export class AppPage implements OnInit, OnDestroy {
  @ViewChild('status') statusComponent: AppStatusComponent;
  @ViewChild('metrics') metricsComponent: MetricsComponent;

  readonly welcomeItems = [
    { title: 'What is Surogate ?', link: 'https://surogate.ai/' },
    { title: 'Applications in Surogate Studio', link: 'https://docs.statemesh.net/applications/intro' },
    { title: 'Configure your Application', link: 'https://docs.statemesh.net/applications/configure' },
    { title: 'Manage your Application', link: 'https://docs.statemesh.net/applications/manage' },
    { title: 'Storage Providers', link: 'https://docs.statemesh.net/applications/storage-providers' }
  ];

  private statusSubscription?: Subscription;
  private appsSubscription?: Subscription;
  private destroy$ = new Subject<void>();

  apps: IApplication[];
  application: IApplication;
  activeTab = -1;

  projects: IProject[] = [];

  reinitTimeout: any;
  ingressHostName: string;

  loading = true;
  isPublishing = false;
  timeoutOccurred = false;

  appStatus: AppStatusWithResources;

  publishWarn: Message[] = [{severity: 'warn', detail: 'Publish your application to get started.'}];
  messages: Message[] = [
    { severity: 'info', detail: 'Info Message' },
    { severity: 'success', detail: 'Success Message' },
    { severity: 'warn', detail: 'Warning Message' },
    { severity: 'error', detail: 'Error Message' },
    { severity: 'secondary', detail: 'Secondary Message' },
    { severity: 'contrast', detail: 'Contrast Message' }
  ];

  constructor(
    private route: ActivatedRoute,
    private statusService: StatusService,
    private store: Store,
    private layoutService: LayoutService,
  ) {
  }

  async ngOnInit() {
    this.layoutService.state.helpItems = this.welcomeItems;
    this.route.queryParams.subscribe(async params => {
      this.loading = true;
      this.activeTab = 0;
      this.store.select(Selectors.projects).subscribe(projects => {
        this.projects = projects;
      });

      await this.init(params['id'], params['reload']);
    });
  }

  async init(applicationId: string, reload: number) {
    if (this.appsSubscription) {
      await this.destroyAppSubscription();
    }

    try {
      this.appsSubscription = this.store.select(Selectors.apps)
        .pipe(
          debounceTime(150),
          distinctUntilChanged()
        )
        .subscribe({
          next: (apps) => {
            if (this.isPublishing) {
              return;
            }
            this.apps = apps;
            this.initApplication(apps, applicationId, reload);
          },
          error: () => {
          }
        });
    } catch (e) {
      displayError(this.store, e);
    }
  }

  async initApplication(apps: IApplication[], applicationId: string, reload: number) {
    if (apps && apps.length) {
      const currentApp = apps.filter(app => app.id === applicationId);
      if (currentApp.length && currentApp[0]) {
        const existingAppId = this.application?.id;
        if (existingAppId && existingAppId !== applicationId) {
          await this.destroyStatus();
          if (currentApp[0].status !== ApplicationStatus.CREATED) {
            await this.destroyAppSubscription();
          }
        }

        if (existingAppId !== applicationId || this.application?.status !== currentApp[0].status) {
          this.application = JSON.parse(JSON.stringify(currentApp[0]));
        }

        this.ingressHostName = currentApp[0].ingressHostName;

        if ((existingAppId !== applicationId || reload) && this.application?.status !== ApplicationStatus.CREATED) {
          this.initStatus();
        } else if (this.application?.status === ApplicationStatus.CREATED) {
          this.appStatus = {
            status: ApplicationStatus.CREATED,
            applicationId,
            resourceStatus: []
          };
          this.store.dispatch(new AppStatusAction(this.appStatus));
        }

        const shouldShowConfigTab =
          (!this.appStatus || this.appStatus?.resourceStatus.length == 0) ||
          (this.application?.status !== ApplicationStatus.DEPLOYED
            && this.application?.status !== ApplicationStatus.BUILDING
            && !ciCdError(this.application));

        this.activeTab = shouldShowConfigTab ? 0 : Math.max(0, this.activeTab);
        this.loading = false;
      }
    }
  }

  initStatus() {
    if (this.statusSubscription) {
      return;
    }

    this.statusSubscription = this.statusService.connectToAppStatus(this.application?.id)
      .pipe(
        distinctUntilChanged(),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (event: SseEvent<AppStatusWithResources>) => {
          if (event.type === SseEventTypeTimeout) {
            this.handleTimeout();
            return;
          } else if (event.type === 'status') {
            this.updateStatus(event.data);
          }
        },
        error: (_) => this.handleError.bind(this)
      });
  }

  private updateStatus(status: AppStatusWithResources) {
    if (this.application) {
      this.appStatus = status;
      this.store.dispatch(new AppStatusAction(status));
    }
  }

  public handleTimeout(): void {
    if (!this.timeoutOccurred) {
      this.timeoutOccurred = true;
      displayWarning(this.store, 'Connection terminated due to inactivity. Click the warning icon to reconnect.');
    }
  }

  public handleDisconnect(error: string): void {
    // Only show notification if the icon isn't already visible
    if (!this.timeoutOccurred) {
      this.timeoutOccurred = true;
      displayWarning(this.store, error || 'Connection interrupted. Click the warning icon to reconnect.');
    }
  }

  private handleError(error: any): void {
    displayError(this.store, error.message || 'An error occurred');
  }

  async refreshConnection() {
    this.timeoutOccurred = false;
    if (this.statusComponent) {
      this.statusComponent.refreshConnection();
    }
    if (this.metricsComponent) {
      await this.metricsComponent.refreshConnection();
    }
  }

  async destroyAppSubscription() {
    if (this.appsSubscription) {
      this.appsSubscription.unsubscribe();
      this.appsSubscription = null;
    }
  }

  async destroyStatus() {
    if (this.statusSubscription) {
      this.statusSubscription.unsubscribe();
      this.statusSubscription = null;
    }

    this.appStatus = null;

    this.destroy$.next();
    this.destroy$.complete();
    if (this.reinitTimeout) {
      clearTimeout(this.reinitTimeout);
    }

    if (this.application?.id) {
      await lastValueFrom(this.statusService.stopAppStatus(this.application));
    }
  }

  async publishing(publishing: boolean) {
    this.isPublishing = publishing;
  }

  statusTabDisabled(): boolean {
    const hasAppStatus = this.appStatus && this.appStatus.resourceStatus.length > 0;
    const isDeployed = this.appStatus?.status === ApplicationStatus.DEPLOYED;
    const isDeploying = this.appStatus?.status === ApplicationStatus.DEPLOYING;
    const isBuilding = this.appStatus?.status === ApplicationStatus.BUILDING;
    return !hasAppStatus || isDeploying || isBuilding || !isDeployed || ciCdError(this.application);
  }

  async ngOnDestroy() {
    await this.destroyStatus();
    await this.destroyAppSubscription();
  }

  public readonly ApplicationStatus = ApplicationStatus;
  protected readonly ciCdError = ciCdError;
  protected readonly ApplicationMode = ApplicationMode;
  protected readonly ResourceStatusStage = ResourceStatusStage;
}
