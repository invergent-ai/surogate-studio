import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {debounceTime, distinctUntilChanged, lastValueFrom, Subject, Subscription} from 'rxjs';
import {displayError} from '../../shared/util/error.util';
import {ConfirmationService} from 'primeng/api';
import {PageComponent} from '../../shared/components/page/page.component';
import {PageLoadComponent} from '../../shared/components/page-load/page-load.component';
import {NgIf} from '@angular/common';
import {TabViewModule} from 'primeng/tabview';
import {DbConfigComponent} from './components/config/db-config.component';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {ChipsModule} from 'primeng/chips';
import {TagModule} from 'primeng/tag';
import {ConfirmDialogModule} from 'primeng/confirmdialog';
import {Store} from '@ngxs/store';
import {ConfirmPopupModule} from 'primeng/confirmpopup';
import {ProgressSpinnerModule} from 'primeng/progressspinner';
import {TooltipModule} from 'primeng/tooltip';
import {takeUntil} from 'rxjs/operators';
import {StatusService} from '../../shared/service/k8s/status.service';
import {OverlayPanelModule} from 'primeng/overlaypanel';
import {Selectors} from '../../shared/state/selectors';
import {InputNumberModule} from 'primeng/inputnumber';
import {RippleModule} from 'primeng/ripple';
import {InputTextareaModule} from 'primeng/inputtextarea';
import {IProject} from '../../shared/model/project.model';
import {DropdownModule} from 'primeng/dropdown';
import {CardModule} from 'primeng/card';
import {ToolbarModule} from 'primeng/toolbar';
import {DatabaseStatus} from '../../shared/model/enum/database-status.model';
import {DatabaseService} from '../../shared/service/database.service';
import {IDatabase} from '../../shared/model/database.model';
import {DbStatus} from '../../shared/model/k8s/db-status.model';
import {PasswordModule} from 'primeng/password';
import {ClipboardModule} from '@angular/cdk/clipboard';
import {displaySuccess, displayWarning} from '../../shared/util/success.util';
import {MenuService} from "../layout/service/app-menu.service";
import {serviceName} from '../../shared/util/naming.util';
import {ResourceStatusStage} from "../../shared/model/enum/application-status.model";
import {DbHeaderCardComponent} from "./components/db-header-card.component";
import {MessagesModule} from "primeng/messages";
import {LayoutService} from "../../shared/service/theme/app-layout.service";

type Severity = 'success' | 'info' | 'warning' | 'danger' | 'secondary' | 'contrast';


@Component({
  standalone: true,
  templateUrl: './db.page.html',
  styleUrls: ['./db.page.scss'],
  imports: [
    PageComponent,
    PageLoadComponent,
    NgIf,
    TabViewModule,
    DbConfigComponent,
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
    ReactiveFormsModule,
    PasswordModule,
    ClipboardModule,
    DbHeaderCardComponent,
    MessagesModule
  ]
})

export class DbPage implements OnInit, OnDestroy {

  readonly welcomeItems = [
    {title: 'What is Surogate ?', link: 'https://surogate.ai'},
    {title: 'Applications in StateMesh', link: 'https://docs.statemesh.net/applications/intro'},
    {title: 'Configure your Application', link: 'https://docs.statemesh.net/applications/configure'},
    {title: 'Manage your Application', link: 'https://docs.statemesh.net/applications/manage'},
    {title: 'Storage Providers', link: 'https://docs.statemesh.net/applications/storage-providers'},
  ];
  DatabaseStatus = DatabaseStatus;

  // Add this line to make serviceName available in the template
  protected readonly serviceName = serviceName;

  private statusSubscription?: Subscription;
  private dbsSubscription?: Subscription;
  private destroy$ = new Subject<void>();
  dbStatus: DbStatus;
  dbs: IDatabase[];
  database: IDatabase;
  projects: IProject[] = [];
  activeTab = 0;
  isEditingName = false;
  isEditingDescription = false;
  isEditingProject = false;
  newName: string = '';
  newDescription: string = '';
  newProject: IProject;
  reinitTimeout: any;
  ingressHostName: string;
  password: string;
  showDetails = false;
  message: string = '';
  details: string[] = [];
  loading = true;
  lockScreen = false;
  lockedByConfig = false;
  timeoutOccurred = false;
  reinitAttempt = false;

