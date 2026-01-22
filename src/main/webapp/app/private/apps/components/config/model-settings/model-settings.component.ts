import {Component, OnDestroy, OnInit} from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormGroup,
  FormsModule,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ReactiveFormsModule,
  ValidationErrors,
  Validator
} from '@angular/forms';
import {CommonModule} from '@angular/common';

import {InputNumberModule} from 'primeng/inputnumber';
import {CheckboxModule} from 'primeng/checkbox';
import {DropdownModule} from 'primeng/dropdown';
import {SliderModule} from 'primeng/slider';
import {InputTextModule} from 'primeng/inputtext';
import {IModelConfig} from '../../../../../shared/model/model-settings';
import {ApplicationFormService} from '../../../../../shared/service/form/application-form.service';
import {RoutingStrategy} from '../../../../../shared/model/enum/routing-strategy.model';
import {DeploymentMode} from '../../../../../shared/model/enum/deployment-mode.model';
import {takeUntil} from 'rxjs/operators';
import {lastValueFrom, Subject} from 'rxjs';
import {Store} from '@ngxs/store';
import {Selectors} from '../../../../../shared/state/selectors';
import {INode} from '../../../../../shared/model/node.model';
import {NodeStatusService} from '../../../../../shared/service/k8s/node-status.service';
import {NodeStats} from '../../../../../shared/model/k8s/node-stats.model';
import {availableGpuMemoryFromNodeStats, estimatePerformance, GPUType} from '../../../../../shared/util/gpu.util';
import {MessagesModule} from 'primeng/messages';
import {MessageModule} from 'primeng/message';
import {roundUpTo} from '../../../../../shared/util/display.util';
import {InputGroupModule} from 'primeng/inputgroup';
import {ButtonDirective} from 'primeng/button';
import {displayError} from '../../../../../shared/util/error.util';
import {GpuCardsComponent} from '../../../../nodes/components/gpu-cards/gpu-cards.component';
import {CardModule} from 'primeng/card';
import {TagModule} from "primeng/tag";

@Component({
  selector: 'sm-model-settings',
  standalone: true,
  templateUrl: './model-settings.component.html',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    InputNumberModule,
    CheckboxModule,
    DropdownModule,
    SliderModule,
    InputTextModule,
    FormsModule,
    MessagesModule,
    MessageModule,
    InputGroupModule,
    ButtonDirective,
    GpuCardsComponent,
    CardModule,
    TagModule
  ],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      multi: true,
      useExisting: ModelSettingsComponent
    },
    {
      provide: NG_VALIDATORS,
      multi: true,
      useExisting: ModelSettingsComponent
    },
  ]
})
export class ModelSettingsComponent implements ControlValueAccessor, Validator, OnInit, OnDestroy {
  readonly NotEnoughMemoryWarning =
    {
      severity: 'warn',
      detail: 'The selected configuration may not fit into the GPU memory. Consider enabling partitioning, disabling replication or reducing the context size.'
    };

  modelSettings: IModelConfig;
  onChange = (_: IModelConfig) => {};
  onTouched: Function = () => {};
  disabled = false;
  form: FormGroup;
  destroy$ = new Subject<void>();

  // node status
  nodes: INode[] = [];
  nodeStats: NodeStats[] = [];
  statusLoading = true;
  modelPerformance: any;
  maxPartitions = 1;

  constructor(formService: ApplicationFormService,
              private store: Store,
              private nodeStatusService: NodeStatusService) {
    this.form = formService.createModelSettingsForm();
    this.form.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(value => {
      this.onChange(value);

      // ensure l1Cache is enabled if l2Cache is enabled
      if (value.l2Cache && !value.l1Cache) {
        this.form.controls['l1Cache'].setValue(true);
      }

      if (!this.statusLoading) {
        this.computeModelPerformance(value);
      }
    });
  }

