import { Injectable } from '@angular/core';
import { FormArray, FormControl, FormGroup, Validators } from '@angular/forms';
import { DEFAULT_VALUES, ResourceType } from '../../model/container.model';
import { IPort } from '../../model/port.model';
import { NewEnvironmentVariable } from '../../model/environment-variable.model';
import { PullImageMode } from '../../model/enum/pull-image-mode.model';
import { IApplication, NewApplication } from '../../model/application.model';
import { ContainerType } from '../../model/enum/container-type.model';
import { IVolume } from '../../model/volume.model';
import { IVolumeMount } from '../../model/volume-mount.model';
import { NewAnnotation } from '../../model/annotation.model';
import { UpdateStrategy } from '../../model/enum/update-strategy.model';
import { SchedulingRule } from '../../model/enum/scheduling-rule.model';
import { NewFirewallEntry } from '../../model/firewall-entry.model';
import { FirewallLevel } from '../../model/enum/firewall-level.model';
import { PolicyType } from '../../model/enum/policy-type.model';
import { RuleType } from '../../model/enum/rule-type.model';
import { ipValidator } from '../../util/form.util';
import { ApplicationMode } from '../../model/enum/application-mode.model';
import { RoutingStrategy } from '../../model/enum/routing-strategy.model';
import { DeploymentMode } from '../../model/enum/deployment-mode.model';
import { IProbe } from '../../model/probe.model';
import { IModelConfig } from '../../model/model-settings';

type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

export interface ContainerFormData {
  id: string | null;
  imageName: string;
  imageTag: string;
  displayName?: string | null;
  type?: keyof typeof ContainerType | null;
  pullImageMode: PullImageMode;
  registryUrl?: string;
  registryUsername?: string;
  registryPassword?: string;
  resourceType: ResourceType;
  cpuRequest?: number | null;
  cpuLimit?: number | null;
  memRequest: string | null;
  memLimit: string | null;
  gpuLimit?: number | 0;
  gpuType?: string | null;
  startParameters?: string | null;
  startCommand?: string | null;
  ports?: IPort[];
  probes?: IProbe[];
  envVars?: NewEnvironmentVariable[];
  firewallEntries?: NewFirewallEntry[];
  volumeMounts?: IVolumeMount[];

  [key: string]: any;
}

type ApplicationFormGroupInput = IApplication | PartialWithRequiredKeyOf<NewApplication>;

type ApplicationFormGroupContent = {
  id: FormControl<IApplication['id'] | NewApplication['id']>;
  name: FormControl<IApplication['name']>;
  internalName: FormControl<IApplication['internalName']>;
  project: FormControl<IApplication['project']>;
  description: FormControl<IApplication['description']>;
  mode: FormControl<ApplicationMode>;
  replicas: FormControl<number>;
  updateStrategy: FormControl<UpdateStrategy | null>;
  schedulingRule: FormControl<SchedulingRule | null>;
  containers: FormArray<FormGroup<ContainerFormGroupContent>>;
  annotations: FormArray<FormGroup>;
  modelSettings: FormControl<IModelConfig | null>;
};

type ContainerFormGroupContent = {
  id: FormControl<string | null>;
  displayName: FormControl<string | null>;
  imageName: FormControl<string>;
  imageTag: FormControl<string>;
  type: FormControl<string | null>;
  pullImageMode: FormControl<PullImageMode>;
  registryUrl: FormControl<string>;
  registryUsername: FormControl<string>;
  registryPassword: FormControl<string>;
  resourceType: FormControl<ResourceType>;
  cpuRequest: FormControl<number | null>;
  cpuLimit: FormControl<number | null>;
  memRequest: FormControl<string | null>;
  memLimit: FormControl<string | null>;
  gpuLimit: FormControl<number | 0>;
  gpuType: FormControl<string | null>;
  startParameters: FormControl<string | null>;
  startCommand: FormControl<string | null>;
  ports: FormArray<FormGroup>;
  probes: FormArray<FormGroup>;
  envVars: FormArray<FormGroup>;
  firewallEntries: FormArray<FormGroup>;
  volumeMounts: FormArray<FormGroup>;
};

