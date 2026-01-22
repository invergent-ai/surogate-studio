import { Component, Input, OnInit } from '@angular/core';
import { CardModule } from 'primeng/card';
import { ButtonDirective } from 'primeng/button';
import { Activity, CircleCheck, Globe, Info, LucideAngularModule, Network, Pencil } from 'lucide-angular';
import { TagModule } from 'primeng/tag';
import { InputTextModule } from 'primeng/inputtext';
import { NgIf } from '@angular/common';
import SharedModule from '../../../shared/shared.module';
import { TooltipModule } from 'primeng/tooltip';
import { IApplication } from '../../../shared/model/application.model';
import { displayError } from '../../../shared/util/error.util';
import { debounceTime, distinctUntilChanged, lastValueFrom, Subscription } from 'rxjs';
import { Store } from '@ngxs/store';
import { Selectors } from '../../../shared/state/selectors';
import { ApplicationService } from '../../../shared/service/application.service';
import { ApplicationMode } from '../../../shared/model/enum/application-mode.model';
import { AppControlComponent } from './status/app-control/app-control.component';
import { ConfirmPopupModule } from 'primeng/confirmpopup';
import HasAnyAuthorityDirective from '../../../shared/auth/has-any-authority.directive';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { ConfirmationService } from 'primeng/api';
import { MenuService } from '../../layout/service/app-menu.service';
import { Router } from '@angular/router';
import { displaySuccess } from '../../../shared/util/success.util';
import { IAppTemplate, NewAppTemplate } from '../../../shared/model/app-template.model';
import { map } from 'rxjs/operators';
import { HttpResponse } from '@angular/common/http';
import { AppTemplateService } from '../../../shared/service/app-template.service';
import { getTemplate } from '../../../shared/util/template.util';
import { ApplicationStatus, ResourceStatusStage } from '../../../shared/model/enum/application-status.model';
import { AppStatusWithResources } from '../../../shared/model/k8s/app-status.model';
import { IProject } from '../../../shared/model/project.model';
import { Account } from '../../../shared/model/account.model';
import { UserService } from '../../../shared/service/user.service';

@Component({
  standalone: true,
  selector: 'sm-app-header-card',
  templateUrl: './app-header-card.component.html',
  imports: [
    CardModule,
    ButtonDirective,
    LucideAngularModule,
    TagModule,
    InputTextModule,
    NgIf,
    SharedModule,
    TooltipModule,
    AppControlComponent,
    ConfirmPopupModule,
    HasAnyAuthorityDirective,
    OverlayPanelModule
  ]
})
export class AppHeaderCardComponent implements OnInit {
  @Input({required:true}) application: IApplication;
  @Input() appStatus: AppStatusWithResources;
  @Input() showStatus = true;
  @Input() showControls = true;
  @Input({required:true}) ingressHostName: string;
  @Input() internalEndpoint: string;

  isEditingName = false;
  isEditingDescription = false;
  isEditingProject = false;
  newName: string = '';
  newDescription: string = '';
  newProject: IProject;
  projects: IProject[] = [];

  appsSubscription?: Subscription;
  apps: IApplication[];

  templateName: string;
  templateCategory: string;
  templateDescription: string;
  templateImport: IAppTemplate;
  templates: IAppTemplate[];

  user?: Account;

  constructor(
    private store: Store,
    private applicationService: ApplicationService,
    private confirmationService: ConfirmationService,
    private menuService: MenuService,
    private router: Router,
    private appTemplateService: AppTemplateService,
    private userService: UserService,
  ) {}

  async ngOnInit() {
    if (this.appsSubscription) {
      await this.destroyAppSubscription();
    }

    this.appsSubscription = this.store.select(Selectors.apps)
      .pipe(
        debounceTime(150),
        distinctUntilChanged()
      ).subscribe({
        next: (apps) => {
          this.apps = apps;
        }
      });

    this.store.select(Selectors.projects).subscribe(projects => {
      this.projects = projects;
    });

    this.user = await this.userService.getUserDetails();
  }

