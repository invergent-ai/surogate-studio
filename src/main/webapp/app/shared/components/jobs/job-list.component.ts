import {
  AfterContentInit,
  Component,
  computed,
  ContentChildren,
  effect,
  inject,
  input,
  OnDestroy,
  QueryList,
  signal,
  TemplateRef,
  viewChild
} from '@angular/core';
import {ConfirmPopupModule} from 'primeng/confirmpopup';
import {ConfirmationService, PrimeTemplate} from 'primeng/api';
import {TableModule} from 'primeng/table';
import {distinctUntilChanged, lastValueFrom, Observable, of, Subject, Subscription} from 'rxjs';
import {catchError, map, takeUntil, tap} from 'rxjs/operators';
import {SseEvent, SseEventTypeTimeout} from '../../model/k8s/event.model';
import {displayError, displayErrorAndRethrow} from '../../util/error.util';
import {StatusService} from '../../service/k8s/status.service';
import {Store} from '@ngxs/store';
import {TaskRunService} from '../../service/task-run.service';
import {derivedAsync} from 'ngxtension/derived-async';
import {LogsComponent} from '../../../private/apps/components/status/logs/logs.component';
import {NgClass, NgIf, NgTemplateOutlet} from '@angular/common';
import {DialogModule} from 'primeng/dialog';
import {Copy, Download, LucideAngularModule, MessageSquare, Pencil, Plus, Trash} from 'lucide-angular';
import {OverlayPanel, OverlayPanelModule} from 'primeng/overlaypanel';
import {ButtonDirective} from 'primeng/button';
import {IJob, IJobRunStatus, IJobStatus, IJobType, JobTypeLabels} from "../../model/job.model";
import {DEFAULT_QUERY} from "../../state/app-state";
import {ExecutorType} from "../../model/enum/executor-type.model";
import {RayJobService} from "../../service/ray-job.service";
import {CardComponent} from "../card/card.component";
import {DropdownModule} from "primeng/dropdown";
import {InputTextModule} from "primeng/inputtext";
import {FormsModule} from "@angular/forms";
import {Router} from "@angular/router";
import {TooltipModule} from "primeng/tooltip";