export type ApplicationFormGroup = FormGroup<ApplicationFormGroupContent>;

@Injectable({providedIn: 'root'})
export class ApplicationFormService {
  createApplicationForm(application: ApplicationFormGroupInput = {id: null}): ApplicationFormGroup {
    return new FormGroup<ApplicationFormGroupContent>({
      id: new FormControl<string | null>(application.id),
      name: new FormControl('', [Validators.required, Validators.minLength(3), Validators.maxLength(50)]),
      internalName: new FormControl(''),
      project: new FormControl(null, [Validators.required]),
      description: new FormControl(null, [Validators.maxLength(250)]),
      mode: new FormControl(ApplicationMode.APPLICATION, [Validators.required]), // Add this line
      replicas: new FormControl(1, [Validators.required, Validators.min(1), Validators.max(16)]),
      updateStrategy: new FormControl(UpdateStrategy.ROLLING, [Validators.required]),
      schedulingRule: new FormControl(SchedulingRule.DECENTRALIZED, [Validators.required]),
      containers: new FormArray<FormGroup>([]),
      annotations: new FormArray<FormGroup>([]),
      modelSettings: new FormControl<IModelConfig>(null)
    });
  }

  createContainerForm(): FormGroup<ContainerFormGroupContent> {
    return new FormGroup<ContainerFormGroupContent>({
      id: new FormControl<string | null>(null),
      displayName: new FormControl<string | null>(null),
      imageName: new FormControl<string>('', [Validators.required]),
      imageTag: new FormControl<string>('', [Validators.required]),
      type: new FormControl<string | null>(ContainerType.WORKER),
      pullImageMode: new FormControl<PullImageMode>(PullImageMode.PULL),
      registryUrl: new FormControl<string>(''),
      registryUsername: new FormControl<string>(''),
      registryPassword: new FormControl<string>(''),
      resourceType: new FormControl<ResourceType>(ResourceType.CPU),
      cpuRequest: new FormControl<number | null>(DEFAULT_VALUES.CPU.request, [Validators.min(0.001), Validators.max(100)]),
      cpuLimit: new FormControl<number | null>(DEFAULT_VALUES.CPU.limit, [Validators.required, Validators.min(0.001), Validators.max(100)]),
      memRequest: new FormControl<string | null>(DEFAULT_VALUES.MEMORY.request, [Validators.min(256), Validators.max(262144)]),
      memLimit: new FormControl<string | null>(DEFAULT_VALUES.MEMORY.limit, [Validators.required, Validators.min(256), Validators.max(262144)]),
      startCommand: new FormControl<string | null>(null),
      startParameters: new FormControl<string | null>(null),
      gpuType: new FormControl<string | null>(null),
      gpuLimit: new FormControl<number | 0>(0, [Validators.min(0), Validators.max(100)]),
      ports: new FormArray<FormGroup>([]),
      probes: new FormArray<FormGroup>([]),
      envVars: new FormArray<FormGroup>([]),
      firewallEntries: new FormArray<FormGroup>([]),
      volumeMounts: new FormArray<FormGroup>([])
    });
  }

  createPortForm(port: Partial<IPort> = {}): FormGroup {
    return new FormGroup({
      id: new FormControl(port.id || null),
      name: new FormControl(port.name || '', [Validators.required, Validators.maxLength(15)]),
      containerPort: new FormControl(port.containerPort || null, [Validators.required, Validators.min(1), Validators.max(65535)]),
      servicePort: new FormControl(port.servicePort || null, [Validators.min(1), Validators.max(65535)]),
      protocol: new FormControl(port.protocol || null, [Validators.required]),
      containerIndex: new FormControl(port.containerIndex || null),
      ingressPort: new FormControl(port.ingressPort || false),
      ingressHost: new FormControl(port.ingressHost || '')
    });
  }

