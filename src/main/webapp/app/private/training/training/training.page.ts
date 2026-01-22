import {Component, computed, inject, OnInit, signal} from '@angular/core';
import {PageComponent} from '../../../shared/components/page/page.component';
import {PageLoadComponent} from '../../../shared/components/page-load/page-load.component';
import {CardModule} from 'primeng/card';
import {ButtonDirective} from 'primeng/button';
import {CardComponent} from '../../../shared/components/card/card.component';
import {CheckboxModule} from 'primeng/checkbox';
import {InputNumberModule} from 'primeng/inputnumber';
import {
  ArrowRight,
  ClipboardList,
  Cpu,
  Database,
  Flame,
  InfoIcon,
  LucideAngularModule,
  Plug,
  Plus,
  Save,
  SlidersHorizontal,
  Trash
} from 'lucide-angular';
import {TableModule} from 'primeng/table';
import {TagModule} from 'primeng/tag';
import {TrainingRecipeComponent} from '../components/training-recipe/training-recipe.component';
import {LayoutService} from '../../../shared/service/theme/app-layout.service';
import {InputTextModule} from 'primeng/inputtext';
import {InputTextareaModule} from 'primeng/inputtextarea';
import {NgIf} from '@angular/common';
import {FormArray, FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators} from '@angular/forms';
import {DropdownModule} from 'primeng/dropdown';
import {LabelTooltipComponent} from '../../../shared/components/label-tooltip/label-tooltip.component';
import {
  BASE_MODEL_REPOSITORY,
  GPUS_PER_WORKER,
  HEAD_GPUS,
  LORA,
  LORA_ALPHA,
  LORA_DROPOUT,
  LORA_MERGE_ITERATIVELY,
  LORA_RANK,
  LORA_TARGET_MODULES,
  MERGE_LORA,
  NUM_NODES,
  QLORA_BNB,
  QLORA_FP4,
  QLORA_FP8,
  RAY_CLUSTER_SHAPE,
  RECOMPUTE_LORA,
  TEST_VLLM_TP,
  USE_HEAD_AS_WORKER
} from '../tooltips';
import {MessagesModule} from 'primeng/messages';
import {RayJobService} from "../../../shared/service/ray-job.service";
import {derivedAsync} from "ngxtension/derived-async";
import {catchError, tap} from "rxjs/operators";
import {displayError, displayErrorAndRethrow} from "../../../shared/util/error.util";
import {Store} from "@ngxs/store";
import {IRayJob} from "../../../shared/model/ray-job.model";
import {EMPTY, lastValueFrom, of} from "rxjs";
import {revalidateForm} from "../../../shared/util/form.util";
import {RayJobType} from "../../../shared/model/enum/ray-job-type.model";
import {RayJobProvisioningStatus} from "../../../shared/model/enum/ray-job-provisioning-status.model";
import {AccountService} from "../../../shared/service/account.service";
import {IProject} from "../../../shared/model/project.model";
import {IJobEnvironmentVariable} from "../../../shared/model/job-environment-variable.model";
import {displaySuccess} from "../../../shared/util/success.util";
import {Router} from "@angular/router";
import {RefSelection, RefSelectorComponent} from "../../hub/components/ref-selector.component";
import {RepoSelectorComponent} from "../../../shared/components/repo-selector/repo-selector.component";
import {ObjectSelectorComponent} from "../../../shared/components/object-selector/object-selector.component";
import {
  DatasetTableChooserComponent,
  newDatasetForm
} from "../../../shared/components/dataset-table-chooser/dataset-table-chooser.component";
import {v4 as uuidv4} from 'uuid';
import {IDatasetConfig, ITrainingConfig} from "../../../shared/model/training-config.model";
import {doubleValidator, filterDouble, sciNumberValidator} from "../../../shared/util/validators.util";
import {
  TrainingOptimizerType,
  TrainingPrecisionType,
  TrainingSchedulerType
} from "../../../shared/model/training.model";
import {IRayClusterShape} from "../../../shared/model/ray-cluster-shape.model";
import {TabViewModule} from "primeng/tabview";
import {injectParams} from "ngxtension/inject-params";

