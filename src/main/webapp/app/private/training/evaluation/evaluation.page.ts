// src/app/features/evaluation/pages/evaluation.page.ts
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { AbstractControl, FormArray, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Store } from '@ngxs/store';
import { derivedAsync } from 'ngxtension/derived-async';
import { injectParams } from 'ngxtension/inject-params';
import { lastValueFrom, of } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';

import { TaskRunService } from '../../../shared/service/task-run.service';
import { LayoutService } from '../../../shared/service/theme/app-layout.service';
import { AccountService } from '../../../shared/service/account.service';
import { ITaskRun } from '../../../shared/model/tasks.model';
import { TaskRunType } from '../../../shared/model/enum/task-run-type.model';
import { TaskRunProvisioningStatus } from '../../../shared/model/enum/task-run-provision-status.model';
import { revalidateForm } from '../../../shared/util/form.util';
import { displayError, displayErrorAndRethrow } from '../../../shared/util/error.util';
import { displaySuccess } from '../../../shared/util/success.util';
import {
  LLMProviderConfig,
  LlmProviderConfigComponent,
} from '../../../shared/components/llm-provider-config/llm-provider-config.component';
import { RefSelection, RefSelectorComponent } from '../../hub/components/ref-selector.component';

// Icons
import {
  ArrowRight,
  Bot,
  Database,
  Flame,
  GalleryVerticalEnd,
  Info,
  LucideAngularModule,
  MessageSquare,
  Save,
  Settings,
  ShieldCheck,
  Sparkles,
  Trash,
} from 'lucide-angular';

// Tooltips
import {
  BASE_RUN_NAME,
  EVAL_CONVERSATION_DATASET,
  EVAL_CUSTOM_CRITERIA,
  EVAL_LANGUAGE,
  EVAL_QUALITY_DATASET,
  EVAL_SHOTS,
  EVAL_TASKS,
} from '../tooltips';
import { EvaluationFormService } from '../../../shared/service/form/evaluation-form.service';
import { LabelTooltipComponent } from '../../../shared/components/label-tooltip/label-tooltip.component';
import { PaginatorModule } from 'primeng/paginator';
import { MultiSelectModule } from 'primeng/multiselect';
import { MessageModule } from 'primeng/message';
import { TableModule } from 'primeng/table';
import { ButtonDirective } from 'primeng/button';
import { CardComponent } from '../../../shared/components/card/card.component';
import { RepoSelectorComponent } from '../../../shared/components/repo-selector/repo-selector.component';
import { CheckboxModule } from 'primeng/checkbox';
import { CommonModule, NgTemplateOutlet } from '@angular/common';
import { TooltipModule } from 'primeng/tooltip';
import { DatasetTableChooserComponent } from '../../../shared/components/dataset-table-chooser/dataset-table-chooser.component';
import { DividerModule } from 'primeng/divider';
import { EvaluationResultsComponent } from '../../../shared/components/evaluation-results/evaluation-results.component';
import { PageLoadComponent } from '../../../shared/components/page-load/page-load.component';
import { PageComponent } from '../../../shared/components/page/page.component';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { InputTextModule } from 'primeng/inputtext';
import { DropdownModule } from 'primeng/dropdown';
import { InputNumberModule } from 'primeng/inputnumber';
import { CONVERSATION_METRICS, QUALITY_METRICS } from './constants/metrics';
import { ATTACKS, SECURITY_TESTS } from './constants/security';
import { BENCHMARKS } from './constants/benchmarks';

@Component({
  standalone: true,
  selector: 'sm-evaluation-page',
  styles: [
    `
      ::ng-deep .p-datatable .p-datatable-thead > tr > th {
        background-color: var(--surface-50);
      }

      ::ng-deep p-checkbox .p-checkbox-label {
        font-weight: 400;
      }
    `,
  ],
  templateUrl: './evaluation.page.html',
  imports: [
    LabelTooltipComponent,
    LucideAngularModule,
    PaginatorModule,
    MultiSelectModule,
    MessageModule,
    TableModule,
    ButtonDirective,
    CardComponent,
    RefSelectorComponent,
    RepoSelectorComponent,
    CheckboxModule,
    NgTemplateOutlet,
    ReactiveFormsModule,
    TooltipModule,
    DatasetTableChooserComponent,
    DividerModule,
    LlmProviderConfigComponent,
    EvaluationResultsComponent,
    PageLoadComponent,
    PageComponent,
    CommonModule,
    InputNumberModule,
    DropdownModule,
    InputTextModule,
    InputTextareaModule,
  ],
})
export class EvaluationPage implements OnInit {
  // Services
  private formService = inject(EvaluationFormService);
  private taskRunService = inject(TaskRunService);
  private layoutService = inject(LayoutService);
  private accountService = inject(AccountService);
  private router = inject(Router);
  private store = inject(Store);

