import {Component, computed, EventEmitter, input, OnDestroy, Output, Signal, ViewChild} from '@angular/core';
import {ButtonModule} from 'primeng/button';
import {DropdownModule} from 'primeng/dropdown';
import {InputNumberModule} from 'primeng/inputnumber';
import {InputTextModule} from 'primeng/inputtext';
import {InputTextareaModule} from 'primeng/inputtextarea';
import {NgClass, NgIf} from '@angular/common';
import {PageLoadComponent} from '../../../../shared/components/page-load/page-load.component';
import {PanelModule} from 'primeng/panel';
import {RadioButtonModule} from 'primeng/radiobutton';
import {FormArray, FormGroup, ReactiveFormsModule} from '@angular/forms';
import {RippleModule} from 'primeng/ripple';
import {IApplication} from '../../../../shared/model/application.model';
import {ApplicationFormService, ContainerFormData} from '../../../../shared/service/form/application-form.service';
import {lastValueFrom, Subject} from 'rxjs';
import {ApplicationService} from '../../../../shared/service/application.service';
import {ActivatedRoute, Router} from '@angular/router';
import {displayError} from '../../../../shared/util/error.util';
import {markFormGroupTouched, revalidateForm} from '../../../../shared/util/form.util';
import {IPort} from '../../../../shared/model/port.model';
import {ContainerSettingsComponent} from './container-settings/container-settings.component';
import {NewAnnotation} from '../../../../shared/model/annotation.model';
import {AnnotationsComponent} from './annotations/annotations.component';
import {ProgressSpinnerModule} from 'primeng/progressspinner';
import {IVolumeMount} from '../../../../shared/model/volume-mount.model';
import {ApplicationStatus} from '../../../../shared/model/enum/application-status.model';
import {MessagesModule} from 'primeng/messages';
import {Store} from '@ngxs/store';
import {displayWarning} from '../../../../shared/util/success.util';
import {ContainerType} from '../../../../shared/model/enum/container-type.model';
import {IContainer} from '../../../../shared/model/container.model';
import {ApplicationMode} from '../../../../shared/model/enum/application-mode.model';
import {ModelSettingsComponent} from './model-settings/model-settings.component';
import TranslateDirective from '../../../../shared/language/translate.directive';
import {IModelConfig} from '../../../../shared/model/model-settings';
import {animate, style, transition, trigger} from '@angular/animations';
import {ConfirmDialogModule} from 'primeng/confirmdialog';
import {roundUpTo} from '../../../../shared/util/display.util';
import {Selectors} from '../../../../shared/state/selectors';
import {toObservable} from '@angular/core/rxjs-interop';
import {AccordionModule} from 'primeng/accordion';

@Component({
  selector: 'sm-app-config',
  standalone: true,
  templateUrl: './app-config.component.html',
  styles: [`
    @keyframes appConfigFadeIn {
      from {
        opacity: 0;
        transform: translateY(-4px);
      }
      to {
        opacity: 1;
        transform: none;
      }
    }

    .fade-in {
      animation: appConfigFadeIn 150ms ease-out;
    }

    :host ::ng-deep .advanced-accordion .p-accordion .p-accordion-header .p-accordion-header-link {
      background: transparent;
    }
  `],
  animations: [
    trigger('fadeIn', [
      transition(':enter', [
        style({opacity: 0, transform: 'translateY(-4px)'}),
        animate('150ms ease-in', style({opacity: 1, transform: 'none'})),
      ]),
      transition(':leave', [
        style({opacity: 1, transform: 'none'}),
        animate('150ms ease-in', style({opacity: 0, transform: 'translateY(-4px)'})),
      ]),
    ]),
  ],
  imports: [
    ButtonModule,
    ContainerSettingsComponent,
    DropdownModule,
    InputNumberModule,
    InputTextModule,
    InputTextareaModule,
    NgIf,
    PageLoadComponent,
    PanelModule,
    RadioButtonModule,
    ReactiveFormsModule,
    RippleModule,
    AnnotationsComponent,
    ProgressSpinnerModule,
    MessagesModule,
    ModelSettingsComponent,
    TranslateDirective,
    NgClass,
    ConfirmDialogModule,
    AccordionModule
  ]
})
export class AppConfigComponent implements OnDestroy {
  application = input.required<IApplication>();

  @Output()
  onCancel = new EventEmitter<void>();
  @Output()
  publishing = new EventEmitter<boolean>();

