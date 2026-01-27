import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { debounceTime, distinctUntilChanged, lastValueFrom, Subject, Subscription } from 'rxjs';
import { ciCdError, displayError } from '../../shared/util/error.util';
import { IApplication } from '../../shared/model/application.model';
import { Message } from 'primeng/api';
import { PageComponent } from '../../shared/components/page/page.component';
import { PageLoadComponent } from '../../shared/components/page-load/page-load.component';
import { NgIf } from '@angular/common';
import { TabViewModule } from 'primeng/tabview';
import { AppStatusComponent } from './components/status/app-status.component';
import { AppConfigComponent } from './components/config/app-config.component';
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
import { ResourceStatus } from '../../shared/model/k8s/app-status.model';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { Selectors } from '../../shared/state/selectors';
import { InputNumberModule } from 'primeng/inputnumber';
import { RippleModule } from 'primeng/ripple';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { Account } from '../../shared/model/account.model';
import { UserService } from '../../shared/service/user.service';
import { IProject } from '../../shared/model/project.model';
import { DropdownModule } from 'primeng/dropdown';
import { CardModule } from 'primeng/card';
import { ToolbarModule } from 'primeng/toolbar';
import { AppControlComponent } from './components/status/app-control/app-control.component';
import { MessagesModule } from 'primeng/messages';
import { displayWarning } from '../../shared/util/success.util';
import { ApplicationMode } from '../../shared/model/enum/application-mode.model';
import { AppStatusAction, ModelStatusAction } from '../../shared/state/actions';
import { SseEvent, SseEventTypeTimeout } from '../../shared/model/k8s/event.model';
import { RouterMetricsComponent } from './components/router-metrics/router-metrics.component';
import { ModelStatusWithResources } from '../../shared/model/k8s/model-status.model';
import {
  MODEL_COMPONENT_CACHE,
  MODEL_COMPONENT_ROUTER,
  MODEL_COMPONENT_WORKER
} from '../../config/constant/model.constants';
import { WorkerMetricsComponent } from './components/worker-metrics/worker-metrics.component';
import { serviceNameForPort } from '../../shared/util/naming.util';
import { AppHeaderCardComponent } from './components/app-header-card.component';
import { Activity, LucideAngularModule } from 'lucide-angular';
import { LayoutService } from '../../shared/service/theme/app-layout.service';
import {ChatVllmComponent} from "../training/components/chat-vllm/chat-vllm.component";

@Component({
  standalone: true,
  templateUrl: './model.page.html',
  imports: [
    PageComponent,
    PageLoadComponent,
    NgIf,
    TabViewModule,
    AppStatusComponent,
    AppConfigComponent,
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
    AppControlComponent,
    MessagesModule,
    ReactiveFormsModule,
    RouterMetricsComponent,
    WorkerMetricsComponent,
    AppHeaderCardComponent,
    LucideAngularModule,
    ChatVllmComponent
  ]
})
export class ModelPage implements OnInit, OnDestroy {
  @ViewChild('status') statusComponent: AppStatusComponent;

  readonly welcomeItems = [
    { title: 'What is Surogate ?', link: 'https://surogate.ai/' },
    { title: 'Applications in Surogate', link: 'https://docs.statemesh.net/applications/intro' }
  ];

  private statusSubscription?: Subscription;
  private appsSubscription?: Subscription;
  private destroy$ = new Subject<void>();

  apps: IApplication[];
  application: IApplication;
  activeTab = -1;

  user?: Account;
  projects: IProject[] = [];

  reinitTimeout: any;
  ingressHostName: string;

  message: string = '';
  details: string[] = [];

  loading = true;
  isPublishing = false;
  timeoutOccurred = false;

  modelStatus: ModelStatusWithResources;
  publishWarn: Message[] = [{severity: 'warn', detail: 'Publish your model to get started.'}];