  editName() {
    this.newName = this.application.name;
    this.isEditingName = true;
  }

  editDescription() {
    this.newDescription = this.application.description;
    this.isEditingDescription = true;
  }

  editProject() {
    if (this.appStatus?.status === ApplicationStatus.CREATED) {
      this.newProject = this.application.project;
      this.isEditingProject = true;
    }
  }

  async saveName() {
    if (!this.newName || this.newName.trim() === '') {
      displayError(this.store, 'Application Name cannot be empty');
      return;
    }
    if (this.apps.filter(app => app.id !== this.application.id &&
      app.name.toLowerCase() === this.newName.toLowerCase().trim()).length) {
      displayError(this.store, 'You already have an application named: ' + this.newName.trim());
      return;
    }

    try {
      this.application.name = this.newName.trim();
      await lastValueFrom(this.applicationService.save(this.application));
      this.application.name = this.newName.trim();
      this.isEditingName = false;
    } catch (e) {
      displayError(this.store, e);
    }
  }

  async saveDescription() {
    try {
      this.application.description = this.newDescription;
      await lastValueFrom(this.applicationService.save(this.application));
      this.application.description = this.newDescription;
      this.isEditingDescription = false;
    } catch (e) {
      displayError(this.store, e);
    }
  }

  async saveProject() {
    try {
      this.application.project = this.newProject;
      await lastValueFrom(this.applicationService.save(this.application));
      this.application.project = this.newProject;
      this.isEditingProject = false;
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

  async deleteApp(event: Event) {
    this.confirmationService.confirm({
      key: 'confirmDelete',
      target: event.target || new EventTarget,
      message: `Do you want to remove all data for App ${this.application.name}?`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Remove',
      rejectLabel: "Remove data",
      rejectVisible: false,
      accept: async () => {
        await this.delete(this.application.id, true);
      },
      reject: async () => {
        await this.delete(this.application.id, false);
      }
    });
  }

  private async delete(id: string, keepVolumes: boolean) {
    try {
      await lastValueFrom(this.applicationService.delete(id, keepVolumes));
      this.menuService.reload(this.application?.project?.id);
    } catch (err) {
      console.log(err);
    } finally {
      await this.router.navigate(['/']);
    }
  }

  async importTemplate() {
    if (!this.templateImport || !this.templateImport.template) {
      return;
    }

    const app = JSON.parse(this.templateImport.template);
    this.application.containers.push(...app.containers);
    displaySuccess(this.store, 'Containers from template "' + this.templateImport.name + '" have been imported successfully.');
  }

  async loadTemplates() {
    this.templateImport = null;
    if (this.templates?.length) {
      return;
    }

    this.templates = await lastValueFrom(
      this.appTemplateService.query()
        .pipe(map((res: HttpResponse<IAppTemplate[]>) => res.body ?? []))
    );
  }

  async generateTemplate() {
    try {
      await lastValueFrom(this.appTemplateService.create({
        name: this.templateName,
        category: this.templateCategory,
        description: this.templateDescription,
        template: getTemplate(this.application)
      } as NewAppTemplate));
      displaySuccess(this.store, 'App Template generated successfully');
    } catch (err) {
      displayError(this.store, (err as any)?.message || 'An error occurred');
    }
  }

  async destroyAppSubscription() {
    if (this.appsSubscription) {
      this.appsSubscription.unsubscribe();
      this.appsSubscription = null;
    }
  }

  protected readonly Info = Info;
  protected readonly Globe = Globe;
  protected readonly Activity = Activity;
  protected readonly Pencil = Pencil;

  protected readonly ApplicationMode = ApplicationMode;
  protected readonly ApplicationStatus = ApplicationStatus;
  protected readonly ResourceStatusStage = ResourceStatusStage;
  protected readonly Network = Network;
  protected readonly CircleCheck = CircleCheck;
}
