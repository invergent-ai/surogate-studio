import {Component, computed, inject, signal} from '@angular/core';
import {LakeFsService} from '../../shared/service/lake-fs.service';
import {PageComponent} from '../../shared/components/page/page.component';
import {PageLoadComponent} from '../../shared/components/page-load/page-load.component';
import {SelectButtonModule} from 'primeng/selectbutton';
import {FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators} from '@angular/forms';
import {ButtonDirective} from 'primeng/button';
import {
  Bot,
  Building2,
  CalendarArrowUp,
  CloudDownload,
  Download,
  Info,
  Lock,
  LucideAngularModule,
  Plus
} from 'lucide-angular';
import {TableModule} from 'primeng/table';
import {TagModule} from 'primeng/tag';
import {ApplicationMode} from '../../shared/model/enum/application-mode.model';
import {InputTextModule} from 'primeng/inputtext';
import {IconFieldModule} from 'primeng/iconfield';
import {InputIconModule} from 'primeng/inputicon';
import {Store} from '@ngxs/store';
import {Selectors} from '../../shared/state/selectors';
import dayjs from 'dayjs/esm';
import {DialogModule} from 'primeng/dialog';
import {InputTextareaModule} from 'primeng/inputtextarea';
import {revalidateForm} from '../../shared/util/form.util';
import {displayError, displayErrorAndRethrow} from '../../shared/util/error.util';
import {lastValueFrom} from 'rxjs';
import {LoadRepositoriesAction} from '../../shared/state/actions';
import {ICreateLakeFsRepository, LakeFsImportSource, LakeFsRepositoryType} from '../../shared/model/lakefs.model';
import {ChipsModule} from 'primeng/chips';
import {PasswordModule} from 'primeng/password';
import {displaySuccess} from '../../shared/util/success.util';
import {RouterLink} from '@angular/router';
import {TaskRunService} from '../../shared/service/task-run.service';
import {ITaskRun} from '../../shared/model/tasks.model';
import {TaskRunType} from '../../shared/model/enum/task-run-type.model';
import {AccountService} from '../../shared/service/account.service';
import {derivedAsync} from 'ngxtension/derived-async';
import {catchError} from 'rxjs/operators';
import {TaskRunProvisioningStatus} from '../../shared/model/enum/task-run-provision-status.model';
import {AutoCompleteCompleteEvent, AutoCompleteModule} from 'primeng/autocomplete';
import {DatePipe, NgIf} from '@angular/common';
import {CardComponent} from '../../shared/components/card/card.component';
import {ConfirmPopupModule} from 'primeng/confirmpopup';
import {injectQueryParams} from 'ngxtension/inject-query-params';
import {JobListComponent} from '../../shared/components/jobs/job-list.component';
import {ProgressBarModule} from 'primeng/progressbar';
import {roundUpTo} from '../../shared/util/display.util';
import {ExecutorType} from "../../shared/model/enum/executor-type.model";
import {IJobType} from "../../shared/model/job.model";

@Component({
  selector: 'app-repository-page',
  standalone: true,
  templateUrl: './hub.page.html',
  imports: [
    PageComponent,
    PageLoadComponent,
    SelectButtonModule,
    FormsModule,
    ButtonDirective,
    LucideAngularModule,
    TableModule,
    TagModule,
    InputTextModule,
    IconFieldModule,
    InputIconModule,
    DialogModule,
    ReactiveFormsModule,
    InputTextareaModule,
    ChipsModule,
    PasswordModule,
    RouterLink,
    AutoCompleteModule,
    DatePipe,
    NgIf,
    CardComponent,
    ConfirmPopupModule,
    JobListComponent,
    JobListComponent,
    ProgressBarModule
  ],
  styles: [`
    .field {
      color: var(--surface-500);
    }

    ::ng-deep .p-datatable .p-datatable-thead > tr > th {
      background-color: var(--surface-50);
    }

    ::ng-deep .p-selectbutton > .p-button {
      font-size: 0.875rem;
    }
  `]
})
export class HubPage {
  readonly lakeFsService = inject(LakeFsService);
  readonly store = inject(Store);
  readonly taskRunService = inject(TaskRunService);
  readonly accountService = inject(AccountService);