  @ViewChild(ModelSettingsComponent) readonly modelSettingsComponent: ModelSettingsComponent;

  readonly GpuMemoryNotUpdated = {severity: 'warn', detail: 'The provided memory allocation is lower than the recommended value. This may lead to out-of-memory errors.'};

  MAX_CONTAINER_NUM = 5;
  ApplicationStatus = ApplicationStatus;

  protected destroy$ = new Subject<void>();
  protected resetTrigger = new Subject<void>();

  applicationForm: FormGroup;
  containers: ContainerFormData[] = [];
  currentContainerForm?: FormGroup;
  currentContainerIndex?: number;

  isSaving = false;
  isPublishing = false;
  showContainerSettings = false;
  showGpuMemoryWarning = false;
  status: Signal<keyof typeof ApplicationStatus>;
  loading = true;

  constructor(
    private applicationService: ApplicationService,
    private router: Router,
    private activatedRoute: ActivatedRoute,
    private applicationFormService: ApplicationFormService,
    private store: Store,
  ) {
    this.status = computed(() => {
      const appStatus = this.store.selectSignal(Selectors.resourceStatus(this.application().id))();
      return appStatus?.status;
    });
    toObservable(this.application).subscribe({next: app => this.initApplication(app)})
  }

  initApplication(application: IApplication) {
    this.activatedRoute.queryParams.subscribe(async params => {
      if (!params['id']) {
        await this.router.navigateByUrl('/404');
        return;
      }

      if (!application) {
        return;
      }

      await this.initExistingApplication(application);

      this.loading = false;
    });
  }

  private async initExistingApplication(application: IApplication) {
    this.applicationForm = this.applicationFormService.createApplicationForm();
    this.applicationForm.patchValue(application);

    if (application.mode === ApplicationMode.MODEL && application.extraConfig) {
      try {
        // First, try to parse as IModelSettings (saved data)
        const modelSettings: IModelConfig = JSON.parse(application.extraConfig);
        this.applicationForm.get('modelSettings').patchValue(modelSettings);
        this.applicationForm.get('modelSettings').valueChanges.subscribe((value) => {
          this.updateGpuMemoryWarning(value as IModelConfig);
        })
      } catch (error) {
        console.warn('Could not parse extraConfig as model settings:', error);
      }
    }

    this.containers = (application.containers as ContainerFormData[])
      .sort((a, b) => a.imageName.localeCompare(b.imageName));

    const containersArray = this.applicationForm.controls.containers as FormArray;
    application.containers.forEach(con => {
      const conForm = this.applicationFormService.createContainerForm();
      conForm.patchValue(con as any);
      containersArray.push(conForm);

      const portsArray = conForm.controls.ports as FormArray;
      con.ports.forEach(port => {
        const portForm = this.applicationFormService.createPortForm();
        portForm.patchValue(port as any);
        portsArray.push(portForm);
      });

      const probesArray = conForm.controls.probes as FormArray;
      con.probes.forEach(probe => {
        const probeForm = this.applicationFormService.createProbeForm();
        probeForm.patchValue(probe as any);
        probesArray.push(probeForm);
      });

      const volumeMountsArray = conForm.controls.volumeMounts as FormArray;
      con.volumeMounts.forEach(volume => {
        const volumeForm = this.applicationFormService.createVolumeMountForm();
        volumeForm.patchValue(volume as IVolumeMount);
        volumeMountsArray.push(volumeForm);
      });

      const envVarsArray = conForm.controls.envVars as FormArray;
      con.envVars.forEach(envVar => {
        const envVarForm = this.applicationFormService.createEnvironmentVariableForm();
        envVarForm.patchValue(envVar as any);
        envVarsArray.push(envVarForm);
      });

      const firewallEntriesArray = conForm.controls.firewallEntries as FormArray;
      con.firewallEntries.forEach(entry => {
        const entryForm = this.applicationFormService.createFirewallEntryForm();
        entryForm.patchValue(entry as any);
        firewallEntriesArray.push(entryForm);
      });
    });
    const annotationsArray = this.applicationForm.controls.annotations as FormArray;
    application.annotations.forEach(ann => {
      annotationsArray.push(this.applicationFormService.createAnnotationForm(ann as NewAnnotation))
    });

    if (!this.containers.length) {
      this.addContainer();
    } else {
      this.currentContainerForm = null;
      this.currentContainerIndex = null;
      this.showContainerSettings = false;
    }
  }