  cpu: string;
  ram: string;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private databaseService: DatabaseService,
    private confirmationService: ConfirmationService,
    private statusService: StatusService,
    private store: Store,
    private menuService: MenuService,
    private layoutService: LayoutService
  ) {
  }

  async ngOnInit() {
    this.layoutService.state.helpItems = this.welcomeItems;

    this.route.queryParams.subscribe(async params => {
      await this.init(params['id']);
    });

    this.store.select(Selectors.projects).subscribe(projects => {
      this.projects = projects;
    });
  }

  async init(databaseId: string) {
    if (this.dbsSubscription) {
      await this.destroyDbSubscription();
    }

    try {
      this.dbsSubscription = this.store.select(Selectors.dbs)
        .pipe(
          debounceTime(150),
          distinctUntilChanged()
        )
        .subscribe({
          next: (dbs) => {
            if (this.lockedByConfig) {
              return;
            }

            this.dbs = dbs;
            this.initDatabase(dbs, databaseId);
          },
          error: () => {
          }
        });
    } catch (e) {
      displayError(this.store, e);
    } finally {
      this.loading = false;
    }
  }

  async initDatabase(dbs: IDatabase[], databaseId: string) {
    if (dbs && dbs.length) {
      const currentDb = dbs.filter(db => db.id === databaseId);
      if (currentDb.length && currentDb[0]) {
        const existingDbId = this.database?.id;
        if (existingDbId && existingDbId !== databaseId) {
          await this.destroyStatus();
          if (currentDb[0].status !== DatabaseStatus.CREATED) {
            await this.destroyDbSubscription();
          }
        }

        this.database = JSON.parse(JSON.stringify(currentDb[0]));
        this.ingressHostName = this.database.ingressHostName;
        if (this.database?.status === DatabaseStatus.DEPLOYED) {
          this.databaseService.password(this.database).subscribe({
            next: (res) => this.password = res.body.value,
            error: (error) => console.log(error)
          });
        }

        if (existingDbId !== databaseId || this.database.status === DatabaseStatus.DEPLOYING) {
          await this.initStatus();
        }
        this.computeResources();
      }
    }
  }

  async initStatus(force?: boolean) {
    // Don't start status monitoring for databases that haven't been deployed
    if (!force && this.database?.status === DatabaseStatus.CREATED) {
      console.log('Database is in CREATED state, skipping status monitoring');
      return;
    }

    if (this.database?.status !== DatabaseStatus.CREATED || force) {
      // First, destroy the old connection completely
      await this.destroyStatus();

      // Small delay to ensure cleanup is complete
      await new Promise(resolve => setTimeout(resolve, 100));

      // Now start the new status monitoring
      this.statusService.startDbStatus(this.database?.id).subscribe({
        next: () => {
          // Wait a bit before connecting to ensure the backend is ready
          setTimeout(() => this.connectToStatus(), 100);
        },
        error: (error) => {
          console.log('Failed to start status monitoring:', error);
          // Optionally show a user-friendly message
          if (this.database?.status === DatabaseStatus.CREATED) {
            this.message = 'Database must be deployed before monitoring status';
          }
        }
      });
    }
  }

  connectToStatus(): void {
    if (this.statusSubscription) {
      return;
    }

    this.statusSubscription = this.statusService.connectToDbStatus(this.database?.id)
      .pipe(
        debounceTime(150),
        distinctUntilChanged(),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (status: DbStatus) => {
          if (status?.type) {
            if (status.type === "timeout") {
              this.handleTimeout();
            } else if (status.type === "disconnect") {
              this.handleDisconnect(status.error);
            }
            return;
          }

          if (!this.lockScreen) {
            this.updateStatus(status);
          }
        },
        error: (error) => {
          console.error('Status connection error:', error);
          this.handleError(error);
        },
        complete: () => {
          console.log('Status connection completed');
        }
      });
  }

  private updateStatus(status: DbStatus) {
    if (this.database) {
      this.dbStatus = status;

      // Note: status.status doesn't exist in DbStatus from backend
      // The database status is managed separately via the database entity

      // Update messages
      if (status.message) {
        this.message = status.message;
      } else {
        this.message = '';
      }

      if (status.details?.length) {
        this.details = status.details;
      } else {
        this.details = [];
      }
    }
  }

  async reinitStatus(force?: boolean) {
    this.reinitAttempt = true;
    await this.initStatus(force);
    this.reinitAttempt = false;
  }

  editName() {
    this.newName = this.database.name;
    this.isEditingName = true;
  }

  editDescription() {
    this.newDescription = this.database.description;
    this.isEditingDescription = true;
  }

  editProject() {
    if (this.database?.status === DatabaseStatus.CREATED) {
      this.newProject = this.database.project;
      this.isEditingProject = true;
    }
  }

  async deleteDatabase(event: Event) {
    this.confirmationService.confirm({
      key: 'confirmDelete',
      target: event.target || new EventTarget,
      message: `Do you want to remove all data for Database ${this.database.name}?`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Don\'t remove data',
      rejectLabel: "Remove data",
      accept: async () => {
        await this.delete(this.database.id, true);
      },
      reject: async () => {
        await this.delete(this.database.id, false);
      }
    });
  }

  private async delete(id: string, keepVolumes: boolean) {
    this.lockScreen = true;
    try {
      await lastValueFrom(this.databaseService.delete(id, keepVolumes));
      this.menuService.reload(this.database?.project?.id);
    } catch (err) {
      console.log(err);
    } finally {
      this.lockScreen = false;
      await this.router.navigate(['/']);
    }
  }

  async saveName() {
    if (!this.newName || this.newName.trim() === '') {
      displayError(this.store, 'Database name cannot be empty');
      return;
    }
    if (this.dbs.filter(db => db.id !== this.database.id &&
      db.name.toLowerCase() === this.newName.toLowerCase().trim()).length) {
      displayError(this.store, 'You already have a database named: ' + this.newName.trim());
      return;
    }

    try {
      this.database.name = this.newName.trim();
      await lastValueFrom(this.databaseService.save(this.database));
      this.database.name = this.newName.trim();
      this.isEditingName = false;
      displaySuccess(this.store, 'Database name updated successfully');
    } catch (e) {
      displayError(this.store, e);
    }
  }

  async saveDescription() {
    try {
      this.database.description = this.newDescription;
      await lastValueFrom(this.databaseService.save(this.database));
      this.database.description = this.newDescription;
      this.isEditingDescription = false;
      displaySuccess(this.store, 'Database description updated successfully');
    } catch (e) {
      displayError(this.store, e);
    }
  }

  async saveProject() {
    try {
      this.database.project = this.newProject;
      await lastValueFrom(this.databaseService.save(this.database));
      this.database.project = this.newProject;
      this.isEditingProject = false;
      displaySuccess(this.store, 'Database updated successfully');
    } catch (e) {
      displayError(this.store, e);
    }
  }

  cancelNameEdit() {
    this.isEditingName = false;
    this.newName = '';
  }

  cancelDescriptionEdit() {
    this.isEditingDescription = false;
    this.newDescription = '';
  }

  cancelProjectEdit() {
    this.isEditingProject = false;
    this.newProject = null;
  }

  computeResources() {
    this.cpu = this.database.cpuLimit + '';
    this.ram = (+this.database.memLimit / 1024) + '';
  }

  async handleControlFinished() {
    if (this.timeoutOccurred) {
      this.timeoutOccurred = false;
      await this.reinitStatus(true);
    }
  }

  public handleTimeout(): void {
    if (!this.timeoutOccurred) {
      this.timeoutOccurred = true;
      displayWarning(this.store, 'The connection has been terminated due to inactivity. Click the warning icon to reconnect.');
    }
  }

  public handleDisconnect(error: string): void {
    if (!this.timeoutOccurred) {
      this.timeoutOccurred = true;
      displayWarning(this.store, error || 'Connection was interrupted. Click the warning icon to reconnect.');
    }
  }

  private handleError(error: any): void {
    displayError(this.store, error.message || 'An error occurred');
  }

  async refreshConnection() {
    this.timeoutOccurred = false;
    await this.reinitStatus(true);
  }

  async destroyDbSubscription() {
    if (this.dbsSubscription) {
      this.dbsSubscription.unsubscribe();
      this.dbsSubscription = null;
    }
  }

  async destroyStatus() {
    if (this.statusSubscription) {
      this.statusSubscription.unsubscribe();
      this.statusSubscription = null;
    }

    this.message = '';
    this.details = [];
    this.dbStatus = null; // Clear the status

    this.destroy$.next();
    this.destroy$.complete();

    // Recreate destroy$ for next use
    this.destroy$ = new Subject<void>();

    if (this.reinitTimeout) {
      clearTimeout(this.reinitTimeout);
    }

    // Stop the backend status monitoring
    if (this.database?.id) {
      try {
        await lastValueFrom(this.statusService.stopDbStatus(this.database));
      } catch (e) {
        console.error('Error stopping status:', e);
      }
    }
  }


  getDbStatusSeverity(status: DatabaseStatus | string | undefined): Severity {
    if (!status) return 'secondary';

    const severityMap: Record<string, Severity> = {
      [DatabaseStatus.ERROR]: 'danger',
      [DatabaseStatus.DEPLOYED]: 'success',
      [DatabaseStatus.DEPLOYING]: 'warning',
      [DatabaseStatus.CREATED]: 'secondary'
    };
    return severityMap[status] ?? 'secondary';
  }

  getResourceStageSeverity(stage: string | undefined): Severity {
    if (!stage) return 'secondary';

    const severityMap: Record<string, Severity> = {
      [ResourceStatusStage.RUNNING]: 'success',
      [ResourceStatusStage.COMPLETED]: 'success',
      [ResourceStatusStage.FAILED]: 'danger',
      [ResourceStatusStage.DEGRADED]: 'danger',
      [ResourceStatusStage.INITIALIZING]: 'warning',
      [ResourceStatusStage.WAITING]: 'warning',
      [ResourceStatusStage.RESTARTING]: 'warning',
      [ResourceStatusStage.UNKNOWN]: 'secondary'
    };
    return severityMap[stage] ?? 'secondary';
  }

  getDbStatusTooltip(status: DatabaseStatus | string | undefined): string {
    if (!status) return '';

    const tooltipMap: Record<string, string> = {
      [DatabaseStatus.CREATED]: 'Database configuration created, ready to deploy',
      [DatabaseStatus.DEPLOYING]: 'Database is being deployed to the cluster',
      [DatabaseStatus.DEPLOYED]: 'Database is successfully deployed and running',
      [DatabaseStatus.ERROR]: 'Database encountered an error during deployment'
    };
    return tooltipMap[status] ?? status;
  }

  getResourceStageTooltip(): string {
    if (!this.dbStatus?.stage) return '';

    const stage = this.dbStatus.stage;
    const stageTooltips: Record<string, string> = {
      'INITIALIZING': 'Initialization containers are running',
      'RUNNING': 'All containers are running and ready',
      'WAITING': 'Waiting for containers to start (pulling images, etc.)',
      'RESTARTING': 'Container is restarting due to failure',
      'DEGRADED': 'Some containers are unhealthy',
      'COMPLETED': 'All containers completed successfully',
      'FAILED': 'One or more containers failed',
      'UNKNOWN': 'Status information unavailable'
    };

    let tooltip = stageTooltips[stage] ?? stage;

    if (this.dbStatus.message) {
      tooltip += '\n\nMessage: ' + this.dbStatus.message;
    }

    if (this.details?.length) {
      tooltip += '\n\nClick info icon for details';
    }

    return tooltip;
  }

  isStatusTransitioning(): boolean {
    const transitioningStatuses: DatabaseStatus[] = [DatabaseStatus.DEPLOYING];
    const transitioningStages = ['INITIALIZING', 'WAITING'];

    const statusMatches = this.database?.status &&
      transitioningStatuses.includes(this.database.status as DatabaseStatus);
    const stageMatches = this.dbStatus?.stage &&
      transitioningStages.includes(this.dbStatus.stage);

    return Boolean(statusMatches || stageMatches);
  }

  async publishing(publishing: boolean) {
    this.lockedByConfig = publishing;
    if (!publishing) {
      await this.reinitStatus(true);
    }
  }

  async ngOnDestroy() {
    await this.destroyStatus();
    await this.destroyDbSubscription();
  }
}