@Component({
  standalone: true,
  selector: 'sm-training-page',
  templateUrl: './training.page.html',
  imports: [
    PageComponent,
    PageLoadComponent,
    CardModule,
    ButtonDirective,
    CardComponent,
    CheckboxModule,
    InputNumberModule,
    LucideAngularModule,
    TableModule,
    TagModule,
    TrainingRecipeComponent,
    InputTextModule,
    InputTextareaModule,
    NgIf,
    FormsModule,
    DropdownModule,
    LabelTooltipComponent,
    MessagesModule,
    ReactiveFormsModule,
    RefSelectorComponent,
    RepoSelectorComponent,
    ObjectSelectorComponent,
    DatasetTableChooserComponent,
    TabViewModule
  ],
  styles: [`
    ::ng-deep .p-datatable .p-datatable-thead > tr > th {
      background-color: var(--surface-50);
    }
    ::ng-deep p-checkbox .p-checkbox-label {
      font-weight: 400;
    }
  `]
})
export class TrainingPage implements OnInit {
  type = injectParams('type');
  jobId = injectParams('id');

  readonly welcomeItems = [
    { title: 'What is Surogate ?', link: 'https://surogate.ai/' },
    { title: 'Train models', link: 'https://surogate.ai/' },
    { title: 'Fine-Tune models', link: 'https://surogate.ai/' }
  ];

  readonly layoutService = inject(LayoutService);
  readonly rayJobService = inject(RayJobService);
  readonly store = inject(Store);
  readonly accountService = inject(AccountService);
  readonly router = inject(Router);
  jobForm: FormGroup;