  type = injectQueryParams<LakeFsRepositoryType>('type');

  loading = this.store.selectSignal(Selectors.repositoriesLoading);
  displayRepoType = signal(this.type() || LakeFsRepositoryType.MODEL);
  taskType = computed(() => {
    if (this.displayRepoType() === LakeFsRepositoryType.MODEL) {
      return IJobType.IMPORT_HF_MODEL;
    } else if (this.displayRepoType() === LakeFsRepositoryType.DATASET) {
      return IJobType.IMPORT_HF_DATASET;
    } else {
      return IJobType.IMPORT_HF_MODEL;
    }
  });
  repositories = this.store.selectSignal(Selectors.repositories);
  projects = this.store.selectSignal(Selectors.projects);
  searchText = signal('');

  user = derivedAsync(() => this.accountService.identity(true)
    .pipe(catchError((e) => displayErrorAndRethrow(this.store, e))));

  newRepoDialogVisible = signal(false);
  newRepoLoading = signal(false);
  jobTypes = [IJobType.IMPORT_HF_MODEL, IJobType.IMPORT_HF_DATASET];
  newRepoForm = new FormGroup({
    repoType: new FormControl<string>(null, [Validators.required]),
    repoId: new FormControl<string>(null, [Validators.required, Validators.pattern(/^[a-zA-Z0-9][a-zA-Z0-9-.]{2,62}$/)]),
    defaultBranch: new FormControl<string>('main', [Validators.required]),
    description: new FormControl<string>(null),
    tags: new FormControl<string[]>([])
  });

  downloadDialogVisible = signal(false);
  downloadLoading = signal(false);
  downloadForm = new FormGroup({
    source: new FormControl(LakeFsImportSource.HF, [Validators.required]),
    repo: new FormControl(null, [Validators.required]),
    subset: new FormControl(null),
    token: new FormControl(null),
  });

  models = computed(() => this.repositories().filter(r => r.metadata?.type === LakeFsRepositoryType.MODEL));
  datasets = computed(() => this.repositories().filter(r => r.metadata?.type === LakeFsRepositoryType.DATASET));
  tblRepositories = computed(() => {
    let repos = this.displayRepoType() === LakeFsRepositoryType.MODEL ? this.models() : this.datasets();
    if (this.searchText()) {
      repos = repos.filter(r => r.id.toLowerCase().includes(this.searchText().toLowerCase()));
    }
    return repos;
  });

  hfSuggestions: any[] | undefined;
  taskSnapshotTime = signal(Date.now());
  taskBytesDownloaded = (progress: string) => Number(progress.split('/')[0]);
  taskTotalBytes = (progress: string) => Number(progress.split('/')[1]);
  taskProgress = (progress: string) => roundUpTo(this.taskBytesDownloaded(progress) / this.taskTotalBytes(progress) * 100, 2);

  async changeRepositoryType(type: LakeFsRepositoryType) {
    this.displayRepoType.set(type);
  }

  showNewRepoDialog() {
    this.newRepoForm.reset({ repoType: 'model', defaultBranch: 'main', tags: [] });
    this.newRepoDialogVisible.set(true);
  }

  showDownloadDialog() {
    this.downloadForm.reset({ source: LakeFsImportSource.HF });
    this.downloadDialogVisible.set(true);
  }

  async createRepository() {
    revalidateForm(this.newRepoForm);

    try {
      const { repoType, repoId, defaultBranch, description, tags } = this.newRepoForm.value;
      this.newRepoLoading.set(true);
      await lastValueFrom(this.lakeFsService.createRepository({
        id: repoId,
        description,
        metadata: { 'tags': (tags || []).join(','), 'type': repoType , 'displayName': repoId},
        defaultBranch
      } as ICreateLakeFsRepository));

      this.store.dispatch(new LoadRepositoriesAction());
      this.newRepoDialogVisible.set(false);
    } catch (e) {
      displayError(this.store, e);
    } finally {
      this.newRepoLoading.set(false);
    }
  }