  createProbeForm(probe: Partial<IProbe> = {}): FormGroup {
    return new FormGroup({
      id: new FormControl(probe.id || null),
      type: new FormControl(probe.type || '', [Validators.required]),
      initialDelaySeconds: new FormControl(probe.initialDelaySeconds || null),
      periodSeconds: new FormControl(probe.periodSeconds || null),
      failureThreshold: new FormControl(probe.failureThreshold || null),
      successThreshold: new FormControl(probe.successThreshold || null),
      timeoutSeconds: new FormControl(probe.timeoutSeconds || null),
      terminationGracePeriodSeconds: new FormControl(probe.terminationGracePeriodSeconds || null),
      httpPath: new FormControl(probe.httpPath || null),
      httpPort: new FormControl(probe.httpPort || null),
      tcpHost: new FormControl(probe.tcpHost || null),
      tcpPort: new FormControl(probe.tcpPort || null),
      execCommand: new FormControl(probe.execCommand || []),
    });
  }

  createEnvironmentVariableForm(envVar: Partial<NewEnvironmentVariable> = {}): FormGroup {
    return new FormGroup({
      id: new FormControl(envVar.id || null),
      key: new FormControl(envVar.key || '', [Validators.required]),
      value: new FormControl(envVar.value || '', [Validators.required]),
      containerIndex: new FormControl(envVar.containerIndex || null),
    });
  }

  createFirewallEntryForm(firewallEntry: Partial<NewFirewallEntry> = {}): FormGroup {
    return new FormGroup({
      id: new FormControl(firewallEntry.id || null),
      cidr: new FormControl(firewallEntry.cidr, [Validators.required, ipValidator()]),
      level: new FormControl(firewallEntry.level || FirewallLevel.INGRESS, []),
      policy: new FormControl(firewallEntry.policy || PolicyType.INGRESS, []),
      rule: new FormControl(firewallEntry.rule || RuleType.ALLOW, []),
      containerIndex: new FormControl(firewallEntry.containerIndex || null),
    });
  }

  createAnnotationForm(annotation: Partial<NewAnnotation> = {}): FormGroup {
    return new FormGroup({
      id: new FormControl(annotation.id || null),
      key: new FormControl(annotation.key || '', [Validators.required]),
      value: new FormControl(annotation.value || '', [Validators.required])
    });
  }

  handleResourceTypeChange(form: FormGroup, resourceType: ResourceType): void {
    const currentCPURequest = form.get('cpuRequest')?.value;
    const currentCPULimit = form.get('cpuLimit')?.value;
    // const currentGPUType = form.get('gpuType')?.value;
    // const currentGPULimit = form.get('gpuLimit')?.value;

    switch (resourceType) {
      // case ResourceType.GPU: // Uncomment when needed
      //   form.patchValue({
      //     cpuRequest: null,
      //     cpuLimit: null,
      //     gpuType: currentGPUType || DEFAULT_VALUES.GPU.type,
      //     gpuLimit: currentGPULimit || DEFAULT_VALUES.GPU.limit,
      //   });
      //   break;
      case ResourceType.CPU:
        form.patchValue({
          cpuRequest: currentCPURequest || DEFAULT_VALUES.CPU.request,
          cpuLimit: currentCPULimit || DEFAULT_VALUES.CPU.limit,
          gpuType: null,
          gpuLimit: 0,
        });
        break;
      // case ResourceType.BOTH: // Uncomment when needed
      //   form.patchValue({
      //     cpuRequest: currentCPURequest || DEFAULT_VALUES.CPU.request,
      //     cpuLimit: currentCPULimit || DEFAULT_VALUES.CPU.limit,
      //     gpuType: currentGPUType || DEFAULT_VALUES.GPU.type,
      //     gpuLimit: currentGPULimit || DEFAULT_VALUES.GPU.limit,
      //   });
      //   break;
    }
  }

