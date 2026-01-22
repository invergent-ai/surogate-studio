import {Component, EventEmitter, Input, Output} from '@angular/core';
import {ButtonModule} from 'primeng/button';
import {DropdownModule} from 'primeng/dropdown';
import {InputNumberModule} from 'primeng/inputnumber';
import {InputTextModule} from 'primeng/inputtext';
import {InputTextareaModule} from 'primeng/inputtextarea';
import {NgIf} from '@angular/common';
import {PageLoadComponent} from '../../../../shared/components/page-load/page-load.component';
import {PanelModule} from 'primeng/panel';
import {RadioButtonModule} from 'primeng/radiobutton';
import {FormGroup, ReactiveFormsModule} from '@angular/forms';
import {RippleModule} from 'primeng/ripple';
import {IZone} from '../../../../shared/model/zone.model';
import {lastValueFrom} from 'rxjs';
import {Router} from '@angular/router';
import {displayError} from '../../../../shared/util/error.util';
import {revalidateForm} from '../../../../shared/util/form.util';
import {Store} from '@ngxs/store';
import {TooltipModule} from 'primeng/tooltip';
import {Selectors} from '../../../../shared/state/selectors';
import {UserService} from '../../../../shared/service/user.service';
import {IProject} from '../../../../shared/model/project.model';
import {IDatabase} from '../../../../shared/model/database.model';
import {DatabaseStatus} from '../../../../shared/model/enum/database-status.model';
import {DatabaseService} from '../../../../shared/service/database.service';
import {DatabaseFormService} from '../../../../shared/service/form/database-form.service';
import {displaySuccess} from '../../../../shared/util/success.util';
import {MenuService} from "../../../layout/service/app-menu.service";

@Component({
  selector: 'sm-db-details',
  standalone: true,
  styleUrls: ['./db-details.component.scss'],
  templateUrl: './db-details.component.html',
  imports: [
    ButtonModule,
    DropdownModule,
    InputNumberModule,
    InputTextModule,
    InputTextareaModule,
    PageLoadComponent,
    PanelModule,
    RadioButtonModule,
    ReactiveFormsModule,
    RippleModule,
    TooltipModule
  ]
})
export class DbDetailsComponent {
  @Input()
  set database(database: IDatabase) {
    this.initDatabase(database);
  }
  @Output() onCancel = new EventEmitter<void>();

  loading = true;
  isSaving = false;

  _database: IDatabase;
  projects: IProject[] = [];
  zones: IZone[] = [];

  databaseForm: FormGroup;

  constructor(
    private databaseService: DatabaseService,
    private userService: UserService,
    private router: Router,
    private databaseFormService: DatabaseFormService,
    private store: Store,
    private menuService: MenuService
  ) {}

  async initDatabase(database: IDatabase) {
    this._database = database;
    this.databaseForm = this.databaseFormService.createDatabaseForm();

    await this.initAssets();
    if (!database) {
      // This is a new database
      this.databaseForm.get('project').setValue(this.projects[0]);
      return;
    }

    await this.initExistingDatabase(database);
  }

  private async initAssets() {
    try {
      this.zones = this.store.selectSnapshot(Selectors.zones);
      this.store.select(Selectors.projects).subscribe(projects => {
        this.projects = projects;
      });
    } catch (e) {
      displayError(this.store, e);
    } finally {
      this.loading = false;
    }
  }

  private async initExistingDatabase(database: IDatabase) {
    this.databaseForm.patchValue(database);
    this.setCurrentProject(database);
  }

  private setCurrentProject(database: IDatabase) {
    const project = this.projects.filter(project => project.id === database.project.id);
    this.databaseForm.get('project').setValue(project.length ? project[0] : null);
  }

  async saveDatabase() {
    revalidateForm(this.databaseForm);
    if (this.databaseForm.invalid) {
      displayError(this.store, new Error('Please fill in all required fields'));
      return;
    }

    let database = this.databaseForm.getRawValue();
    try {
      this.isSaving = true;
      if (!database.id) {
        // Create database
        database.status = DatabaseStatus.CREATED;
      } else {
        // Update database
        database = JSON.parse(JSON.stringify(this._database))
        database.project = this.databaseForm.getRawValue().project;
      }
      const saved = await lastValueFrom(this.databaseService.save(database));
      displaySuccess(this.store, 'Database ' + (database.id ? 'updated' : 'created') + ' successfully');
      await this.router.navigate(['/dbs'], {queryParams: { id: saved.body.id }});
      this.menuService.reload(database.project?.id);
    } catch (e) {
      displayError(this.store, e);
    } finally {
      this.isSaving = false;
    }
  }

  cancel() {
    this.resetForms();
    this.onCancel.emit()
  }

  private resetForms(): void {
    this.databaseForm = this.databaseFormService.createDatabaseForm();
  }
}