  addContainer() {
    this.currentContainerForm = this.applicationFormService.createContainerForm();
    this.currentContainerIndex = null;
    this.showContainerSettings = true;
  }

  cancelContainer() {
    this.showContainerSettings = false;
    this.currentContainerForm = undefined;
    this.currentContainerIndex = undefined;
  }

  async saveContainer() {
    if (this.currentContainerForm?.valid) {
      const containerData = this.currentContainerForm.getRawValue();

      // Volume names & mount paths must be unique within container
      if (containerData.volumeMounts?.length) {
        if (this.hasDuplicates(
          containerData.volumeMounts?.map((mount: IVolumeMount) => mount.volume.name)
        )) {
          displayError(this.store, 'You can not define two volumes with the same name in the same container');
          return;
        }
        if (this.hasDuplicates(
          containerData.volumeMounts?.map((vol: IVolumeMount) => vol.containerPath)
        )) {
          displayError(this.store, 'You can not mount multiple volumes on the same path');
          return;
        }
      }
      // Port names must be unique within container
      if (containerData.ports?.length) {
        if (this.hasDuplicates(
          containerData.ports?.map((port: IPort) => port.name)
        )) {
          displayError(this.store, 'You have duplicate port names in this container');
          return;
        }
      }

      const containersArray = this.applicationForm.get('containers') as FormArray;
      if (this.currentContainerIndex !== undefined && this.currentContainerIndex !== null) {
        this.containers[this.currentContainerIndex] = containerData;
        containersArray.setControl(
          this.currentContainerIndex,
          this.applicationFormService.createContainerFormWithData(containerData)
        );
      } else {
        this.containers.push(containerData);
        containersArray.push(
          this.applicationFormService.createContainerFormWithData(containerData)
        );
      }

      this.containers = [...this.containers];
      this.showContainerSettings = false;
      this.currentContainerForm = undefined;
      this.currentContainerIndex = undefined;

      if (this.applicationForm.valid && this.application()?.status === ApplicationStatus.CREATED) {
        await this.saveApplication();
      }
    } else {
      markFormGroupTouched(this.currentContainerForm);
    }
  }

  editContainer(container: ContainerFormData): void {
    const index = this.containers.indexOf(container);
    if (index === this.currentContainerIndex) { // Dbl-click => just close container settings
      this.currentContainerIndex = null;
      this.showContainerSettings = false;
      return;
    }

    this.currentContainerForm = this.applicationFormService.createContainerFormWithData(container);
    this.currentContainerIndex = index;
    this.showContainerSettings = true;
  }

  removeContainer(event: Event, container: ContainerFormData) {
    event.stopPropagation();
    const index = this.containers.indexOf(container);
    if (index > -1) {
      this.containers.splice(index, 1);
      const containersArray = this.applicationForm.get('containers') as FormArray;
      containersArray.removeAt(index);
      this.applicationForm.markAsDirty();
    }
  }

  hasExistingIngressPort(containerIndex: number): boolean {
    return this.containers.some((container, idx) =>
      idx !== containerIndex && container.ports?.some(p => p.ingressPort)
    );
  }

  onPortsUpdated(event: { ports: IPort[], containerIndex: number }) {
    const {ports, containerIndex} = event;
    const newIngressPort = ports.find(p => p.ingressPort);

    if (newIngressPort) {
      const hasExistingIngressPort = this.containers.some((container, idx) =>
        idx !== containerIndex && container.ports?.some(p => p.ingressPort)
      );
      if (hasExistingIngressPort) {
        displayWarning(this.store, 'Another container has an Ingress port. Please uncheck it first.');
        return;
      }
    }

    // Update current container's ports
    if (containerIndex < this.containers.length) {
      this.containers[containerIndex].ports = ports;
    }

    const containersArray = this.applicationForm.get('containers') as FormArray;
    if (containerIndex < containersArray.length) {
      const containerForm = containersArray.at(containerIndex);
      containerForm.patchValue({ports});
    }

    this.applicationForm.markAsDirty();
  }

