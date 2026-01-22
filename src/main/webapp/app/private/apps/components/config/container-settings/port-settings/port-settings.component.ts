import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { FormArray, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { DropdownModule } from 'primeng/dropdown';
import { ButtonModule } from 'primeng/button';
import { InputNumberModule } from 'primeng/inputnumber';
import { CommonModule } from '@angular/common';
import { ChipsModule } from 'primeng/chips';
import { firstValueFrom, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { IPort } from '../../../../../../shared/model/port.model';
import { ProtocolService } from '../../../../../../shared/service/protocol.service';
import { ApplicationFormService } from '../../../../../../shared/service/form/application-form.service';
import { DockerHubImage, SelectedDockerImage } from '../../../../../../shared/model/docker/docker-hub.model';
import { TooltipModule } from 'primeng/tooltip';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { CheckboxModule } from 'primeng/checkbox';
import { DockerPortInfoModelPortInfo } from '../../../../../../shared/model/docker/docker-port-info.model';
import { serviceNameForPort } from '../../../../../../shared/util/naming.util';
import { LayoutService } from '../../../../../../shared/service/theme/app-layout.service';
import { CdkCopyToClipboard } from '@angular/cdk/clipboard';
import { TagModule } from 'primeng/tag';
import { ApplicationMode } from '../../../../../../shared/model/enum/application-mode.model';


@Component({
  selector: 'sm-ports-settings',
  templateUrl: './port-settings.component.html',
  styleUrls: ['./port-settings.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    InputNumberModule,
    DropdownModule,
    ButtonModule,
    ChipsModule,
    TooltipModule,
    ToastModule,
    CheckboxModule,
    CdkCopyToClipboard,
    TagModule
  ]
})
export class PortSettingsComponent implements OnInit, OnDestroy {
  @Input() ports: IPort[] = [];
  @Input() resetTrigger!: Subject<void>;
  @Input() containerIndex: number;
  @Input() containerForm!: FormGroup;
  @Input() applicationName!: string;
  @Input() publicIp: string;
  @Input() namespace: string;
  @Input() selectedImage?: DockerHubImage | SelectedDockerImage;
  @Input() mode: keyof typeof ApplicationMode = ApplicationMode.APPLICATION;

  @Input() set defaultPorts(ports: DockerPortInfoModelPortInfo[]) {
    this._defaultPorts = ports || [];
    if (this._defaultPorts.length && !this.portsFormArray.length) {
      Promise.resolve(null).then(() => this.useDefaultPorts());
    }
    this.loading = false;
  }

  @Input() hasExistingIngressPort?: boolean;
  @Input() selectedTag?: string;
  @Output() portsChange = new EventEmitter<IPort[]>();

  private destroy$ = new Subject<void>();
  protocols: { label: string; value: any }[] = [];
  loading = true;
  _defaultPorts: DockerPortInfoModelPortInfo[];

  constructor(
    public layoutService: LayoutService,
    private applicationFormService: ApplicationFormService,
    private protocolService: ProtocolService,
    private messageService: MessageService
  ) {}

  get portsFormArray(): FormArray {
    return this.containerForm.get('ports') as FormArray;
  }

  get portForms(): FormGroup[] {
    return (this.containerForm.get('ports') as FormArray).controls as FormGroup[];
  }

  async ngOnInit(): Promise<void> {
    await this.loadProtocols();

    if (!this.containerForm.contains('ports')) {
      this.containerForm.addControl('ports', new FormArray([]));
    }

    const portsArray = this.containerForm.get('ports') as FormArray;
    const existingPorts = this.ports || [];

    // Clear existing ports
    while (portsArray.length) {
      portsArray.removeAt(0);
    }

    // Add existing ports
    if (existingPorts.length) {
      existingPorts.forEach((port: IPort) => {
        portsArray.push(this.applicationFormService.createPortForm({
          ...port,
          containerIndex: this.containerIndex
        }));
      });
    }

    // Subscribe to changes
    portsArray.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.emitPortsChange();
    });
  }

  onIngressPortChange(changedIndex: number): void {
    const portsArray = this.containerForm.get('ports') as FormArray;
    const portForm = portsArray.at(changedIndex);
    const servicePortControl = portForm.get('servicePort');
    const ingressPortChecked = portForm.get('ingressPort')?.value;
    portForm.patchValue({ingressHost: null});

    if (ingressPortChecked) {
      // Check both same container and other containers
      const hasExistingIngressInSameContainer = portsArray.controls.some((control, index) =>
        index !== changedIndex && control.get('ingressPort')?.value
      );

      if (hasExistingIngressInSameContainer || this.hasExistingIngressPort) {
        portForm.get('ingressPort')?.setValue(false, {emitEvent: false});
        this.messageService.add({
          severity: 'warn',
          summary: 'Warning',
          detail: 'Another port is already set as ingress. Please uncheck it first.'
        });
        return;
      }
      if (!servicePortControl?.value) {
        portForm.get('ingressPort')?.setValue(false, {emitEvent: false});
        servicePortControl?.markAsTouched();
        servicePortControl?.setErrors({'required': true});
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'Service port is required for ingress'
        });
        return;
      }
    }

    if (servicePortControl?.value) {
      servicePortControl.setErrors(null);
    }

    this.emitPortsChange();
  }

  private async loadProtocols(): Promise<void> {
    if (this.protocols && this.protocols.length) { // We keep protocols cached
      return;
    }

    try {
      const response = await firstValueFrom(this.protocolService.query());
      if (response.body) {
        this.protocols = response.body.map(protocol => ({
          label: protocol.code,
          value: protocol
        }));
      }
    } catch (error) {
      this.messageService.add({
        severity: 'error',
        summary: 'Error',
        detail: 'Failed to load protocols'
      });
      console.error('Failed to load protocols:', error);
      this.protocols = [];
    }
  }

  addPort(): void {
    const portsArray = this.containerForm.get('ports') as FormArray;

    // Find TCP protocol from loaded protocols
    const tcpProtocol = this.protocols.find(p => p.value.code === 'TCP')?.value;

    const newPort: Partial<IPort> = {
      name: 'port-' + this.containerIndex + (portsArray.length + 1),
      containerIndex: this.containerIndex,
      protocol: tcpProtocol || null,
      ingressPort: false  // Add default value
    };

    const portForm = this.applicationFormService.createPortForm(newPort);
    portsArray.push(portForm);
    this.containerForm.markAsDirty();
  }

  removePort(index: number): void {
    const portsArray = this.containerForm.get('ports') as FormArray;
    portsArray.removeAt(index);
    this.emitPortsChange();
  }

  private emitPortsChange(): void {
    const portsArray = this.containerForm.get('ports') as FormArray;
    const ports = portsArray.controls.map((control) => ({
      ...control.value,
      containerIndex: this.containerIndex,
      protocol: control.get('protocol')?.value,
      ingressPort: control.get('ingressPort')?.value
    }));

    this.containerForm.markAsDirty();
    this.portsChange.emit(ports);
  }

  async useDefaultPorts() {
    await this.loadProtocols();

    if (!this._defaultPorts?.length) {
      this.messageService.add({
        severity: 'info',
        summary: 'Information',
        detail: 'No default ports available for this image'
      });
      return;
    }

    this.portsFormArray.clear();
    // Add each port from the image config
    this._defaultPorts.forEach(portInfo => {
      const protocol = this.protocols.find(p => p.value.code === portInfo.protocol)?.value;
      this.portsFormArray.push(
        this.applicationFormService.createPortForm(
          {
            containerIndex: this.containerIndex,
            protocol,
            containerPort: Number(portInfo.containerPort),
            name: 'port-' + this.containerIndex + (this.portsFormArray.length + 1)
          }
        )
      );
    });

    this.emitPortsChange();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  protected readonly ApplicationMode = ApplicationMode;
  protected readonly serviceNameForPort = serviceNameForPort;
}