@Component({
  selector: 'sm-job-list',
  standalone: true,
  imports: [
    ConfirmPopupModule,
    PrimeTemplate,
    TableModule,
    LogsComponent,
    NgIf,
    DialogModule,
    LucideAngularModule,
    OverlayPanelModule,
    ButtonDirective,
    NgTemplateOutlet,
    NgClass,
    CardComponent,
    DropdownModule,
    InputTextModule,
    FormsModule,
    TooltipModule
  ],
  template: `
    <div class="flex flex-column gap-4">
      <sm-card [padContent]="false" *ngIf="showFilterPanel()">
        <div class="formgrid grid text-sm p-fluid">
          <div class="field col-4">
            <label>Search</label>
            <input pInputText placeholder="Run name..." [(ngModel)]="nameFilter"/>
          </div>
          <div class="field col-2">
            <label>Type</label>
            <p-dropdown [options]="jobTypes()" [(ngModel)]="typeFilter"></p-dropdown>
          </div>
          <div class="field col-2">
            <label>Status</label>
            <p-dropdown [options]="jobStatuses" [(ngModel)]="statusFilter"></p-dropdown>
          </div>
          <div class="field col-2">
            <label>Started</label>
            <p-dropdown [options]="startedTime" [(ngModel)]="timeFilter"></p-dropdown>
          </div>
          @if (createButtonTemplate) {
            <div class="col-2 flex align-items-center justify-content-start pt-1">
              <ng-container [ngTemplateOutlet]="createButtonTemplate"></ng-container>
            </div>
          }
        </div>
      </sm-card>

      <sm-card header="Active">
        <p-table [value]="displayActiveJobs" dataKey="id" rowExpandMode="single" [expandedRowKeys]="expandedRows()"
                 [tableStyle]="{'table-layout': 'auto', 'width': '100%'}" styleClass="text-sm">
          <ng-template pTemplate="header">
            <tr>
              <th class="text-500 min-col">Run</th>
              <th class="text-500 min-col">Type</th>
              <th class="text-500 min-col-sm">Provisioning Status</th>
              <th class="text-500 min-col-sm">Running Status</th>
              <th class="text-500 min-col-sm">Start Time</th>
              <th class="text-500 min-col-sm">End Time</th>
              <th class="text-500">Actions</th>
            </tr>
          </ng-template>
          <ng-template pTemplate="body" let-job let-expanded="expanded">
            <tr [ngClass]="{'font-italic text-500': actionLoading(job)}">
              <td>
                @if (shouldShowDetailPane(job)) {
                  <a class="underline" (click)="showDetailPane(job)">{{ job.name }}</a>
                } @else {
                  {{ job.name }}
                }
              </td>
              <td>{{ jobTypeLabel(job.type) }}</td>
              <td>{{ statusOrJobField(job, 'provisioningStatus', 'provisioningStatus') }}</td>
              <td>
                @if (statusesLoading() || statusOrJobField(job, 'provisioningStatus', 'provisioningStatus') === IJobStatus.DEPLOYING) {
                  <span><i class="pi pi-spinner pi-spin"></i></span>
                } @else {
                  <div class="flex flex-column gap-2" tooltipPosition="left"
                       [pTooltip]="statusOrJobField(job, 'message', null) ?? statusOrJobField(job, 'stage', '-')">
                    <p>{{ statusOrJobField(job, 'stage', '-') }}</p>
                    @if (progressTemplate && statusOrJobField(job, 'progress', null)) {
                      <ng-container [ngTemplateOutlet]="progressTemplate" [ngTemplateOutletContext]="{task: job, $implicit: statusOrJobField(job, 'progress', null)}"></ng-container>
                    }
                  </div>
                }
              </td>
              <td>{{ statusOrJobField(job, 'startTime', 'startTime', '-') }}</td>
              <td>{{ statusOrJobField(job, 'completionTime', 'endTime', '-') }}</td>
              <td>
                @if (deleteLoading().includes(job.id)) {
                  <span><i class="pi pi-spinner pi-spin"></i></span>
                } @else {
                  <button pButton *ngIf="!deleteLoading().includes(job.id)" (click)="showItemMenu($event, job)" size="small" text icon="pi pi-align-justify"></button>
                }
              </td>
            </tr>
          </ng-template>
          <ng-template pTemplate="rowexpansion">
            <tr>
              <td colspan="7" class="p-0 surface-50">
                @if (currentJob()) {
                  <div class="flex flex-column gap-2">
                    <div class="w-full"><button pButton label="Close" size="small" outlined (click)="hideJobPane()" class="w-full"></button></div>
                    @if (detailPaneMode() === 'logs') {
                      <div class="px-2">
                        <sm-logs [resourceId]="currentJob().id"
                                 [resourceType]="executor() === ExecutorType.RAY ? 'rayJob' : 'taskRun'"
                                 [podName]="currentJob().podName"
                                 [containerId]="currentJob().container"
                                 class="block"></sm-logs>
                      </div>
                    } @else if (detailTemplate && detailPaneMode() === 'detail') {
                      <ng-container [ngTemplateOutlet]="detailTemplate" [ngTemplateOutletContext]="{$implicit: currentJob()}"></ng-container>
                    } @else if (chatTemplate && detailPaneMode() === 'chat') {
                      <ng-container [ngTemplateOutlet]="chatTemplate" [ngTemplateOutletContext]="{$implicit: currentJob()}"></ng-container>
                    }
                  </div>
                }
              </td>
            </tr>
          </ng-template>
        </p-table>
      </sm-card>

      <sm-card header="History">
        <p-table [value]="displayHistoricJobs" dataKey="id" rowExpandMode="single" [expandedRowKeys]="expandedRows()"
                 [tableStyle]="{'table-layout': 'auto', 'width': '100%'}" styleClass="text-sm">
          <ng-template pTemplate="header">
            <tr>
              <th class="text-500 min-col">Run</th>
              <th class="text-500 min-col">Type</th>
              <th class="text-500 min-col-sm">Provisioning Status</th>
              <th class="text-500 min-col-sm">Last Running Status</th>
              <th class="text-500 min-col-sm">Start Time</th>
              <th class="text-500 min-col-sm">End Time</th>
              <th class="text-500">Actions</th>
            </tr>
          </ng-template>
          <ng-template pTemplate="body" let-job let-expanded="expanded">
            <tr [ngClass]="{'font-italic text-500': actionLoading(job)}">
              <td>
                @if (shouldShowDetailPane(job)) {
                  <a class="underline" (click)="showDetailPane(job)">{{ job.name }}</a>
                } @else {
                  {{ job.name }}
                }
              </td>
              <td>{{ jobTypeLabel(job.type) }}</td>
              <td>{{ statusOrJobField(job, 'provisioningStatus', 'provisioningStatus') }}</td>
              <td>
                <div class="flex flex-column gap-2">
                  <p>{{ job.completedStatus ?? '-' }}</p>
                </div>
              </td>
              <td>{{ statusOrJobField(job, 'startTime', 'startTime', '-') }}</td>
              <td>{{ statusOrJobField(job, 'completionTime', 'endTime', '-') }}</td>
              <td>
                <button pButton (click)="showItemMenu($event, job)" size="small" text icon="pi pi-align-justify"></button>
              </td>
            </tr>
          </ng-template>
          <ng-template pTemplate="rowexpansion">
            <tr>
              <td colspan="7" class="p-0 surface-50">
                @if (currentJob()) {
                  <div class="flex flex-column gap-2">
                    <div class="w-full"><button pButton label="Close" size="small" outlined (click)="hideJobPane()" class="w-full"></button></div>
                    @if (detailPaneMode() === 'logs') {
                      <div class="px-2">
                        <sm-logs [resourceId]="currentJob().id"
                                 [resourceType]="executor() === ExecutorType.RAY ? 'rayJob' : 'taskRun'"
                                 [podName]="currentJob().podName"
                                 [containerId]="currentJob().container"
                                 class="block"></sm-logs>
                      </div>
                    } @else if (detailTemplate && detailPaneMode() === 'detail') {
                      <ng-container [ngTemplateOutlet]="detailTemplate" [ngTemplateOutletContext]="{$implicit: currentJob()}"></ng-container>
                    } @else if (chatTemplate && detailPaneMode() === 'chat') {
                      <ng-container [ngTemplateOutlet]="chatTemplate" [ngTemplateOutletContext]="{$implicit: currentJob()}"></ng-container>
                    }
                  </div>
                }
              </td>
            </tr>
          </ng-template>
        </p-table>
      </sm-card>
    </div>

    <p-confirmPopup />
    <p-overlayPanel [dismissable]="true">
      @if (currentJob()) {
        <div class="flex flex-column text-sm gap-2">
          <div *ngIf="shouldShowEditLink()" (click)="edit()" class="px-2 py-1 flex gap-2 align-items-center cursor-pointer hover:bg-primary-50">
            <i-lucide [img]="Pencil" class="w-1rem h-1rem"></i-lucide>
            <span>Edit</span>
          </div>
          <div *ngIf="shouldShowChatLink()" (click)="showChat()" class="px-2 py-1 flex gap-2 align-items-center cursor-pointer hover:bg-primary-50">
            <i-lucide [img]="MessageSquare" class="w-1rem h-1rem"></i-lucide>
            <span>Chat</span>
          </div>
          <div *ngIf="shouldShowLogsLink()" (click)="showLogs()" class="px-2 py-1 flex gap-2 align-items-center cursor-pointer hover:bg-primary-50">
            <i-lucide [img]="Download" class="w-1rem h-1rem"></i-lucide>
            <span>View Logs</span>
          </div>
          <div *ngIf="!cancelLoading().includes(currentJob().id) && shouldShowCancelLink()" class="p-2 flex gap-2 align-items-center cursor-pointer hover:bg-primary-50" (click)="cancel($event)">
            <i-lucide [img]="Copy" class="w-1rem h-1rem"></i-lucide>
            <span>Cancel</span>
          </div>
          <div *ngIf="!deleteLoading().includes(currentJob().id)" (click)="delete($event)" class="p-2 flex gap-2 align-items-center cursor-pointer text-red-500 hover:bg-primary-50">
            <i-lucide [img]="Trash" class="w-1rem h-1rem"></i-lucide>
            <span>Delete</span>
          </div>
        </div>
      }
    </p-overlayPanel>
  `,
  styles: `
    .min-col {
      min-width: 200px;
    }
    .min-col-sm {
      min-width: 180px;
    }
  `
})
export class JobListComponent implements AfterContentInit, OnDestroy {
  readonly confirmationService = inject(ConfirmationService);
  readonly statusService = inject(StatusService);
  readonly store = inject(Store);
  readonly taskRunService = inject(TaskRunService);
  readonly rayJobService = inject(RayJobService);
  readonly router = inject(Router);

