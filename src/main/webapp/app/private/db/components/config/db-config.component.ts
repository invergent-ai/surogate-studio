import {Component, EventEmitter, Input, OnDestroy, Output} from '@angular/core';
import {ButtonModule} from 'primeng/button';
import {DropdownModule} from 'primeng/dropdown';
import {InputNumberModule} from 'primeng/inputnumber';
import {InputTextModule} from 'primeng/inputtext';
import {InputTextareaModule} from 'primeng/inputtextarea';
import {NgIf} from '@angular/common';
import {PageLoadComponent} from '../../../../shared/components/page-load/page-load.component';
import {PanelModule} from 'primeng/panel';
import {RadioButtonModule} from 'primeng/radiobutton';
import {FormArray, FormGroup, ReactiveFormsModule} from '@angular/forms';
import {RippleModule} from 'primeng/ripple';
import {lastValueFrom, Subject} from 'rxjs';
import {ActivatedRoute, Router} from '@angular/router';
import {displayError} from '../../../../shared/util/error.util';
import {revalidateForm} from '../../../../shared/util/form.util';
import {DbSettingsComponent} from './db-settings/db-settings.component';
import {ProgressSpinnerModule} from "primeng/progressspinner";
import {UserService} from "../../../../shared/service/user.service";
import {MessagesModule} from 'primeng/messages';
import {IDatabase} from "../../../../shared/model/database.model";
import {DatabaseStatus} from "../../../../shared/model/enum/database-status.model";
import {DatabaseService} from "../../../../shared/service/database.service";
import {DatabaseFormService} from "../../../../shared/service/form/database-form.service";
import {IVolumeMount} from "../../../../shared/model/volume-mount.model";
import {CheckboxModule} from "primeng/checkbox";
import {Store} from '@ngxs/store';
import {displaySuccess} from '../../../../shared/util/success.util';
import {NewFirewallEntry} from "../../../../shared/model/firewall-entry.model";
import {CardModule} from "primeng/card";

@Component({
  selector: 'sm-db-config',
  standalone: true,
  templateUrl: './db-config.component.html',
  imports: [
    ButtonModule,
    DbSettingsComponent,
    DropdownModule,
    InputNumberModule,
    InputTextModule,
    InputTextareaModule,
    NgIf,
    PageLoadComponent,
    PanelModule,
    RadioButtonModule,
    ReactiveFormsModule,
    RippleModule,
    ProgressSpinnerModule,
    MessagesModule,
    CheckboxModule,
    CardModule
  ]
})
export class DbConfigComponent implements OnDestroy {
  @Input()
  set database(database: IDatabase) {
    this.initDatabase(database);
  }
  @Output()
  onCancel = new EventEmitter<void>();
  @Output()
  publishing = new EventEmitter<boolean>();

  DatabaseStatus = DatabaseStatus;

  protected destroy$ = new Subject<void>();
  protected resetTrigger = new Subject<void>();

  _database: IDatabase;
  databaseForm: FormGroup;

  loading = false;
  isSaving = false;
  isPublishing = false;
  moreSeconds = false;
  lockScreen = false;
  lockTimeout: any;

  constructor(
    private databaseService: DatabaseService,
    private router: Router,
    private activatedRoute: ActivatedRoute,
    private databaseFormService: DatabaseFormService,
    private store: Store,
    private userService: UserService
  ) {}

  initDatabase(database: IDatabase) {
    this._database = database;
    this.activatedRoute.queryParams.subscribe(async params => {
      if (!params['id']) {
        await this.router.navigateByUrl('/404');
        return;
      }

      if (!database) {
        // This is a new database
        return;
      }

      await this.initExistingDatabase(database);
    });
  }

  private async initExistingDatabase(database: IDatabase) {
    this.databaseForm = this.databaseFormService.createDatabaseForm();
    this.databaseForm.patchValue(database);
    const volumeMountsArray = this.databaseForm.controls.volumeMounts as FormArray;
    database.volumeMounts.forEach(volume => {
      const volumeForm = this.databaseFormService.createVolumeMountForm(volume as IVolumeMount);
      volumeMountsArray.push(volumeForm);
    });
    const firewallEntriesArray = this.databaseForm.controls.firewallEntries as FormArray;
    database.firewallEntries.forEach(entry => {
      const entryForm = this.databaseFormService.createFirewallEntryForm(entry as NewFirewallEntry);
      firewallEntriesArray.push(entryForm);
    });
  }

  async saveDatabase(publish?: boolean): Promise<IDatabase> {
    revalidateForm(this.databaseForm);
    if (this.databaseForm.invalid) {
      displayError(this.store, new Error('Please fill in all required fields'));
      return Promise.reject();
    }

    try {
      this.isSaving = true;
      const database = this.databaseForm.getRawValue();

      // Re-add attributes not present in the form
      database.status = this._database.status;
      database.ingressHostName = this._database.ingressHostName;
      database.deployedNamespace = this._database.deployedNamespace;
      database.keepVolumes = true;
      await lastValueFrom(this.databaseService.save(database));

      if (publish) {
        if (!this.checkCredentials(database)) {
          return Promise.reject();
        }

        this.isSaving = false;
        this.isPublishing = true;
        this.lock();
        try {
          await lastValueFrom(this.databaseService.deploy(database));
        } catch(err) {
          console.log(err);
        } finally {
          this.unlock();
        }
      } else {
        displaySuccess(this.store, 'Database updated successfully');
      }

      return Promise.resolve(database);
    } catch (e) {
      displayError(this.store, e);
      this.unlock();
    } finally {
      this.isSaving = false;
      this.isPublishing = false;
    }

    return Promise.reject();
  }

  async redeployDatabase() {
    const database = await this.saveDatabase();
    if (!this.checkCredentials(database)) {
      return;
    }

    if (database) {
      this.isPublishing = true;
      this.lock();
      try {
        await lastValueFrom(this.databaseService.redeploy(database));
        displaySuccess(this.store, 'Database re-deployed successfully');
      } catch(err) {
        console.log(err);
      } finally {
        this.isPublishing = false;
        this.unlock();
      }
    }
  }

  private lock() {
    this.lockScreen = true;
    this.moreSeconds = false;
    if (this.lockTimeout) {
      clearTimeout(this.lockTimeout);
    }
    this.lockTimeout = setTimeout(() => {
      if (this.lockScreen) {
        this.moreSeconds = true;
      }
    }, 5000);
    this.publishing.emit(true);
  }

  private unlock() {
    this.lockScreen = false;
    this.moreSeconds = false;
    if (this.lockTimeout) {
      clearTimeout(this.lockTimeout);
    }
    this.publishing.emit(false);
  }

  private checkCredentials(database: IDatabase): boolean {
    if (!database) {
      return true;
    }
    let validCredentials = true;

    // S3 credentials
    if (database.volumeMounts) {
      database.volumeMounts.forEach(volumeMount => {
        if (volumeMount.volume.bucketUrl && !volumeMount.volume.accessSecret) {
          validCredentials = false;
          displayError(this.store, 'Please reenter your storage password for volume ' + volumeMount.volume?.name);
        }
      });
    }

    return validCredentials;
  }

  ngOnDestroy() {
    this.resetTrigger.complete();
    this.destroy$.next();
    this.destroy$.complete();

    if (this.lockTimeout) {
      clearTimeout(this.lockTimeout);
    }
  }
}
