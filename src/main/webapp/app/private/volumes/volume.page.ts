import { Component, OnDestroy, OnInit } from '@angular/core';
import { PageComponent } from '../../shared/components/page/page.component';
import { PageLoadComponent } from '../../shared/components/page-load/page-load.component';
import { debounceTime, distinctUntilChanged, lastValueFrom, Subject } from 'rxjs';
import { ActivatedRoute, Router } from '@angular/router';
import { ConfirmationService } from 'primeng/api';
import { Store } from '@ngxs/store';
import { VolumeService } from '../../shared/service/volume.service';
import { Selectors } from '../../shared/state/selectors';
import { map, takeUntil } from 'rxjs/operators';
import { displayError } from '../../shared/util/error.util';
import { IProject } from '../../shared/model/project.model';
import { IVolume } from '../../shared/model/volume.model';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { InputTextModule } from 'primeng/inputtext';
import { ApplicationStatus } from '../../shared/model/enum/application-status.model';
import { DropdownModule } from 'primeng/dropdown';
import { NgIf } from '@angular/common';
import { IVolumeMount } from '../../shared/model/volume-mount.model';
import SharedModule from '../../shared/shared.module';
import { FieldsetModule } from 'primeng/fieldset';
import { CardModule } from 'primeng/card';
import { VolumeMountService } from '../../shared/service/volume-mount.service';
import { revalidateForm } from '../../shared/util/form.util';
import { VolumeType } from '../../shared/model/enum/volume-type.model';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmPopupModule } from 'primeng/confirmpopup';
import { displaySuccess } from '../../shared/util/success.util';
import { MenuService } from '../layout/service/app-menu.service';
import { LayoutService } from '../../shared/service/theme/app-layout.service';

@Component({
  standalone: true,
  templateUrl: './volume.page.html',
  imports: [
    PageComponent,
    PageLoadComponent,
    FormsModule,
    ReactiveFormsModule,
    InputTextModule,
    DropdownModule,
    NgIf,
    SharedModule,
    SharedModule,
    FieldsetModule,
    CardModule,
    ConfirmDialogModule,
    ConfirmPopupModule
  ]
})
export class VolumePage implements OnInit, OnDestroy {
  protected readonly ApplicationStatus = ApplicationStatus;

  readonly welcomeItems = [
    { title: 'What is Surogate ?', link: 'https://surogate.ai/' },
    { title: 'Volumes in Surogate', link: 'https://docs.statemesh.net/applications/intro' },
    { title: 'Manage your Volumes', link: 'https://docs.statemesh.net/applications/manage' },
  ];