  createForm() {
    this.jobForm = new FormGroup({
      id: new FormControl<string | null>(null),
      name: new FormControl<string>(null, [Validators.required, Validators.minLength(3), Validators.maxLength(50)]),
      description: new FormControl<string>(null, []),
      baseModelRepo: new FormControl<string>(null, [Validators.required]),
      baseModelBranch: new FormControl<RefSelection>(null, [Validators.required]),
      branch: new FormControl<string>(null, [Validators.required]),
      lora: new FormControl<boolean>(true),
      qloraFp8: new FormControl<boolean>(false),
      qloraFp4: new FormControl<boolean>(true),
      qloraBnb: new FormControl<boolean>(true),
      recomputeLora: new FormControl<boolean>(false),
      mergeLora: new FormControl<boolean>(false),
      mergeIteratively: new FormControl<boolean>(false),
      loraR: new FormControl<number>(32, [Validators.required]),
      loraAlpha: new FormControl<number>(64, [Validators.required]),
      loraDropout: new FormControl<number>(0.0, [Validators.required, doubleValidator]),
      loraTargetModules: new FormControl<string[]>([], []),
      qProj: new FormControl<boolean>(true),
      kProj: new FormControl<boolean>(true),
      vProj: new FormControl<boolean>(true),
      oProj: new FormControl<boolean>(true),
      upProj: new FormControl<boolean>(true),
      downProj: new FormControl<boolean>(true),
      gateProj: new FormControl<boolean>(true),
      fromCheckpoint: new FormControl<boolean>(false),
      numNodes: new FormControl<number>(1, [Validators.required]),
      gpusPerWorker: new FormControl<number>(2, [Validators.required]),
      headGpus: new FormControl<number>(2, [Validators.required]),
      useHeadAsWorker: new FormControl<boolean>(false, []),
      testVllmTp: new FormControl<number>(2, [Validators.required]),
      datasets: new FormArray<FormGroup>([], []),
      testDatasets: new FormArray<FormGroup>([], []),
      type: new FormControl<keyof typeof RayJobType>(null, []),
      provisioningStatus: new FormControl<keyof typeof RayJobProvisioningStatus>(null, []),
      deployedNamespace: new FormControl<string | null>(null),
      chatHostName: new FormControl<string | null>(null),
      workDirVolumeName: new FormControl<string | null>(null),
      podName: new FormControl<string | null>(null),
      container: new FormControl<string | null>(null),
      internalName: new FormControl<string | null>(null),
      project: new FormControl<IProject | null>(null),
      trainingConfigPojo: new FormControl<ITrainingConfig | null>(null),
      rayClusterShapePojo: new FormControl<IRayClusterShape | null>(null),
      envVars: new FormControl<IJobEnvironmentVariable[] | null>(null),
      trainingForm: new FormGroup({
        numEpochs: new FormControl<number>(1, []),
        microBatchSize: new FormControl<number>(2, [Validators.required]),
        gradientAccumulationSteps: new FormControl<number>(2, [Validators.required]),
        learningRate: new FormControl<string>('2e-4', [Validators.required, sciNumberValidator]),
        sequenceLength: new FormControl<number>(1024, [Validators.required]),
        optimizer: new FormControl<string>(TrainingOptimizerType.AdamW, [Validators.required]),
        trainOnInputs: new FormControl<boolean>(true),
        weightDecay: new FormControl<number>(0.0, [doubleValidator]),
        maxGradNorm: new FormControl<number>(0.0, [doubleValidator]),
        valSetSize: new FormControl<number>(0.05, [doubleValidator]),
        evalSteps: new FormControl<number>(10, []),
        loggingSteps: new FormControl<number>(10, []),
        maxSteps: new FormControl<number>(null, []),
        samplePacking: new FormControl<boolean>(false),
        lrScheduler: new FormControl<string>(TrainingSchedulerType.Cosine, [Validators.required]),
        warmupSteps: new FormControl<number>(0, []),
        warmupRatio: new FormControl<number>(0.0, [doubleValidator]),
        cooldownSteps: new FormControl<number>(0, []),
        finalLrFraction: new FormControl<number>(0.0, [doubleValidator]),
        gradientCheckpointing: new FormControl<boolean>(false),
        skipQuantFirstLayers: new FormControl<number>(null, []),
        skipQuantLastLayers: new FormControl<number>(null, []),
        debugTimeBreakdown: new FormControl<boolean>(null),
        debugMemoryBreakdown: new FormControl<boolean>(null),
        recipe: new FormControl<string>(TrainingPrecisionType.BF16, [Validators.required]),
        zeroLevel: new FormControl<number>(1, [])
      })
    });
  }

  get datasets(): FormArray {
    return this.jobForm.get('datasets') as FormArray;
  }

  get testDatasets(): FormArray {
    return this.jobForm.get('testDatasets') as FormArray;
  }

  get trainingForm(): FormGroup {
    return this.jobForm.get('trainingForm') as FormGroup;
  }

  get mergeLora(): boolean {
    return this.jobForm.get('mergeLora')?.value;
  }

  isSaving = false;
  isLaunching = false;
  advanced = signal(false);
  user = derivedAsync(() => this.accountService.identity(true));
  job = derivedAsync(() => {
    if (!this.user() || !this.type()) {
      return EMPTY;
    }

    this.createForm();
    if (this.jobId()) {
      return this.rayJobService.find(this.jobId())
        .pipe(
          catchError((e) => displayErrorAndRethrow(this.store, e)),
          tap(job => this.initForm(job))
        )
    }

    return of({
      type: this.type() === 'pretrain' ? RayJobType.TRAIN : RayJobType.FINE_TUNE,
      provisioningStatus: RayJobProvisioningStatus.CREATED,
      project: this.user().defaultProject
    } as IRayJob);
  });
  provisioningStatus = computed(() => {
    return this.job()?.provisioningStatus ?? null;
  });
  mustRelaunch = computed(() => {
    return this.provisioningStatus() === RayJobProvisioningStatus.DEPLOYED ||
      this.provisioningStatus() === RayJobProvisioningStatus.CANCELLED ||
      this.provisioningStatus() === RayJobProvisioningStatus.ERROR ||
      this.provisioningStatus() === RayJobProvisioningStatus.COMPLETED;
  });
  title = computed(() => {
    switch (this.type()) {
      case 'pretrain':
        return 'Training';
      default:
        return 'Fine-Tuning';
    }
  });
  subtitle = computed(() => {
    switch (this.type()) {
      case 'pretrain':
        return 'Create a base model by training it on large datasets of raw text.';
      default:
        return 'Adapt a base checkpoint to your task using labeled data. Supports Full Fine-Tuning, LoRA/QLoRA and Pre-training continuation.';
    }
  });
  dsType = computed(() => {
    switch (this.type()) {
      case 'pretrain':
        return 'pretrain';
      default:
        return 'sft';
    }
  });