  createContainerFormWithData(data: Partial<ContainerFormData>): FormGroup {
    const form = this.createContainerForm();

    // Explicitly set the tag first
    if (data.imageTag) {
      form.get('imageTag')?.setValue(data.imageTag, {emitEvent: false});
    }

    form.patchValue({
      id: data.id || null,
      displayName: data.displayName || '',
      imageName: data.imageName || '',
      imageTag: data.imageTag, // Don't provide fallback to prevent override
      type: data.type || null,
      pullImageMode: data.pullImageMode || PullImageMode.PULL,
      registryUrl: data.registryUrl || '',
      registryUsername: data.registryUsername || '',
      registryPassword: data.registryPassword || '',
      resourceType: data.resourceType || ResourceType.CPU,
      cpuRequest: data.cpuRequest || null,
      cpuLimit: data.cpuLimit || null,
      memRequest: data.memRequest || '',
      memLimit: data.memLimit || null,
      gpuLimit: data.gpuLimit || 0,
      gpuType: data.gpuType || null,
      volumeMounts: data.volumeMounts || [],
      startCommand: data.startCommand || null,
      startParameters: data.startParameters || null
    }, {emitEvent: false});


    if (data.ports?.length) {
      const portsArray = form.get('ports') as FormArray;
      data.ports.forEach(port => {
        portsArray.push(this.createPortForm(port));
      });
    }

    if (data.probes?.length) {
      const probesArray = form.get('probes') as FormArray;
      data.probes.forEach(probe => {
        probesArray.push(this.createProbeForm(probe));
      });
    }

    if (data.envVars?.length) {
      const envVarsArray = form.get('envVars') as FormArray;
      data.envVars.forEach(envVar => {
        envVarsArray.push(this.createEnvironmentVariableForm(envVar));
      });
    }

    if (data.firewallEntries?.length) {
      const firewallEntriesArray = form.get('firewallEntries') as FormArray;
      data.firewallEntries.forEach(firewallEntry => {
        firewallEntriesArray.push(this.createFirewallEntryForm(firewallEntry));
      });
    }

    if (data.volumeMounts?.length) {
      const volumesArray = form.get('volumeMounts') as FormArray;
      data.volumeMounts.forEach(volumeMount => {
        const volumeForm = this.createVolumeMountForm(volumeMount);
        volumesArray.push(volumeForm);
      });
    }

    return form;
  }

  createVolumeMountForm(volumeMount?: Partial<IVolumeMount>): FormGroup {
    return new FormGroup({
      id: new FormControl<string | null>(volumeMount?.id ?? null),
      containerPath: new FormControl<string>(volumeMount?.containerPath ?? '', [Validators.required]),
      readOnly: new FormControl<boolean>(volumeMount?.readOnly ?? false),
      containerIndex: new FormControl<number | null>(volumeMount?.containerIndex),
      volume: new FormControl<IVolume>(volumeMount?.volume ?? null),
      volumeId: new FormControl<string | null>(volumeMount?.volume?.id ?? null)
    });
  }

  createModelSettingsForm(): FormGroup {
    return new FormGroup({
      modelName: new FormControl<string>(null, [Validators.required]),
      maxContextSize: new FormControl<number>(4000, [Validators.min(128)]),
      enablePartitioning: new FormControl<boolean>(false),
      partitions: new FormControl<number>(2, [Validators.max(8)]),

      enableReplication: new FormControl<boolean>(false),
      replicas: new FormControl<number>(2, [Validators.max(8)]),

      routingStrategy: new FormControl<RoutingStrategy>(RoutingStrategy.ROUND_ROBIN, Validators.required),

      routerReplicas: new FormControl<number>(1, [Validators.min(1), Validators.max(8)]),
      routerSessionKey: new FormControl<string>(null),

      deploymentMode: new FormControl<DeploymentMode>(DeploymentMode.AGGREGATED, Validators.required),

      l1Cache: new FormControl<boolean>(false),
      l1CacheSize: new FormControl<number>(1, [Validators.min(1), Validators.max(128)]),
      l2Cache: new FormControl<boolean>(false),
      l2CacheSize: new FormControl<number>(1, [Validators.min(1), Validators.max(10240)]),

      gpuMemory: new FormControl<number>(1024, [Validators.min(512)]),

      hfConfig: new FormControl<string>(''),
      hfTotalSafetensors: new FormControl<number>(0),
      source: new FormControl<string>(null, []),
      hfModelName: new FormControl<string>(null, []),
      branchToDeploy: new FormControl<string>(null, []),
      branchToDeployDisplayName: new FormControl<string>(null, []),
      loraSourceModel: new FormControl<string>(null, []),
    });
  }
}