  @ContentChildren(PrimeTemplate) templates: QueryList<PrimeTemplate> | undefined;
  createButtonTemplate: TemplateRef<any> | undefined;
  detailTemplate: TemplateRef<any> | undefined;
  chatTemplate: TemplateRef<any> | undefined;
  progressTemplate: TemplateRef<any> | undefined;

  executor = input.required<ExecutorType>();
  type = input.required<IJobType[]>();
  snapshotTime = input<number>(0);
  editRoute = input<string>(null);
  showFilterPanel = input<boolean>(true);
  actionMenu = viewChild(OverlayPanel);

  reload = signal(this.snapshotTime());
  statuses = signal<IJobRunStatus[]>([]);
  statusesLoading = signal<boolean>(true);

  activeJobs = derivedAsync(this.getJobs());
  historicJobs = derivedAsync(this.getJobs(true));
  // Filters
  nameFilter = signal<string>('');
  typeFilter = signal<IJobType>(null);
  statusFilter = signal<IJobStatus>(null);
  timeFilter = signal<number>(null);

  getJobs(history?: boolean): () => Observable<IJob[]> {
    return (): Observable<IJob[]> => {
      this.reload();
      this.snapshotTime();
      // Filters reload
      this.nameFilter() || this.typeFilter() || this.statusFilter() || this.timeFilter();

      if (this.type() && this.executor()) {
        const service =
          this.executor() === ExecutorType.RAY ? this.rayJobService : this.taskRunService;
        const criteria = [
          { key: 'name.contains', value: this.nameFilter() },
          { key: 'type.in', value: this.typeFilter() ?? this.type() },
          {
            key: 'provisioningStatus.in',
            value: history ?
              this.filteredHistoryStatuses() ?? this.historyStatuses :
              this.filteredActiveStatuses() ?? this.activeStatuses
          }
        ];
        if (this.computedTimeFilter()) {
          criteria.push(
            {
              key: 'startTime.greaterThan', value: this.computedTimeFilter()
            }
          );
        }
        return service.query({...DEFAULT_QUERY, sort: ['createdDate,desc'], criteria})
          .pipe(
            catchError((e) => displayErrorAndRethrow(this.store, e)),
            tap(jobs => !history && this.reinitStatus(jobs))
          )
      } else {
        return of([]);
      }
    }
  }