  ngOnInit() {
    this.layoutService.state.helpItems = this.welcomeItems;
  }

  initForm(job: IRayJob) {
    this.jobForm.patchValue(job);
    this.envVarsToForm(job.envVars);
    this.trainingConfigToForm(job.trainingConfigPojo);
    this.rayClusterShapeToForm(job.rayClusterShapePojo);
    revalidateForm(this.jobForm);
  }

  envVarsToForm(envVars: IJobEnvironmentVariable[] = []) {
    const baseModel = envVars.find(ev => ev.key === 'BASE_MODEL')?.value || "/";
    const branch = envVars.find(ev => ev.key === 'BRANCH')?.value || "";
    const mergeLora = !!(envVars.find(ev => ev.key === 'MERGE_LORA')?.value === 'true' || false);
    const mergeIteratively = !!(envVars.find(ev => ev.key === 'MERGE_ITERATIVELY')?.value === 'true' || false);

    this.jobForm.patchValue({
      baseModelRepo: baseModel.split('/')[0],
      baseModelBranch: {
        id: baseModel.split('/')[1],
        type: 'branch'
      } as RefSelection,
      branch, mergeLora, mergeIteratively
    });
  }

  trainingConfigToForm(trainingConfig: ITrainingConfig) {
    this.jobForm.patchValue({
      lora: trainingConfig.lora,
      mergeLora: trainingConfig.mergeLora,
      loraR: trainingConfig.loraR,
      loraAlpha: trainingConfig.loraAlpha,
      loraDropout: trainingConfig.loraDropout,
      qProj: trainingConfig.loraTargetModules?.indexOf('q_proj') >= 0,
      kProj: trainingConfig.loraTargetModules?.indexOf('k_proj') >= 0,
      vProj: trainingConfig.loraTargetModules?.indexOf('v_proj') >= 0,
      oProj: trainingConfig.loraTargetModules?.indexOf('o_proj') >= 0,
      upProj: trainingConfig.loraTargetModules?.indexOf('up_proj') >= 0,
      downProj: trainingConfig.loraTargetModules?.indexOf('down_proj') >= 0,
      gateProj: trainingConfig.loraTargetModules?.indexOf('gate_proj') >= 0,
      qloraFp8: trainingConfig.qloraFp8,
      qloraFp4: trainingConfig.qloraFp4,
      qloraBnb: trainingConfig.qloraBnb,
      recomputeLora: trainingConfig.recomputeLora,
      trainingForm: {
        numEpochs: trainingConfig.numEpochs,
        microBatchSize: trainingConfig.microBatchSize,
        gradientAccumulationSteps: trainingConfig.gradientAccumulationSteps,
        learningRate: trainingConfig.learningRate,
        maxSteps: trainingConfig.maxSteps,
        sequenceLength: trainingConfig.sequenceLen,
        optimizer: trainingConfig.optimizer,
        trainOnInputs: trainingConfig.trainOnInputs,
        weightDecay: trainingConfig.weightDecay,
        maxGradNorm: trainingConfig.maxGradNorm,
        valSetSize: trainingConfig.valSetSize,
        evalSteps: trainingConfig.evalSteps,
        loggingSteps: trainingConfig.loggingSteps,
        samplePacking: trainingConfig.samplePacking,
        lrScheduler: trainingConfig.lrScheduler,
        warmupSteps: trainingConfig.warmupSteps,
        warmupRatio: trainingConfig.warmupRatio,
        cooldownSteps: trainingConfig.cooldownSteps,
        finalLrFraction: trainingConfig.finalLrFraction,
        gradientCheckpointing: trainingConfig.gradientCheckpointing,
        skipQuantFirstLayers: trainingConfig.skipQuantFirstLayers,
        skipQuantLastLayers: trainingConfig.skipQuantLastLayers,
        debugTimeBreakdown: trainingConfig.debugTimeBreakdown,
        debugMemoryBreakdown: trainingConfig.debugMemoryBreakdown,
        recipe: trainingConfig.recipe,
        zeroLevel: trainingConfig.zeroLevel
      }
    });

    if (trainingConfig.datasets) {
      trainingConfig.datasets.forEach(dataset => {
        const dsForm = newDatasetForm(uuidv4(), dataset.type);
        dsForm.patchValue({
          ...dataset,
          ref: {id: dataset.ref, type: 'branch'} as RefSelection,
          messagePropertyMappingsRole: dataset.messagePropertyMappings?.role,
          messagePropertyMappingsContent: dataset.messagePropertyMappings?.content
        })
        this.datasets.push(dsForm);
      });
    }
    if (trainingConfig.testDatasets) {
      trainingConfig.testDatasets.forEach(dataset => {
        const dsForm = newDatasetForm(uuidv4(), dataset.type);
        dsForm.patchValue({
          ...dataset,
          ref: {id: dataset.ref, type: 'branch'} as RefSelection,
          messagePropertyMappingsRole: dataset.messagePropertyMappings?.role,
          messagePropertyMappingsContent: dataset.messagePropertyMappings?.content
        })
        this.testDatasets.push(dsForm);
      });
    }
  }

