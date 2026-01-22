import {Component, EventEmitter, Input, OnDestroy, OnInit, Output} from '@angular/core';
import {CardModule} from 'primeng/card';
import {Activity, CircleCheck, Globe, Info, LucideAngularModule, Network, Pencil} from 'lucide-angular';
import {TagModule} from 'primeng/tag';
import {InputTextModule} from 'primeng/inputtext';
import {NgIf} from '@angular/common';
import SharedModule from '../../../shared/shared.module';
import {TooltipModule} from 'primeng/tooltip';
import {IDatabase} from '../../../shared/model/database.model';
import {displayError} from '../../../shared/util/error.util';
import {lastValueFrom, Subscription} from 'rxjs';
import {Store} from '@ngxs/store';
import {DatabaseService} from '../../../shared/service/database.service';
import {DbControlComponent} from './db-control/db-control.component';
import {ConfirmPopupModule} from 'primeng/confirmpopup';
import {OverlayPanelModule} from 'primeng/overlaypanel';
import {ConfirmationService, MessageService} from 'primeng/api';
import {MenuService} from '../../layout/service/app-menu.service';
import {Router} from '@angular/router';
import {displaySuccess} from '../../../shared/util/success.util';
import {DatabaseStatus} from '../../../shared/model/enum/database-status.model';
import {DbStatus} from '../../../shared/model/k8s/db-status.model';
import {IProject} from '../../../shared/model/project.model';
import {serviceName} from '../../../shared/util/naming.util';
import {DropdownModule} from 'primeng/dropdown';
import {ReactiveFormsModule} from '@angular/forms';
import {StatusService} from '../../../shared/service/k8s/status.service';
import {filter} from "rxjs/operators";

@Component({
  standalone: true,
  selector: 'sm-db-header-card',
  templateUrl: './db-header-card.component.html',
  imports: [
    CardModule,
    LucideAngularModule,
    TagModule,
    InputTextModule,
    NgIf,
    SharedModule,
    TooltipModule,
    DbControlComponent,
    ConfirmPopupModule,
    OverlayPanelModule,
    DropdownModule,
    ReactiveFormsModule
  ]
})
export class DbHeaderCardComponent implements OnInit, OnDestroy {
  @Input({required: true}) database: IDatabase;
  @Input() dbStatus: DbStatus;
  @Input() showStatus = true;
  @Input() showControls = true;
  @Input({required: true}) ingressHostName: string;
  @Input() internalEndpoint: string;
  @Input() password: string;

  @Output() controlFinished = new EventEmitter<void>();

  isEditingName = false;
  isEditingDescription = false;
  isEditingProject = false;
  newName: string = '';
  newDescription: string = '';
  newProject: IProject;
  projects: IProject[] = [];

  dbs: IDatabase[];
  private statusSubscription?: Subscription;

  protected readonly Info = Info;
  protected readonly Globe = Globe;
  protected readonly Activity = Activity;
  protected readonly Pencil = Pencil;
  protected readonly DatabaseStatus = DatabaseStatus;
  protected readonly Network = Network;
  protected readonly CircleCheck = CircleCheck;
  protected readonly serviceName = serviceName;

  constructor(
    private store: Store,
    private databaseService: DatabaseService,
    private confirmationService: ConfirmationService,
    private menuService: MenuService,
    private router: Router,
    private statusService: StatusService,
    private messageService: MessageService,
  ) {
  }

  ngOnInit(): void {
    if (this.database?.id && this.showStatus) {
      this.startStatusMonitoring();
    }
  }

  private startStatusMonitoring(): void {
    this.statusService.startDbStatus(this.database.id).subscribe({
      next: () => {
        this.subscribeToStatusUpdates();
      },
      error: (err) => console.error('Failed to start database status monitoring:', err)
    });
  }

  private subscribeToStatusUpdates(): void {
    this.statusSubscription = this.statusService
      .connectToDbStatus(this.database.id)
      .pipe(
        filter(status => status && status.stage !== null) // Only pass through real status updates
      )
      .subscribe({
        next: (status: DbStatus) => {
          this.dbStatus = status;
        },
        error: (err) => {
          // This should now rarely be called
          console.debug('SSE stream ended, will reconnect');
        }
      });
  }

  ngOnDestroy(): void {
    this.statusSubscription?.unsubscribe();
    if (this.database?.id && this.showStatus) {
      this.statusService.stopDbStatus(this.database).subscribe({
        next: () => console.log('Database status monitoring stopped'),
        error: (err) => console.error('Failed to stop database status monitoring:', err)
      });
    }
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

  async handleControlFinished() {
    this.controlFinished.emit();
  }

  async saveName() {
    if (!this.newName || this.newName.trim() === '') {
      displayError(this.store, 'Database Name cannot be empty');
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

  copyToClipboard(text: string, label: string): void {
    navigator.clipboard.writeText(text).then(() => {
      this.messageService.add({
        severity: 'success',
        summary: 'Copied',
        detail: `${label} copied to clipboard`,
        life: 2000
      });
    }).catch(err => {
      console.error('Failed to copy:', err);
      this.messageService.add({
        severity: 'error',
        summary: 'Error',
        detail: 'Failed to copy to clipboard',
        life: 3000
      });
    });
  }

  copyPassword(): void {
    this.copyToClipboard(this.password, 'Password');
  }

  private async delete(id: string, keepVolumes: boolean) {
    try {
      await lastValueFrom(this.databaseService.delete(id, keepVolumes));
      this.menuService.reload(this.database?.project?.id);
    } catch (err) {
      console.log(err);
    } finally {
      await this.router.navigate(['/']);
    }
  }
}
