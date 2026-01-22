import { Component, OnDestroy, OnInit } from '@angular/core';
import { PageComponent } from '../../shared/components/page/page.component';
import { PageLoadComponent } from '../../shared/components/page-load/page-load.component';
import { LayoutService } from '../../shared/service/theme/app-layout.service';
import { ConfirmationService } from 'primeng/api';
import { ActivatedRoute, Router } from '@angular/router';
import { Store } from '@ngxs/store';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ProjectService } from '../../shared/service/project.service';
import { debounceTime, distinctUntilChanged, lastValueFrom, Subject } from 'rxjs';
import { displayError } from '../../shared/util/error.util';
import { Selectors } from '../../shared/state/selectors';
import { takeUntil } from 'rxjs/operators';
import { IProject, IProjectResource, IProjectResourceType } from '../../shared/model/project.model';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { ButtonModule } from 'primeng/button';
import { LoadProjectsAction } from '../../shared/state/actions';
import { TableModule } from 'primeng/table';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { CardModule } from 'primeng/card';
import { NgIf } from '@angular/common';
import { ConfirmPopupModule } from 'primeng/confirmpopup';
import { Profile } from '../../shared/model/enum/profile.model';
import { displaySuccess } from '../../shared/util/success.util';
import { MenuService } from '../layout/service/app-menu.service';

@Component({
  standalone: true,
  imports: [
    PageComponent,
    PageLoadComponent,
    FormsModule,
    ReactiveFormsModule,
    InputTextModule,
    InputTextareaModule,
    ButtonModule,
    TableModule,
    ConfirmDialogModule,
    CardModule,
    NgIf,
    ConfirmPopupModule
  ],
  templateUrl: './project.page.html'
})
export class ProjectPage implements OnInit, OnDestroy {
  readonly welcomeItems = [
    { title: 'What is Surogate ?', link: 'https://surogate.ai' },
  ];

  projectForm: FormGroup;
  destroy$ = new Subject<void>();
  loading = true;
  project: IProject;
  projectResources: IProjectResource[] = [];

  constructor(
    public layoutService: LayoutService,
    private projectService: ProjectService,
    private confirmationService: ConfirmationService,
    private route: ActivatedRoute,
    private router: Router,
    private store: Store,
    private menuService: MenuService,
    fb: FormBuilder
  ) {
    this.projectForm = fb.group({
      name: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50)]],
      description: [null, [Validators.maxLength(250)]],
      zone: [null, [Validators.required]],
      profile: [Profile.HYBRID, [Validators.required]]
    });
  }

  ngOnInit() {
    this.layoutService.state.helpItems = this.welcomeItems;
    this.route.queryParams.subscribe(async params => {
      try {
        const id = params['id'];
        this.store.select(Selectors.projects)
          .pipe(
            debounceTime(150),
            distinctUntilChanged(),
            takeUntil(this.destroy$)
          )
          .subscribe({
            next: async (projects) => {
              if (projects?.length) {
                const currentProject = projects.find(project => project.id === id);
                if (currentProject) {
                  await this.setProject(currentProject);
                }
              }
            },
            error: (_) => displayError(this.store, new Error('Failed to update project data'))
          });
      } catch (e) {
        displayError(this.store, e);
      } finally {
        this.loading = false;
      }
    });
  }

  async setProject(currentProject: IProject) {
    this.project = JSON.parse(JSON.stringify(currentProject));
    Promise.resolve(null).then(() => {
      this.projectForm.patchValue({
        name: currentProject.name,
        description: currentProject.description,
        zone: currentProject.zone,
        profile: currentProject.profile ?? Profile.HYBRID
      });
    });

    try {
      this.projectResources = await lastValueFrom(this.projectService.resources(currentProject.id));
    } catch (e) {
      displayError(this.store, e);
    }
  }

  async saveProject() {
    try {
      const data = {
        id: this.project.id,
        name: this.projectForm.value.name,
        description: this.projectForm.value.description,
        zone: this.projectForm.value.zone,
        profile: this.projectForm.value.profile
      };
      await lastValueFrom(this.projectService.partialUpdate(data));
      this.store.dispatch(new LoadProjectsAction());
      displaySuccess(this.store, 'Project updated');
    } catch (e) {
      displayError(this.store, e);
    }
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  gotoResource(res: IProjectResource) {
    switch (res.type) {
      case IProjectResourceType.APPLICATION:
        this.router.navigate(['/apps'], { queryParams: { id: res.id } });
        break;
      case IProjectResourceType.VIRTUAL_INSTANCE:
        this.router.navigate(['/vms'], { queryParams: { id: res.id } });
        break;
      case IProjectResourceType.VOLUME:
        this.router.navigate(['/volumes'], { queryParams: { id: res.id } });
        break;
      case IProjectResourceType.DATABASE:
        this.router.navigate(['/dbs'], { queryParams: { id: res.id } });
        break;
    }
  }

  deleteProject(event: Event) {
    this.confirmationService.confirm({
      key: 'confirmDelete',
      target: event.target || new EventTarget,
      message: `Are you sure you want to delete project ${this.project.name}?`,
      icon: 'pi pi-exclamation-triangle',
      accept: async () => {
        try {
          await lastValueFrom(this.projectService.delete(this.project.id));
          this.store.dispatch(new LoadProjectsAction());
          await this.router.navigate(['']);
          this.menuService.reload();
        } catch (e) {
          displayError(this.store, e);
        }
      }
    });
  }
}