  async saveApplication(publish?: boolean, redeploy = false): Promise<IApplication> {
    revalidateForm(this.applicationForm);
    if (this.applicationForm.invalid) {
      displayError(this.store, 'Please fill in all required fields');
      return Promise.reject();
    }

    const application = this.applicationForm.getRawValue();
    const existing = this.application();

    // Must have at least one worker container for APPLICATION mode
    if (existing.mode === ApplicationMode.APPLICATION &&
      !application.containers.filter((container: IContainer) => container.type === ContainerType.WORKER).length) {
      displayError(this.store, 'This application must have at least one worker container');
      return Promise.reject();
    }

    try {
      this.isSaving = true;
      // Re-add attributes not present in the form
      application.type = existing.type;
      application.mode = existing.mode;
      application.status = existing.status;
      application.workloadType = existing.workloadType;
      application.ingressHostName = existing.ingressHostName;
      application.deployedNamespace = existing.deployedNamespace;
      application.keepVolumes = true;
      application.fromTemplate = existing.fromTemplate;
      application.totalAppCosts = existing.totalAppCosts;
      application.monthlyAppCosts = existing.monthlyAppCosts;

      // Handle model settings for MODEL applications
      if (existing.mode === ApplicationMode.MODEL && application.modelSettings) {
        application.extraConfig = JSON.stringify(application.modelSettings);
        // Remove the temporary modelSettings property since it's not part of IApplication
        delete application.modelSettings;
      }

      await lastValueFrom(this.applicationService.save(application));

      if (publish) {
        if (!this.checkCredentials(application)) {
          return Promise.reject();
        }

        this.publishing.emit(true);
        this.isSaving = false;
        this.isPublishing = true;
        try {
          await lastValueFrom(redeploy ? this.applicationService.redeploy(application) : this.applicationService.deploy(application));
        } catch (err) {
          console.log(err);
        } finally {
          this.publishing.emit(false);
          if (!redeploy && existing.status === ApplicationStatus.CREATED) {
            await this.router.navigate([existing.mode === 'MODEL' ? '/models' : '/apps'], {queryParams: { id: existing.id, reload: 1 }});
          }
        }
      }

      return Promise.resolve(application);
    } catch (e) {
      displayError(this.store, e);
    } finally {
      this.isSaving = false;
      this.isPublishing = false;
    }

    return Promise.reject();
  }

  async redeployApplication() {
    await this.saveApplication(true, true);
  }

  onAnnotationsUpdated(annotations: NewAnnotation[]): void {
    this.applicationForm.patchValue({
      annotations: annotations
    }, {emitEvent: false});

    this.applicationForm.markAsDirty();
  }

  hasRequiredContainers() {
    return (this.applicationForm.get('containers') as FormArray).length;
  }

  private checkCredentials(application: IApplication): boolean {
    if (!application || !application.containers) {
      return true;
    }
    let validCredentials = true;

    // Docker credentials
    application.containers.forEach(container => {
      if (container.registryUrl && !container.registryPassword) {
        validCredentials = false;
        displayError(this.store, 'Please reenter your registry password for container ' + container.imageName);
      }
    })
    // S3 credentials
    application.containers.forEach(container => {
      if (container.volumeMounts) {
        container.volumeMounts.forEach(volumeMount => {
          if (volumeMount.volume.bucketUrl && !volumeMount.volume.accessSecret) {
            validCredentials = false;
            displayError(this.store, 'Please reenter your storage password for volume ' + volumeMount.volume?.name);
          }
        });
      }
    })

    return validCredentials;
  }

  volumesAreValid() {
    return !this.currentContainerForm?.getRawValue().volumeMounts
      .filter((mount: IVolumeMount) => !mount.volume.name).length;
  }

  private hasDuplicates = (arr: string[]) =>
    arr.filter((item, index) => item && arr.indexOf(item) !== index).length;


  containerName(container: IContainer): string {
    if (container.displayName)
      return container.displayName;
    else
      return `${container.imageName}:${container.imageTag || 'latest'}`;
  }

  updateGpuMemoryWarning(modelConfig: IModelConfig) {
    if (this.modelSettingsComponent?.modelPerformanceReport()) {
      const recommendedMemory = this.modelSettingsComponent.modelPerformanceReport()[0] * 1024 // convert to MB;
      this.showGpuMemoryWarning = modelConfig.gpuMemory < roundUpTo(recommendedMemory, 0);
    } else {
      this.showGpuMemoryWarning = false;
    }
  }

  ngOnDestroy() {
    this.resetTrigger.complete();
    this.destroy$.next();
    this.destroy$.complete();
  }

  protected readonly ApplicationMode = ApplicationMode;
}