  statusOrJobField = (job: any, statusField: string, jobFieldOrValue: string, value?: string) => {
    const status = this.statuses().find(s => s.jobId === job.id) as any;
    if (status?.[statusField]) {
      return status[statusField];
    } else {
      return job[jobFieldOrValue] ?? value ?? jobFieldOrValue;
    }
  };

  jobTypeLabel(type: IJobType) {
    return JobTypeLabels[type];
  }

  displayActiveJobs: IJob[] = [];
  displayHistoricJobs: IJob[] = [];
  cancelLoading = signal([]);
  deleteLoading = signal([]);
  actionLoading = (job: IJob) => job &&
    (this.deleteLoading().includes(job.id) || this.cancelLoading().includes(job.id));

  expandedRows = signal<Record<string, boolean>>({});
  currentJob = signal<IJob>(null);
  detailPaneMode = signal<'logs'|'detail'|'chat'|''>('');

  statusSubscription?: Subscription;
  destroy$ = new Subject<void>();

  constructor() {
    effect(() => {
      this.displayActiveJobs = this.activeJobs() || [];
      this.displayHistoricJobs = this.historicJobs() || [];
    });
  }

  ngAfterContentInit() {
    this.templates.forEach((item) => {
      switch (item.getType()) {
        case 'detail':
          this.detailTemplate = item.template;
          break;
        case 'chat':
          this.chatTemplate = item.template;
          break;
        case 'progress':
          this.progressTemplate = item.template;
          break;
        case 'createButton':
          this.createButtonTemplate = item.template;
          break;
      }
    });
  }