  ngOnInit() {
    this.store.select(Selectors.nodes)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: async (nodes) => {
          this.nodes = [...nodes];
          this.nodes.sort((a, b) => a.cluster.zone.zoneId.localeCompare(b.cluster.zone.zoneId));
          await this.fetchNodeStatus();
        },
        error: () => {}
      });
  }

  // called by the Forms module to write a value into a form control
  writeValue(value: IModelConfig): void {
    value && this.form.patchValue({ ...value });
  }

  // When a form value changes due to user input, we need to report the value back to the parent form.
  // This is done by calling the fn callback
  registerOnChange(fn: any): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }

  // form controls can be enabled and disabled using the Forms API.
  // This state can be transmitted to the form control via the setDisabledState method
  setDisabledState?(isDisabled: boolean): void {
    this.disabled = isDisabled;
    isDisabled ? this.form.disable() : this.form.enable();
  }

  validate(control: AbstractControl): ValidationErrors | null {
    if (this.form.valid) {
      return null;
    }
    let errors : any = {};
    Object.keys(this.form.controls).forEach(control => {
      const myErrors = this.form.controls[control].errors;
      if (myErrors) {
        errors[control] = myErrors;
      }
    });
    return errors;
  }

  registerOnValidatorChange?(fn: () => void): void {
    // not needed
  }

  async fetchNodeStatus() {
    const clusterCid = this.nodes.length > 0 ? this.nodes[0].cluster.cid : '';
    try {
      this.statusLoading = true;
      this.nodeStats = await lastValueFrom(this.nodeStatusService.statusSnapshot(clusterCid));
      if (!this.modelPerformance) {
        // calculate model performance the first time we receive a status update
        this.computeModelPerformance(this.form.value);
      }
    } catch (e) {
      displayError(this.store, e);
    } finally {
      this.statusLoading = false;
    }
  }

  setRecommendedMemoryValue() {
    let minMemory = roundUpTo(this.modelPerformance.memory.worst_case.perGPU_required_gb * 1024, 0);
    if (this.form.value.enableReplication) {
      minMemory = minMemory * this.form.value.replicas;
    }
    this.form.patchValue({
      gpuMemory: minMemory
    });
  }

  computeModelPerformance(modelConfig: any): void {
    if (!this.nodeStats)
      return;

    const stats = Object.values(this.nodeStats);
    const availableGpuMemory = availableGpuMemoryFromNodeStats(stats[0]);
    const gpuModel = stats[0]?.gpuModel || 'rtx4070ti';
    this.modelPerformance = estimatePerformance(
      modelConfig,
      availableGpuMemory,
      gpuModel as GPUType,
      "fp16/bf16",
      "fp4/int4",
      "fp16/bf16",
      {
        avgOutputLen: 512,
      }
    );

    this.maxPartitions = availableGpuMemory.length;
    if (this.maxPartitions === 1) {
      // disable partitioning if there is only one GPU
      this.form.patchValue({
        enablePartitioning: false,
        partitions: 1
      }, {emitEvent: false});
    } else {
      // reset partitions if it exceeds the max partitions
      if (modelConfig.partitions > this.maxPartitions) {
        this.form.patchValue({
          partitions: this.maxPartitions
        }, {emitEvent: false});
      }
    }
  }

  hasEnoughMemory(): boolean {
    if (this.statusLoading)
      return true;

    const stats = Object.values(this.nodeStats);
    const availableGpuMemory = availableGpuMemoryFromNodeStats(stats[0]);
    const gpuMemory = this.form.value.gpuMemory * 1024 * 1024; // convert to bytes

    if (this.form.value.enablePartitioning) {
      const partitions = this.form.value.partitions;
      for (let i = 1; i <= partitions; i++) {
        if (gpuMemory >= (availableGpuMemory[i] || 0))
          return false;
      }
      return true;
    } else {
      return gpuMemory <= availableGpuMemory[0];
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  modelPerformanceReport(): number[] {
    if (!this.modelPerformance)
      return [0, 0, 0];

    const recommendedMemory = this.modelPerformance.memory.worst_case.perGPU_required_gb;
    const throughput = this.modelPerformance.tokens_per_second.conservative_tps;
    const genTime = this.modelPerformance.tokens_per_second.gen_time_seconds;

    const replicas = this.form.value.enableReplication ? this.form.value.replicas : 1;
    return [
      roundUpTo(recommendedMemory * replicas, 1),
      roundUpTo(throughput * replicas, 0),
      genTime
    ];
  }

  routingStrategies: Array<{ label: string; value: RoutingStrategy }> = [
    {label: 'Round-Robin', value: RoutingStrategy.ROUND_ROBIN},
    {label: 'Session', value: RoutingStrategy.SESSION},
    {label: 'KV-based', value: RoutingStrategy.KV_BASED},
    {label: 'Prefix-based', value: RoutingStrategy.PREFIX_BASED},
  ];

  deploymentModes: Array<{ label: string; value: DeploymentMode }> = [
    {label: 'Aggregated', value: DeploymentMode.AGGREGATED},
    {label: 'Disaggregated', value: DeploymentMode.DISAGGREGATED},
  ];
}
