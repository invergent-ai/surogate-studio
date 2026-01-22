import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { DropdownModule } from 'primeng/dropdown';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { PageLoadComponent } from '../../../../shared/components/page-load/page-load.component';
import { PanelModule } from 'primeng/panel';
import { RadioButtonModule } from 'primeng/radiobutton';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { RippleModule } from 'primeng/ripple';
import { IApplication } from '../../../../shared/model/application.model';
import { IZone } from '../../../../shared/model/zone.model';
import { ApplicationFormService, ContainerFormData } from '../../../../shared/service/form/application-form.service';
import { first, lastValueFrom } from 'rxjs';
import { ApplicationService } from '../../../../shared/service/application.service';
import { ActivatedRoute, Router } from '@angular/router';
import { displayError } from '../../../../shared/util/error.util';
import { revalidateForm } from '../../../../shared/util/form.util';
import { ApplicationType } from '../../../../shared/model/enum/application-type.model';
import { WorkloadType } from '../../../../shared/model/enum/workload-type.model';
import { ApplicationStatus } from '../../../../shared/model/enum/application-status.model';
import { Store } from '@ngxs/store';
import { TooltipModule } from 'primeng/tooltip';
import { Selectors } from '../../../../shared/state/selectors';
import { UserService } from '../../../../shared/service/user.service';
import { IProject } from '../../../../shared/model/project.model';
import { displaySuccess } from '../../../../shared/util/success.util';
import { MenuService } from '../../../layout/service/app-menu.service';
import { ApplicationMode } from '../../../../shared/model/enum/application-mode.model';

@Component({
  selector: 'sm-app-details',
  standalone: true,
  styleUrls: ['./app-details.component.scss'],
  templateUrl: './app-details.component.html',
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
export class AppDetailsComponent {
  @Input()
  set application(application: IApplication) {
    this.initApplication(application);
  }
  @Input()
  mode: ApplicationMode = ApplicationMode.APPLICATION;
  @Output() onCancel = new EventEmitter<void>();

  ApplicationStatus = ApplicationStatus;

  loading = true;
  isSaving = false;

  _application: IApplication;
  projects: IProject[] = [];
  zones: IZone[] = [];

  applicationForm: FormGroup;
  containers: ContainerFormData[] = [];

  constructor(
    private applicationService: ApplicationService,
    private userService: UserService,
    private router: Router,
    private activatedRoute: ActivatedRoute,
    private applicationFormService: ApplicationFormService,
    private store: Store,
    private menuService: MenuService
  ) {}

  initApplication(application: IApplication) {
    this._application = application;
    this.applicationForm = this.applicationFormService.createApplicationForm();

    this.activatedRoute.queryParams.pipe(first()).subscribe(async params => {
      if (!params['id']) {
        await this.router.navigateByUrl('/404');
        return;
      }

      await this.initAssets();
      if (!application) {
        // This is a new application
        this.applicationForm.get('project').setValue(this.projects[0]);
        return;
      }

      await this.initExistingApplication(application);
    });
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

  private async initExistingApplication(application: IApplication) {
    this.applicationForm.patchValue(application);
    this.setCurrentProject(application);
  }

  private setCurrentProject(application: IApplication) {
    const project = this.projects.filter(project => project.id === application.project.id);
    this.applicationForm.get('project').setValue(project.length ? project[0] : null);
  }

  async saveApplication() {
    revalidateForm(this.applicationForm);
    if (this.applicationForm.invalid) {
      displayError(this.store, new Error('Please fill in all required fields'));
      return;
    }

    let application = this.applicationForm.getRawValue();
    try {
      this.isSaving = true;
      if (!application.id) {
        // Create application
        application.mode = this.mode;
        application.type = ApplicationType.UI;
        application.status = ApplicationStatus.CREATED;
        application.workloadType  = WorkloadType.DEPLOYMENT;
      } else {
        // Update application
        application = JSON.parse(JSON.stringify(this._application))
        application.project = this.applicationForm.getRawValue().project;
      }
      const saved = await lastValueFrom(this.applicationService.save(application));
      displaySuccess(this.store, 'Application ' + (application.id ? 'updated' : 'created') + ' successfully');
      await this.router.navigate(['/apps'], {queryParams: { id: saved.id }});
      this.menuService.reload(application.project?.id);
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
    this.applicationForm = this.applicationFormService.createApplicationForm();
  }
}