  rayClusterShapeToForm(rayClusterShape: IRayClusterShape) {
    this.jobForm.patchValue({
      numNodes: rayClusterShape.numNodes,
      gpusPerWorker: rayClusterShape.gpusPerWorker,
      headGpus: rayClusterShape.headGpus,
      useHeadAsWorker: rayClusterShape.useHeadAsWorker,
      testVllmTp: rayClusterShape.testVllmTp
    });
  }

  formToEnvVars(formValues: any): IJobEnvironmentVariable[] {
    const envVars: IJobEnvironmentVariable[] = [];
    const { baseModelRepo, baseModelBranch, branch, datasets, testDatasets, mergeLora, mergeIteratively } = formValues;

    envVars.push({id: null, key: 'BASE_MODEL', value: `${baseModelRepo}/${baseModelBranch.id}`});
    envVars.push({id: null, key: 'BRANCH', value: branch ?? null});
    envVars.push({id: null, key: 'MERGE_LORA', value: mergeLora ?? null});
    envVars.push({id: null, key: 'MERGE_ITERATIVELY', value: mergeIteratively ?? null});

    if (datasets?.length || testDatasets?.length) {
      const dsts = [...datasets];
      if (testDatasets) {
        dsts.push(...testDatasets);
      }
      let datasetString = '';
      dsts.forEach((ds: any) => {
        if (datasetString) {
          datasetString += ','
        }
        datasetString += `${ds.repoId}/${ds.ref.id}`;
      })
      envVars.push({id: null, key: 'DATASET', value: datasetString});
    }

    return envVars;
  }