  public async reinitStatus(jobs: IJob[]) {
    try {
      const jobsToMonitor = this.monitoredJobIds(jobs);
      await this.destroyStatus(jobsToMonitor);
      if (jobs && jobs.length > 0) {
        // Recreate the destroy$ subject for the new subscription
        this.destroy$ = new Subject<void>();
        this.statusesLoading.set(true);
        if (jobsToMonitor.length == 0) {
          this.statusesLoading.set(false);
          return;
        }

        const events = this.executor() === ExecutorType.TEKTON ?
          this.statusService.connectToTaskRunStatus(jobsToMonitor) :
          this.statusService.connectToRayJobStatus(jobsToMonitor);
        this.statusSubscription = events
          .pipe(
            distinctUntilChanged(),
            takeUntil(this.destroy$),
            map(event => {
              event.data = event.data.map(status => {
                status.jobId = this.executor() === ExecutorType.TEKTON ? status.taskId : status.rayJobId;
                return status;
              });
              return event;
            })
          )
          .subscribe({
            next: (event: SseEvent<IJobRunStatus[]>) => {
              this.statusesLoading.set(false);
              if (event.type === SseEventTypeTimeout) {
                return;
              } else if (event.type === 'taskstatus' || event.type === 'rayjobstatus') {
                this.updateStatus(event.data);
              }
            },
            error: (error) => displayError(this.store, error.message || 'An error occurred')
          });
      }
    } catch (e) {
      displayError(this.store, e);
    }
  }

  private monitoredJobIds(jobs: IJob[]) {
    return jobs
      .filter(j => j.provisioningStatus !== IJobStatus.ERROR)
      .map(j => j.id);
  }

  private updateStatus(statuses: IJobRunStatus[]) {
    this.statuses.set(statuses);
    this.displayActiveJobs.forEach(job => {
      const status = statuses.find(s => s.jobId === job.id);
      this.mergeJobWithStatus(job, status);
    })
  }

  async cancel(event: Event) {
    const job = this.currentJob();
    this.confirmationService.confirm({
      target: event.target,
      message: 'Are you sure you want to proceed?',
      icon: 'pi pi-exclamation-triangle',
      accept: async () => {
        try {
          this.hideJobPane();
          this.cancelLoading().push(job.id);
          await lastValueFrom(
            this.executor() === ExecutorType.TEKTON ?
              this.taskRunService.cancel(job.id) :
              this.rayJobService.cancel(job.id)
          );
          this.reload.update(s => s + 1);
        } catch (e) {
          displayError(this.store, e);
        } finally {
          this.cancelLoading().splice(this.cancelLoading().indexOf(job.id), 1);
        }
      }
    });
  }