  // Constants
  readonly benchmarks = BENCHMARKS;
  readonly security = SECURITY_TESTS;
  readonly attacks = ATTACKS;
  readonly quality = QUALITY_METRICS;
  readonly conversation = CONVERSATION_METRICS;
  readonly languages = [
    { name: 'Romanian', code: 'ro' },
    { name: 'English', code: 'en' },
  ];

  // Form
  taskForm = this.formService.createForm();

  // State
  taskRunId = injectParams('id');
  isSaving = signal(false);
  isLaunching = signal(false);

  user = derivedAsync(() => this.accountService.identity(true));

  taskRun = derivedAsync(() => {
    if (this.taskRunId()) {
      return this.taskRunService.find(this.taskRunId()).pipe(
        catchError(e => displayErrorAndRethrow(this.store, e)),
        tap(taskRun => this.initForm(taskRun)),
      );
    }
    return of({
      type: TaskRunType.EVALUATION,
      provisioningStatus: TaskRunProvisioningStatus.CREATED,
      project: this.user()?.defaultProject ?? null,
    } as ITaskRun);
  });

  provisioningStatus = computed(() => this.taskRun()?.provisioningStatus ?? null);
  mustRelaunch = computed(() => ['DEPLOYED', 'COMPLETED', 'CANCELLED', 'ERROR'].includes(this.provisioningStatus() as string));

  ngOnInit() {
    this.layoutService.state.helpItems = [
      { title: 'What is Surogate?', link: 'https://surogate.ai/' },
      { title: 'Evaluate models', link: 'https://surogate.ai/' },
    ];
  }

  initForm(taskRun: ITaskRun) {
    this.taskForm.patchValue(taskRun);
    this.formService.envVarsToForm(this.taskForm, taskRun.params);
    revalidateForm(this.taskForm);
  }

  async save(launch?: boolean): Promise<ITaskRun> {
    revalidateForm(this.taskForm);
    if (this.taskForm.invalid) {
      displayError(this.store, 'Please fill in all required fields');
      return Promise.reject();
    }

    const taskRunValue = this.taskForm.getRawValue();
    const existing = this.taskRun();

    try {
      this.isSaving.set(true);
      taskRunValue.params = this.formService.formToParams(taskRunValue);
      taskRunValue.benchmarks = [];
      taskRunValue.project = existing.project;

      const saved = await lastValueFrom(this.taskRunService.submit(taskRunValue));

      if (launch) {
        this.isSaving.set(false);
        this.isLaunching.set(true);
        await lastValueFrom(this.taskRunService.submit(saved));
        displaySuccess(this.store, 'Job launched successfully');
      } else {
        displaySuccess(this.store, 'Job saved successfully');
      }

      await this.router.navigate(['/train/jobs/tekton/evaluation']);
      return taskRunValue;
    } catch (e) {
      displayError(this.store, e);
      return Promise.reject();
    } finally {
      this.isSaving.set(false);
      this.isLaunching.set(false);
    }
  }

  // Form array getters
  get benchmarksArray(): FormArray {
    return this.taskForm.get('benchmarks') as FormArray;
  }
  get performanceMetricsArray(): FormArray {
    return this.taskForm.get('performanceMetrics') as FormArray;
  }
  get qualityMetricsArray(): FormArray {
    return this.taskForm.get('qualityMetrics') as FormArray;
  }
  get conversationMetricsArray(): FormArray {
    return this.taskForm.get('conversationMetrics') as FormArray;
  }
  get securityTestsArray(): FormArray {
    return this.taskForm.get('securityTests') as FormArray;
  }
  get redTeamingConfig(): FormGroup {
    return this.taskForm.get('redTeamingConfig') as FormGroup;
  }
  get customEvalDatasetsArray(): FormArray<FormGroup> {
    return this.taskForm.controls.customEvalDatasets as FormArray<FormGroup>;
  }
  get accuracyBenchmarks(): FormGroup[] {
    return this.benchmarksArray.controls.filter(c => ['accuracy', 'coding'].includes(c.get('type')?.value)) as FormGroup[];
  }

  // Config getters
  get modelUnderTest(): LLMProviderConfig | null {
    return this.taskForm.get('modelUnderTest')?.value ?? null;
  }
  get judgeConfig(): LLMProviderConfig | null {
    return this.taskForm.get('judgeConfig')?.value ?? null;
  }
  get simulatorConfig(): LLMProviderConfig | null {
    return this.taskForm.get('simulatorConfig')?.value ?? null;
  }
  get useSeparateSimulator(): boolean {
    return this.taskForm.get('useSeparateSimulator')?.value ?? false;
  }