  formToTrainingConfig(formValues: any): ITrainingConfig {
    const { lora, loraR, loraAlpha, loraDropout, qProj, kProj, vProj, oProj, upProj, downProj, gateProj,
      qloraFp8, qloraFp4, qloraBnb, recomputeLora, mergeLora, datasets, testDatasets, trainingForm } = formValues;
    const loraTargetModules: string[] = [];
    if (qProj) { loraTargetModules.push('q_proj'); }
    if (kProj) { loraTargetModules.push('k_proj'); }
    if (vProj) { loraTargetModules.push('v_proj'); }
    if (oProj) { loraTargetModules.push('o_proj'); }
    if (upProj) { loraTargetModules.push('up_proj'); }
    if (downProj) { loraTargetModules.push('down_proj'); }
    if (gateProj) { loraTargetModules.push('gate_proj'); }

    const dsets: IDatasetConfig[] = [];
    datasets.forEach((ds: IDatasetConfig) => dsets.push({
      ...ds,
      ref: (ds.ref as any).id,
      messagePropertyMappings: {
        role: (ds as any).messagePropertyMappingsRole,
        content: (ds as any).messagePropertyMappingsContent
      }
    }));
    const tdsets: IDatasetConfig[] = [];
    testDatasets.forEach((ds: IDatasetConfig) => tdsets.push({
      ...ds,
      ref: (ds.ref as any).id,
      messagePropertyMappings: {
        role: (ds as any).messagePropertyMappingsRole,
        content: (ds as any).messagePropertyMappingsContent
      }
    }));

    return {
      lora, loraR, loraAlpha, loraDropout, loraTargetModules, qloraFp8, qloraFp4, qloraBnb, recomputeLora, mergeLora,
      datasets: dsets,
      testDatasets: tdsets.length ? tdsets : null,
      numEpochs: trainingForm.numEpochs,
      microBatchSize: trainingForm.microBatchSize,
      gradientAccumulationSteps: trainingForm.gradientAccumulationSteps,
      learningRate: trainingForm.learningRate,
      maxSteps: trainingForm.maxSteps,
      sequenceLen: trainingForm.sequenceLength,
      optimizer: trainingForm.optimizer,
      trainOnInputs: trainingForm.trainOnInputs,
      weightDecay: trainingForm.weightDecay,
      maxGradNorm: trainingForm.maxGradNorm,
      valSetSize: trainingForm.valSetSize,
      evalSteps: trainingForm.evalSteps,
      loggingSteps: trainingForm.loggingSteps,
      samplePacking: trainingForm.samplePacking,
      lrScheduler: trainingForm.lrScheduler,
      warmupSteps: trainingForm.warmupSteps,
      warmupRatio: trainingForm.warmupRatio,
      cooldownSteps: trainingForm.cooldownSteps,
      finalLrFraction: trainingForm.finalLrFraction,
      gradientCheckpointing: trainingForm.gradientCheckpointing,
      skipQuantFirstLayers: trainingForm.skipQuantFirstLayers,
      skipQuantLastLayers: trainingForm.skipQuantLastLayers,
      debugTimeBreakdown: trainingForm.debugTimeBreakdown,
      debugMemoryBreakdown: trainingForm.debugMemoryBreakdown,
      recipe: trainingForm.recipe,
      zeroLevel: trainingForm.zeroLevel
    };
  }

  formToRayClusterShape(formValues: any): IRayClusterShape {
    const { numNodes, gpusPerWorker, headGpus, useHeadAsWorker, testVllmTp } = formValues;
    return { numNodes, gpusPerWorker, headGpus, useHeadAsWorker, testVllmTp };
  }