  async delete(event: Event) {
    const job = this.currentJob();
    this.confirmationService.confirm({
      target: event.target,
      message: 'Are you sure you want to proceed?',
      icon: 'pi pi-exclamation-triangle',
      accept: async () => {
        try {
          this.hideJobPane();
          this.deleteLoading().push(job.id);
          await lastValueFrom(
            this.executor() === ExecutorType.TEKTON ?
              this.taskRunService.delete(job.id) :
              this.rayJobService.delete(job.id)
          );
          this.reload.update(s => s + 1);
        } catch (e) {
          displayError(this.store, e);
        } finally {
          this.deleteLoading().splice(this.deleteLoading().indexOf(job.id), 1);
        }
      }
    });
  }

  async destroyStatus(monitoredJobIds?: string[]) {
    if (this.statusSubscription) {
      this.statusSubscription.unsubscribe();
      this.statusSubscription = null;
    }

    this.destroy$.next();
    this.destroy$.complete();

    const monitoredJobs = monitoredJobIds ?? this.monitoredJobIds(this.displayActiveJobs);
    if (monitoredJobs.length) {
      this.executor() === ExecutorType.TEKTON ?
        await lastValueFrom(this.statusService.stopJobRunStatus(monitoredJobs)) :
        await lastValueFrom(this.statusService.stopRayJobStatus(monitoredJobs));
    }
  }

  showItemMenu(event: MouseEvent, job: IJob): void {
    this.actionMenu().show(event);
    this.currentJob.set(this.newJobWithStatus(job));
  }

  shouldShowDetailPane(job: IJob) {
    return (job.provisioningStatus !== IJobStatus.DEPLOYING) &&
      (job.stage !== 'TaskRunCancelled' && job.stage !== 'TaskRunTimeout');
  }

  showDetailPane(job: IJob) {
    if (this.currentJob()) {
      // hide pane
      this.hideJobPane();
      return;
    }
    this.currentJob.set(this.newJobWithStatus(job));
    this.detailPaneMode.set('detail');
    this.expandedRows.set({ [job.id]: true });
  }

  showLogs() {
    const job = this.currentJob();
    if (job) {
      this.expandedRows.set({ [job.id]: true });
      this.detailPaneMode.set('logs');
      this.actionMenu().hide();
    }
  }

  showChat() {
    const job = this.currentJob();
    if (job) {
      this.expandedRows.set({ [job.id]: true });
      this.detailPaneMode.set('chat');
      this.actionMenu().hide();
    }
  }

  async edit() {
    await this.router.navigate([this.editRoute(), this.currentJob().id]);
  }

  shouldShowLogsLink() {
    if (!this.currentJob() || this.detailPaneMode() === 'logs') {
      return false;
    }
    const job = this.currentJob();
    return (job.provisioningStatus === IJobStatus.DEPLOYED
        || job.provisioningStatus === IJobStatus.COMPLETED
        || job.provisioningStatus === IJobStatus.CANCELLED
        || job.provisioningStatus === IJobStatus.ERROR) &&
      (job.stage !== 'TaskRunCancelled' && job.stage !== 'TaskRunTimeout');
  }

  shouldShowEditLink() {
    if (!this.currentJob() || !this.editRoute()) {
      return false;
    }
    const job = this.currentJob();
    return (job.provisioningStatus === IJobStatus.CREATED
        || job.provisioningStatus === IJobStatus.DEPLOYED
        || job.provisioningStatus === IJobStatus.COMPLETED
        || job.provisioningStatus === IJobStatus.CANCELLED
        || job.provisioningStatus === IJobStatus.ERROR);
  }