  loading = true;
  destroy$ = new Subject<void>();
  volume: IVolume;
  volumeForm: FormGroup;
  projects: IProject[] = [];
  mounts: IVolumeMount[] = [];
  createMode = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private confirmationService: ConfirmationService,
    private volumeService: VolumeService,
    private volumeMountService: VolumeMountService,
    private store: Store,
    private menuService: MenuService,
    private layoutService: LayoutService,
    fb: FormBuilder
  ) {
    this.volumeForm = fb.group({
      name: [null, [Validators.required]],
      type: [null, [Validators.required]],
      size: [1, Validators.min(1)],
      project: [null, [Validators.required]],
      bucketUrl: [null],
      accessKey: [null],
      accessSecret: [null]
    });
  }

  async ngOnInit() {
    this.layoutService.state.helpItems = this.welcomeItems;
    this.route.queryParams.subscribe(async params => {
      try {
        const id = params['id'];
        if (id) {
          this.store.select(Selectors.volumes)
            .pipe(
              debounceTime(150),
              distinctUntilChanged(),
              takeUntil(this.destroy$)
            )
            .subscribe({
              next: async (volumes) => {
                if (volumes?.length) {
                  const currentVolume = volumes.find(volume => volume.id === id);
                  if (currentVolume) {
                    await this.setCurrentVolume(currentVolume);
                  }
                }
              },
              error: (_) => displayError(this.store, new Error('Failed to update page data'))
            });
        } else {
          this.setCreateMode();
        }
      } catch (e) {
        displayError(this.store, e);
      } finally {
        this.loading = false;
      }
    });

    this.store.select(Selectors.projects).subscribe(projects => {
      this.projects = projects;
    });
  }

  setCreateMode() {
    this.volumeForm.reset({ size: 1, type: 'statemesh' }, { emitEvent: false });
    this.createMode = true;
  }

  async setCurrentVolume(volume: IVolume) {
    this.volume = JSON.parse(JSON.stringify(volume));
    this.volumeForm.patchValue({
      name: volume.name,
      type: volume.type === VolumeType.TEMPORARY ? 'ephemeral' : volume.bucketUrl ? 'byos' : 'statemesh',
      size: volume.size,
      project: volume.project,
      bucketUrl: volume.bucketUrl,
      accessKey: volume.accessKey,
      accessSecret: volume.accessSecret
    }, { emitEvent: false });

    if (this.volume?.project?.id) {
      const selectedProject = this.projects.filter(p => p.id === this.volume.project.id);
      if (selectedProject?.length) {
        this.volumeForm.get('project').setValue(selectedProject[0]);
      }
    }

    try {
      this.mounts = await lastValueFrom(this.volumeMountService.query({
        page: 0,
        size: 100,
        sort: [] as any,
        criteria: [{ key: 'volumeId.equals', value: volume.id }]
      }).pipe(map(res => res.body ?? [])));
    } catch (e) {
      displayError(this.store, e);
    }
  }

  gotoResource(res: IVolumeMount) {
    if (res.applicationId) {
      this.router.navigate(['/apps'], { queryParams: { id: res.applicationId } });
    }
    if (res.database?.id) {
      this.router.navigate(['/dbs'], { queryParams: { id: res.database.id } });
    }
  }

  mountResourceType(res: IVolumeMount) {
    if (res.applicationId) {
      return 'APPLICATION';
    } else if (res.database?.id) {
      return 'DATABASE';
    }
    return '';
  }

  mountResourceName(res: IVolumeMount) {
    if (res.applicationId) {
      const apps = this.store.selectSnapshot(Selectors.apps);
      return apps.find(app => app.id === res.applicationId)?.name;
    } else if (res.database?.id) {
      const dbs = this.store.selectSnapshot(Selectors.dbs);
      return dbs.find(db => db.id === res.database.id)?.name;
    }
    return '';
  }

  async saveVolume() {
    revalidateForm(this.volumeForm);
    if (!this.volumeForm.valid) {
      return;
    }

    const volume: IVolume = {
      id: this.createMode ? null : this.volume.id,
      name: this.volumeForm.value.name,
      size: this.volumeForm.value.size,
      project: this.volumeForm.value.project,
      type: this.volumeForm.value.type === 'ephemeral' ? VolumeType.TEMPORARY : VolumeType.PERSISTENT,
      bucketUrl: this.volumeForm.value.bucketUrl,
      accessKey: this.volumeForm.value.accessKey,
      accessSecret: this.volumeForm.value.accessSecret
    };

    try {
      if (this.createMode) {
        const result = await lastValueFrom(this.volumeService.create(volume));
        await this.menuService.reload(volume.project?.id);
        await this.router.navigate(['']);
      } else {
        await lastValueFrom(this.volumeService.partialUpdate(volume));
        await this.menuService.reload(volume.project?.id);
        displaySuccess(this.store, 'Volume updated successfully.');
      }
    } catch (e) {
      displayError(this.store, e);
    }
  }

  async deleteVolume(event: Event) {
    if (this.mounts.length > 0) {
      displayError(this.store, 'The Volume is mounted. Please unmount it before deleting.');
      return;
    }

    this.confirmationService.confirm({
      key: 'confirmDelete',
      target: event.target || new EventTarget,
      message: `Are you sure you want to delete volume ${this.volume.name}?`,
      icon: 'pi pi-exclamation-triangle',
      accept: async () => {
        try {
          await lastValueFrom(this.volumeService.delete(this.volume.id));
          await this.router.navigate(['']);
          this.menuService.reload(this.volume?.project?.id);
        } catch (e) {
          displayError(this.store, e);
        }
      }
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