  async save(launch?: boolean): Promise<IRayJob> {
    revalidateForm(this.jobForm);
    if (this.jobForm.invalid) {
      displayError(this.store, 'Please fill in all required fields');
      return Promise.reject();
    }
    if (!this.crossValidate()) {
      return Promise.reject();
    }

    const job = this.jobForm.getRawValue();
    const existing = this.job();
    try {
      this.isSaving = true;
      // Re-add attributes not present in the form
      job.type = existing.type;
      job.provisioningStatus = existing.provisioningStatus;
      job.deployedNamespace = existing.deployedNamespace;
      job.podName = existing.podName;
      job.container = existing.container;
      job.project = existing.project;
      job.internalName = existing.internalName;

      job.trainingConfigPojo = this.formToTrainingConfig(job);
      job.rayClusterShapePojo = this.formToRayClusterShape(job);
      job.envVars = this.formToEnvVars(job);

      const saved = await lastValueFrom(this.rayJobService.save(job));

      if (launch) {
        this.isSaving = false;
        this.isLaunching = true;
        try {
          await lastValueFrom(
            launch && this.mustRelaunch() ?
              this.rayJobService.redeploy(saved) :
              this.rayJobService.deploy(saved)
          );
          displaySuccess(this.store, 'Job launched successfully');
        } catch (err) {
          console.log(err);
        }
      } else {
        displaySuccess(this.store, 'Job saved successfully');
      }
      await this.router.navigate([`/train/jobs/ray/${this.type() === 'pretrain' ? 'training' : 'fine-tuning'}`]);
      return Promise.resolve(job);
    } catch (e) {
      displayError(this.store, e);
    } finally {
      this.isSaving = false;
      this.isLaunching = false;
    }

    return Promise.reject();
  }

  crossValidate(): boolean {
    const { lora, qProj, kProj, vProj, oProj, upProj, downProj, gateProj, datasets, trainingForm } = this.jobForm.getRawValue();
    if (!trainingForm.numEpochs && !trainingForm.maxSteps) {
      displayError(this.store, 'Please set at least one of "Number of epochs" or "Max steps".');
      return false;
    }
    if (lora && !qProj && !kProj && !vProj && !oProj && !upProj && !downProj && !gateProj) {
      displayError(this.store, 'Please check at least one lora target module.');
      return false;
    }
    if (!datasets?.length) {
      displayError(this.store, 'Please add at least one training dataset.');
      return false;
    }
    return true;
  }

  protected readonly Database = Database;
  protected readonly Plus = Plus;
  protected readonly Trash = Trash;
  protected readonly Flame = Flame;
  protected readonly Plug = Plug;
  protected readonly ClipboardList = ClipboardList;

  protected readonly InfoIcon = InfoIcon;
  protected readonly ArrowRight = ArrowRight;
  protected readonly Save = Save;
  protected readonly Cpu = Cpu;

  protected readonly BASE_MODEL_REPOSITORY = BASE_MODEL_REPOSITORY;
  protected readonly LORA = LORA;
  protected readonly LORA_RANK = LORA_RANK;
  protected readonly LORA_ALPHA = LORA_ALPHA;
  protected readonly LORA_DROPOUT = LORA_DROPOUT;
  protected readonly LORA_TARGET_MODULES = LORA_TARGET_MODULES;
  protected readonly QLORA_FP8 = QLORA_FP8;
  protected readonly QLORA_FP4 = QLORA_FP4;
  protected readonly QLORA_BNB = QLORA_BNB;
  protected readonly RECOMPUTE_LORA = RECOMPUTE_LORA;
  protected readonly MERGE_LORA = MERGE_LORA;
  protected readonly LORA_MERGE_ITERATIVELY = LORA_MERGE_ITERATIVELY;
  protected readonly NUM_NODES = NUM_NODES;
  protected readonly GPUS_PER_WORKER = GPUS_PER_WORKER;
  protected readonly HEAD_GPUS = HEAD_GPUS;
  protected readonly USE_HEAD_AS_WORKER = USE_HEAD_AS_WORKER;
  protected readonly TEST_VLLM_TP = TEST_VLLM_TP;

  protected readonly RayJobProvisioningStatus = RayJobProvisioningStatus;
  protected readonly RAY_CLUSTER_SHAPE = RAY_CLUSTER_SHAPE;
  protected readonly SlidersHorizontal = SlidersHorizontal;

  protected readonly filterDouble = filterDouble;
}