  shouldShowChatLink() {
    if (!this.currentJob() || !this.chatTemplate) {
      return false;
    }
    const job = this.currentJob();
    return job.provisioningStatus === IJobStatus.COMPLETED && (job.stage === 'SUCCEEDED' || job.completedStatus === 'SUCCEEDED');
  }

  shouldShowCancelLink() {
    const job = this.currentJob();
    return (job.provisioningStatus !== IJobStatus.ERROR
      && job.provisioningStatus !== IJobStatus.CREATED
      && job.provisioningStatus !== IJobStatus.COMPLETED
      && job.provisioningStatus !== IJobStatus.CANCELLED) &&
      (job.stage !== 'TaskRunCancelled' && job.stage !== 'TaskRunTimeout');
  }

  hideJobPane() {
    this.currentJob.set(null);
    this.expandedRows.set({});
    this.detailPaneMode.set('');
  }

  newJobWithStatus(job: IJob) {
    const statuses = this.statuses() || [];
    const status = statuses.find(s => s.jobId === job.id);
    return this.mergeJobWithStatus({ ...job }, status);
  }

  mergeJobWithStatus(job: IJob, status: IJobRunStatus) {
    if (status) {
      job.provisioningStatus = status.provisioningStatus || job.provisioningStatus;
      job.stage = status.stage;
      job.message = status.message;
      job.startTime = status.startTime || job.startTime;
      job.endTime = status.completionTime || job.endTime;
      job.podName = status.podName || job.podName;
      job.container = status.container || job.container;
    }
    return job;
  }

  async ngOnDestroy() {
    await this.destroyStatus();
  }

  protected readonly MessageSquare = MessageSquare;
  protected readonly Download = Download;
  protected readonly Trash = Trash;
  protected readonly Copy = Copy;
  protected readonly Pencil = Pencil;

  activeStatuses = [IJobStatus.CREATED, IJobStatus.ERROR, IJobStatus.DEPLOYING, IJobStatus.DEPLOYED];
  historyStatuses = [IJobStatus.COMPLETED, IJobStatus.CANCELLED];
  filteredActiveStatuses = computed(() => {
    if (!this.statusFilter()) {
      return this.activeStatuses;
    }
    return this.activeStatuses.indexOf(this.statusFilter()) >= 0 ? [this.statusFilter()] : [];
  });
  filteredHistoryStatuses = computed(() => {
    if (!this.statusFilter()) {
      return this.historyStatuses;
    }
    return this.historyStatuses.indexOf(this.statusFilter()) >= 0 ? [this.statusFilter()] : [];
  });

  computedTimeFilter = computed(() => {
    if (!this.timeFilter()) {
      return null;
    }

    const date = new Date();
    date.setDate(date.getDate() - this.timeFilter());
    return date.toISOString();
  });

  // Filter LOVs
  jobTypes = computed(() => {
    const types = this.type().map(type => ({
      label: this.jobTypeLabel(type),
      value: [type]
    }));
    return [
      {
        label: 'All',
        value: null
      },
      ...types
    ]
  });
  jobStatuses: any[] = [
    { label: 'All', value: null },
    { label: 'Created', value: IJobStatus.CREATED },
    { label: 'Deploying', value: IJobStatus.DEPLOYING },
    { label: 'Deployed', value: IJobStatus.DEPLOYED },
    { label: 'Error', value: IJobStatus.ERROR },
    { label: 'Completed', value: IJobStatus.COMPLETED },
    { label: 'Cancelled', value: IJobStatus.CANCELLED }
  ];
  startedTime: any[] = [
    { label: 'Any time', value: null },
    { label: 'Last 24h', value: 1 },
    { label: 'Last 7 days', value: 7 },
    { label: 'Last 30 days', value: 30 }
  ];

  protected readonly Plus = Plus;
  protected readonly ExecutorType = ExecutorType;
  protected readonly IJobStatus = IJobStatus;
}
