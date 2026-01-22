import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {InputSwitchModule} from 'primeng/inputswitch';
import {InputTextareaModule} from 'primeng/inputtextarea';
import {NgIf} from '@angular/common';
import SharedModule from '../../../../../../shared/shared.module';
import {FormControl, FormGroup, ReactiveFormsModule} from '@angular/forms';
import {ContainerType} from '../../../../../../shared/model/enum/container-type.model';
import {Subject, takeUntil} from 'rxjs';
import {RadioButtonModule} from 'primeng/radiobutton';
import {ButtonModule} from 'primeng/button';
import {InputTextModule} from 'primeng/inputtext';
import {StartCommandComponent} from "./start-command/start-command.component";
import {EnvironmentVariablesComponent} from "./environment-variables/environment-variables.component";
import {ResourceSettingsComponent} from "./resource-settings/resource-settings.component";
import {NewEnvironmentVariable} from "../../../../../../shared/model/environment-variable.model";
import {DEFAULT_VALUES} from '../../../../../../shared/model/container.model';
import {FirewallEntriesComponent} from "./firewall-entries/firewall-entries.component";
import {NewFirewallEntry} from "../../../../../../shared/model/firewall-entry.model";
import { ApplicationMode } from '../../../../../../shared/model/enum/application-mode.model';
import { AccordionModule } from 'primeng/accordion';

@Component({
  selector: 'sm-advanced-settings',
  standalone: true,
  imports: [
    InputSwitchModule,
    InputTextareaModule,
    NgIf,
    SharedModule,
    ReactiveFormsModule,
    RadioButtonModule,
    ButtonModule,
    InputTextModule,
    StartCommandComponent,
    EnvironmentVariablesComponent,
    FirewallEntriesComponent,
    ResourceSettingsComponent,
    AccordionModule
  ],
  templateUrl: './advanced-settings.component.html',
  styleUrls: ['./advanced-settings.component.scss'],
})
export class AdvancedSettingsComponent implements OnInit, OnDestroy {
  @Input() containerForm!: FormGroup;
  @Input() applicationForm!: FormGroup;
  @Input() containerIndex: number = 0;
  @Input()
  set resetTrigger(input: Subject<void>) {
    if (input) {
      this._resetTrigger = input;
      this._resetTrigger.pipe(
        takeUntil(this.destroy$)
      ).subscribe(() => {
        this.resetComponentState();
      });
    }
  }

  private destroy$ = new Subject<void>();
  _resetTrigger!: Subject<void>;
  private _showForm = false;

  get showForm(): boolean {
    return this._showForm;
  }

  set showForm(value: boolean) {
    this._showForm = value;
    if (!value) {
      this.resetAdvancedSettings();
    }
  }

  ngOnInit() {
    if (!this.containerForm.contains('type')) {
      this.containerForm.addControl('type', new FormControl(null));
    }

    if (
      this.containerForm.get('cpuLimit')?.value !== DEFAULT_VALUES.CPU.limit
      || this.containerForm.get('cpuRequest')?.value !== DEFAULT_VALUES.CPU.request
      || this.containerForm.get('memLimit')?.value !== DEFAULT_VALUES.MEMORY.limit
      || this.containerForm.get('memRequest')?.value !== DEFAULT_VALUES.MEMORY.request
      || this.containerForm.get('type')?.value
      || this.containerForm.get('startCommand')?.value
      || this.containerForm.get('startParameters')?.value
      || this.containerForm.get('envVars')?.value
      || this.containerForm.get('type')?.value !== ContainerType.WORKER
    ) {
      this.showForm = true;
    }
  }

  private resetAdvancedSettings(): void {
    this.containerForm.get('type')?.setValue(null);
  }

  private resetComponentState(): void {
    this.resetAdvancedSettings();
    this._showForm = false;
  }

  onEnvVarsUpdated(envVars: NewEnvironmentVariable[]): void {
    const envVarsWithIndex = envVars.map(ev => ({
      ...ev,
      containerIndex: this.containerIndex
    }));

    this.containerForm.patchValue({
      envVars: envVarsWithIndex
    }, { emitEvent: false });

    this.containerForm.markAsDirty();
  }

  onFirewallEntriesUpdated(firewallEntries: NewFirewallEntry[]): void {
    const firewallEntriesWithIndex = firewallEntries.map(entry => ({
      ...entry,
      containerIndex: this.containerIndex
    }));

    this.containerForm.patchValue({
      firewallEntries: firewallEntriesWithIndex
    }, { emitEvent: false });

    this.containerForm.markAsDirty();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  protected readonly ContainerType = ContainerType;
  protected readonly ApplicationMode = ApplicationMode;
}