  async importData() {
    revalidateForm(this.downloadForm);

    try {
      const { repo, subset } = this.downloadForm.value;
      this.downloadLoading.set(true);
      let task = {
        id: null,
        name: 'hf-import-'+Date.now(),
        type: this.taskType(),
        project: this.user().defaultProject,
        provisioningStatus: TaskRunProvisioningStatus.CREATED,
        params: [
          { key: "hf-repo-id", value: repo.name },
          { key: "hf-subset", value: subset || ''}
        ]
      } as ITaskRun;

      await lastValueFrom(this.taskRunService.submit(task));
      this.taskSnapshotTime.set(Date.now());
      this.downloadDialogVisible.set(false);
      this.store.dispatch([new LoadRepositoriesAction()]);
      displaySuccess(this.store, 'Job submitted successfully. Your data will show up in the new repository once the operation is complete.');
    } catch (e) {
      displayError(this.store, e);
    } finally {
      this.downloadLoading.set(false);
    }
  }

  async searchHf(event: AutoCompleteCompleteEvent) {
    const token = this.downloadForm.value?.token;

    try {
      if (this.displayRepoType() === LakeFsRepositoryType.MODEL) {
        const filtered = await this.hfSearch('models', event.query, 10, token);
        filtered.sort((a, b) => (b.downloads ?? 0) - (a.downloads ?? 0));
        this.hfSuggestions = filtered;
      } else if (this.displayRepoType() === LakeFsRepositoryType.DATASET) {
        const filtered = await this.hfSearch('datasets', event.query, 10, token);
        filtered.sort((a, b) => (b.downloads ?? 0) - (a.downloads ?? 0));
        this.hfSuggestions = filtered;
      }
    } catch (e) {
      // Optional: surface a nice UI error instead of just console
      displayError(this.store, e);
      this.hfSuggestions = [];
    }
  }

  private async hfSearch(
    kind: 'models' | 'datasets',
    query: string,
    limit = 10,
    token?: string | null
  ): Promise<any[]> {
    const params = new URLSearchParams();
    params.set('search', query);
    params.set('limit', String(limit));
    // These are commonly supported on the Hub listing endpoints; harmless if ignored:
    params.set('sort', 'downloads');
    params.set('direction', '-1');
    params.set('full', 'true');

    const url = `https://huggingface.co/api/${kind}?${params.toString()}`;

    const headers: Record<string, string> = {};
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const res = await fetch(url, { headers });
    if (!res.ok) {
      const text = await res.text().catch(() => '');
      throw new Error(`HF ${kind} search failed: ${res.status} ${res.statusText} ${text}`);
    }

    const items = (await res.json()) as any[];
    return items.map((x) => ({
      ...x,
      name: x.id,
      author: x.author ?? x.id?.split('/')?.[0],
      createdAt: x.createdAt ?? x.lastModified ?? x.last_modified,
      downloads: x.downloads ?? 0,
    }));
  }

  readonly repoTypes: any[] = [{ label: 'Models', value: LakeFsRepositoryType.MODEL }, { label: 'Datasets',value: LakeFsRepositoryType.DATASET}];
  readonly repoTypes2: any[] = [{ label: 'Model', value: LakeFsRepositoryType.MODEL }, { label: 'Dataset', value: LakeFsRepositoryType.DATASET }];
  readonly downloadSources: any[] = [{ label: 'HuggingFace', value: LakeFsImportSource.HF }, { label: 'ModelScope', value: LakeFsImportSource.MODELSCOPE, disabled:true }];

  protected readonly Plus = Plus;
  protected readonly Bot = Bot;
  protected readonly ApplicationMode = ApplicationMode;
  protected readonly dayjs = dayjs;
  protected readonly CloudDownload = CloudDownload;
  protected readonly Info = Info;
  protected readonly Download = Download;
  protected readonly CalendarArrowUp = CalendarArrowUp;
  protected readonly Building2 = Building2;
  protected readonly Lock = Lock;
  protected readonly LakeFsRepositoryType = LakeFsRepositoryType;
  protected readonly TaskRunProvisioningStatus = TaskRunProvisioningStatus;
  protected readonly TaskRunType = TaskRunType;
  protected readonly ExecutorType = ExecutorType;
}