  constructor(
    private route: ActivatedRoute,
    private statusService: StatusService,
    private userService: UserService,
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

      await this.initUserDetails();
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
        if (currentApp[0]?.message) {
          this.message = currentApp[0].message;
        }

        if ((existingAppId !== applicationId || reload) && this.application?.status !== ApplicationStatus.CREATED) {
          this.initStatus();
        } else if (this.application?.status === ApplicationStatus.CREATED) {
          this.modelStatus = {
            status: ApplicationStatus.CREATED,
            applicationId,
            router: {
              status: ApplicationStatus.CREATED,
              applicationId,
              resourceStatus: []
            },
            worker: {
              status: ApplicationStatus.CREATED,
              applicationId,
              resourceStatus: []
            },
            cache: {
              status: ApplicationStatus.CREATED,
              applicationId,
              resourceStatus: []
            },
          };
          this.store.dispatch(new ModelStatusAction(this.modelStatus));
        }

        const shouldShowConfigTab =
          (!this.modelStatus || this.modelStatus?.worker?.resourceStatus.length == 0) ||
          (this.application?.status !== ApplicationStatus.DEPLOYED);

        this.activeTab = shouldShowConfigTab ? 0 : Math.max(0, this.activeTab);
        this.loading = false;
      }
    }
  }

  initStatus() {
    if (this.statusSubscription) {
      return;
    }

    this.statusSubscription = this.statusService.connectToModelStatus(this.application?.id)
      .pipe(
        distinctUntilChanged(),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (event: SseEvent<ModelStatusWithResources>) => {
          if (event.type === SseEventTypeTimeout) {
            this.handleTimeout();
            return;
          } else if (event.type === 'modelstatus') {
            this.updateStatus(event.data);
          }
        },
        error: (_) => this.handleError.bind(this)
      });
  }

  private updateStatus(status: ModelStatusWithResources) {
    if (this.application) {
      this.modelStatus = status;
      this.store.dispatch(new AppStatusAction({applicationId: status.applicationId, status: status.status, resourceStatus: []}));
      this.store.dispatch(new ModelStatusAction(status));
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

  public handleModelMetricsError(error: any): void {
    displayError(this.store, error.message || 'Router metrics error occurred');
  }

  async refreshConnection() {
    this.timeoutOccurred = false;
    if (this.statusComponent) {
      this.statusComponent.refreshConnection();
    }
    // if (this.metricsComponent) {
    //   await this.metricsComponent.refreshConnection();
    // }
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

    this.modelStatus = null;
    this.message = '';
    this.details = [];

    this.destroy$.next();
    this.destroy$.complete();
    if (this.reinitTimeout) {
      clearTimeout(this.reinitTimeout);
    }

    if (this.application?.id) {
      await lastValueFrom(this.statusService.stopModelStatus(this.application));
    }
  }

  async publishing(publishing: boolean) {
    this.isPublishing = publishing;
  }

  private async initUserDetails() {
    this.user = await this.userService.getUserDetails();
  }

  replicaStatusMessage(resourceStatus: ResourceStatus, replica: number): string {
    let msg = 'Replica: ' + replica;
    if (resourceStatus.component) {
      msg += ' (' + resourceStatus.component + ')';
    }
    if (resourceStatus.message) {
      msg += ': ' + resourceStatus.message;
    }
    if (resourceStatus.details && resourceStatus.details.length) {
      msg += ': ' + resourceStatus.details.join(', ');
    }
    return msg;
  }

  statusTabDisabled(component: string): boolean {
    switch (component) {
      case MODEL_COMPONENT_ROUTER: {
        const hasAppStatus = this.modelStatus?.router && this.modelStatus.router.resourceStatus?.length > 0;
        const isDeployed = hasAppStatus ? this.modelStatus?.router?.status === ApplicationStatus.DEPLOYED : false;
        const isDeploying = hasAppStatus ? this.modelStatus?.router?.status === ApplicationStatus.DEPLOYING : false;
        const isBuilding = hasAppStatus ? this.modelStatus?.router?.status === ApplicationStatus.BUILDING : false;
        return !hasAppStatus || isDeploying || isBuilding || !isDeployed || ciCdError(this.application);
      }
      case MODEL_COMPONENT_WORKER: {
        const hasAppStatus = this.modelStatus?.worker && this.modelStatus.worker.resourceStatus?.length > 0;
        const isDeployed = hasAppStatus ? this.modelStatus.worker.status === ApplicationStatus.DEPLOYED : false;
        const isDeploying = hasAppStatus ? this.modelStatus?.worker?.status === ApplicationStatus.DEPLOYING : false;
        const isBuilding = hasAppStatus ? this.modelStatus?.worker?.status === ApplicationStatus.BUILDING : false;
        return !hasAppStatus || isDeploying || isBuilding || !isDeployed || ciCdError(this.application);
      }
      case MODEL_COMPONENT_CACHE: {
        const hasAppStatus = this.modelStatus?.cache && this.modelStatus.cache.resourceStatus?.length > 0;
        const isDeployed = hasAppStatus ? this.modelStatus?.cache?.status === ApplicationStatus.DEPLOYED : false;
        const isDeploying = hasAppStatus ? this.modelStatus?.cache?.status === ApplicationStatus.DEPLOYING : false;
        const isBuilding = hasAppStatus ? this.modelStatus?.cache?.status === ApplicationStatus.BUILDING : false;
        return !hasAppStatus || isDeploying || isBuilding || !isDeployed || ciCdError(this.application);
      }
    }
    return false;
  }

  modelStatusCreated(): boolean {
    return this.modelStatus?.status === ApplicationStatus.CREATED;
  }

  modelStatusDeployingOrBuilding(): boolean {
    return this.modelStatus?.status === ApplicationStatus.DEPLOYING
      || this.modelStatus?.status === ApplicationStatus.BUILDING;
  }

  internalRouterEndpoint(): string | null{
    if (!this.application) {
      return null;
    }

    const routerMainServicePort = this.application.containers.find(c => c.displayName === MODEL_COMPONENT_ROUTER)
      .ports.find(p => p.servicePort === 80);
    return serviceNameForPort(this.application.internalName, routerMainServicePort.name, this.application.deployedNamespace);
  }

  async ngOnDestroy() {
    await this.destroyStatus();
    await this.destroyAppSubscription();
  }

  public readonly ApplicationStatus = ApplicationStatus;
  protected readonly ciCdError = ciCdError;
  protected readonly ApplicationMode = ApplicationMode;
  protected readonly ResourceStatusStage = ResourceStatusStage;
  protected readonly Activity = Activity;
}





















