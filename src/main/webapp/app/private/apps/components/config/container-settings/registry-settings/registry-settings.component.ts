import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {FormControl, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {MessageService} from 'primeng/api';
import {firstValueFrom} from 'rxjs';
import {DockerHubService} from "../../../../../../shared/service/docker/docker-hub.service";
import {ChipsModule} from "primeng/chips";
import {InputSwitchModule} from "primeng/inputswitch";
import {ButtonModule} from "primeng/button";
import {CommonModule} from "@angular/common";
import {dummyText} from "../../../../../../shared/util/form.util";
import {TooltipModule} from "primeng/tooltip";

@Component({
  selector: 'sm-registry-settings',
  standalone: true,
  templateUrl: './registry-settings.component.html',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    ChipsModule,
    InputSwitchModule,
    ButtonModule,
    TooltipModule
  ]
})
export class RegistrySettingsComponent implements OnInit {
  @Input() applicationId: string;
  @Input() imageName: string;
  @Input() registry: {registryUrl: string, registryUser: string, registryPassword: string};
  @Output() registryValidated = new EventEmitter<any>();
  @Output() cancelPrivateRegistry = new EventEmitter<any>();
  @Output() closeSettings = new EventEmitter<void>();

  registryForm = new FormGroup({
    registryUrl: new FormControl(null, Validators.required),
    registryUser: new FormControl(null, Validators.required),
    registryPassword: new FormControl(null, Validators.required)
  });

  constructor(
    private dockerService: DockerHubService,
    private messageService: MessageService
  ) {}

  async ngOnInit() {
    this.loadExistingRegistryData();
  }

  private loadExistingRegistryData(): void {
    this.registryForm.patchValue({
      registryUrl: this.registry.registryUrl,
      registryUser: this.registry.registryUser,
      registryPassword: this.registry.registryPassword ? this.registry.registryPassword :
        this.registry.registryUser ? dummyText(10) : null
    })
  }

  hide(): void {
    this.registryForm.reset();
    this.closeSettings.emit();
  }

  async cancelPrivate() {
    this.hide();
    this.cancelPrivateRegistry.emit();
  }

  async saveRegistry(): Promise<void> {
    if (this.registryForm.valid) {
      const formValue = this.registryForm.getRawValue();

      try {
        await firstValueFrom(this.dockerService.validateRegistryCredentials({
          registryUrl: formValue.registryUrl,
          registryUser: formValue.registryUser,
          registryPassword: formValue.registryPassword,
          applicationId: this.applicationId,
          imageName: this.imageName
        }));

        this.registryValidated.emit(formValue);
        this.hide();

      } catch {
        this.messageService.add({
          severity: 'error',
          summary: 'Registry Validation Error',
          detail: 'Failed to validate registry credentials'
        });
      }
    }
  }
}