  // Add/remove methods
  add(item: any) {
    if (this.benchmarksArray.value.some((b: any) => b.name === item.name)) return;
    const tasks = Array.isArray(item.tasks) ? item.tasks : [{ label: 'All', value: 'All' }];
    this.benchmarksArray.push(
      new FormGroup({
        name: new FormControl(item.name, Validators.required),
        evalScopeName: new FormControl(item.evalScopeName, Validators.required),
        type: new FormControl(item.type),
        tasks: new FormControl(tasks),
        selectedTasks: new FormControl(['All'], Validators.required),
        shots: new FormControl(item.defaultShots ?? 0, [Validators.required, Validators.min(0)]),
        limit: new FormControl(item.defaultLimit || null),
        supportsFewshot: new FormControl(item.supportsFewshot ?? true),
        useCustomDataset: new FormControl<boolean>(false),
        datasetRepo: new FormControl<string | null>(null),
        datasetRef: new FormControl<RefSelection | null>(null),
        datasetSubset: new FormControl<string>('default'),
      }),
    );
  }

  removeBenchmarkByName(name: string) {
    const index = this.benchmarksArray.value.findIndex((b: any) => b.name === name);
    if (index !== -1) this.benchmarksArray.removeAt(index);
  }

  addQualityMetric(item: any) {
    if (this.qualityMetricsArray.value.some((m: any) => m.name === item.name)) return;
    this.qualityMetricsArray.push(
      new FormGroup({
        name: new FormControl(item.name),
        type: new FormControl('quality'),
        datasetRepo: new FormControl<string | null>(null, Validators.required),
        datasetRef: new FormControl<RefSelection | null>(null, Validators.required),
        criteria: new FormControl<string>(''),
        limit: new FormControl<number | null>(null),
      }),
    );
  }

  removeQualityMetric(name: string) {
    const index = this.qualityMetricsArray.value.findIndex((m: any) => m.name === name);
    if (index !== -1) this.qualityMetricsArray.removeAt(index);
  }

  addConversationMetric(item: any) {
    if (this.conversationMetricsArray.value.some((m: any) => m.name === item.name)) return;
    this.conversationMetricsArray.push(
      new FormGroup({
        name: new FormControl(item.name),
        type: new FormControl('conversation'),
        datasetRepo: new FormControl<string | null>(null, Validators.required),
        datasetRef: new FormControl<RefSelection | null>(null, Validators.required),
        hasConfig: new FormControl<boolean>(item.hasConfig || false),
        configValue: new FormControl<number | null>(item.configDefault || null),
        configLabel: new FormControl<string>(item.configLabel || ''),
        configTooltip: new FormControl<string>(item.configTooltip || ''),
        limit: new FormControl<number | null>(null),
      }),
    );
  }

  removeConversationMetric(name: string) {
    const index = this.conversationMetricsArray.value.findIndex((m: any) => m.name === name);
    if (index !== -1) this.conversationMetricsArray.removeAt(index);
  }

  addSecurityTest(item: any) {
    if (this.securityTestsArray.value.some((s: any) => s.name === item.name)) return;
    this.securityTestsArray.push(
      new FormGroup({
        name: new FormControl(item.name),
        evalScopeName: new FormControl(item.evalScopeName),
        category: new FormControl(item.category),
        subtypesOptions: new FormControl(item.subtypes),
        selectedSubtypes: new FormControl(item.subtypes.map((s: any) => s.value)),
        attacks: new FormControl(['prompt_injection', 'roleplay', 'prompt_probing']),
      }),
    );
    if (this.securityTestsArray.length === 1) this.redTeamingConfig.get('enabled')?.setValue(true);
  }

  removeSecurityTest(name: string) {
    const index = this.securityTestsArray.value.findIndex((s: any) => s.name === name);
    if (index !== -1) this.securityTestsArray.removeAt(index);
    if (this.securityTestsArray.length === 0) this.redTeamingConfig.get('enabled')?.setValue(false);
  }

  getBenchmarkMinContext(name: string): number | null {
    return (this.benchmarks.find(b => b.name === name) as any)?.minContextLength || null;
  }

  isInvalid(control: AbstractControl | null): boolean {
    return !!(control && control.invalid && (control.touched || control.dirty));
  }

  // Icons & tooltips
  protected readonly Flame = Flame;
  protected readonly Trash = Trash;
  protected readonly GalleryVerticalEnd = GalleryVerticalEnd;
  protected readonly ShieldCheck = ShieldCheck;
  protected readonly Save = Save;
  protected readonly ArrowRight = ArrowRight;
  protected readonly Sparkles = Sparkles;
  protected readonly MessageSquare = MessageSquare;
  protected readonly Settings = Settings;
  protected readonly Bot = Bot;
  protected readonly Info = Info;
  protected readonly Database = Database;
  protected readonly BASE_RUN_NAME = BASE_RUN_NAME;
  protected readonly EVAL_LANGUAGE = EVAL_LANGUAGE;
  protected readonly EVAL_TASKS = EVAL_TASKS;
  protected readonly EVAL_SHOTS = EVAL_SHOTS;
  protected readonly EVAL_CONVERSATION_DATASET = EVAL_CONVERSATION_DATASET;
  protected readonly EVAL_QUALITY_DATASET = EVAL_QUALITY_DATASET;
  protected readonly EVAL_CUSTOM_CRITERIA = EVAL_CUSTOM_CRITERIA;
}
