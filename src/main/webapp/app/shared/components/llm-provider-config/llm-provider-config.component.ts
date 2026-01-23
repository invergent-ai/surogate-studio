// src/app/shared/components/llm-provider-config/llm-provider-config.component.ts

import { Component, forwardRef, inject, Input } from '@angular/core';
import { ControlValueAccessor, FormControl, FormGroup, NG_VALUE_ACCESSOR, ReactiveFormsModule } from '@angular/forms';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { LabelTooltipComponent } from '../label-tooltip/label-tooltip.component';
import { NgIf } from '@angular/common';
import { IApplication } from '../../model/application.model';
import { ApplicationService } from '../../service/application.service';
import { derivedAsync } from 'ngxtension/derived-async';
import { map } from 'rxjs/operators';

export interface LLMProviderConfig {
  provider: string;
  model: string;
  baseUrl: string;
  apiKey: string;
  internalName?: string;
  namespace?: string;
}

interface DeployedModelOption extends IApplication {
  apiModelName: string;
}

@Component({
  standalone: true,
  selector: 'sm-llm-provider-config',
  imports: [ReactiveFormsModule, DropdownModule, InputTextModule, LabelTooltipComponent, NgIf],
  template: `
    <div class="formgrid grid text-sm p-fluid" [formGroup]="form">
      <div class="field" [class]="fieldClass">
        <sm-label-tooltip tooltip="Select the LLM provider">Provider</sm-label-tooltip>
        <p-dropdown formControlName="provider" [options]="providers" optionLabel="name" optionValue="code" (onChange)="onProviderChange()">
        </p-dropdown>
      </div>
      <div class="field" [class]="fieldClass" *ngIf="isInternal">
        <sm-label-tooltip tooltip="Select a deployed model from your platform">Deployed Model</sm-label-tooltip>
        <p-dropdown
          [options]="deployedModels() ?? []"
          formControlName="deployedModel"
          optionLabel="name"
          [placeholder]="'Select deployed model'"
          [showClear]="true"
          [filter]="true"
          filterBy="name"
          appendTo="body"
          (onChange)="onDeployedModelChange($event.value)"
        >
          <ng-template pTemplate="selectedItem" let-item>
            <div class="flex align-items-center gap-2" *ngIf="item">
              <span>{{ item.name }}</span>
              <span class="text-xs text-500">({{ item.deployedNamespace }})</span>
            </div>
          </ng-template>
          <ng-template pTemplate="item" let-item>
            <div class="flex align-items-center justify-content-between w-full">
              <span>{{ item.name }}</span>
              <span class="text-xs text-500">{{ item.deployedNamespace }}</span>
            </div>
          </ng-template>
        </p-dropdown>
      </div>
      <div class="field" [class]="fieldClass" *ngIf="showModel && !isInternal">
        <sm-label-tooltip tooltip="Model name or identifier">Model</sm-label-tooltip>
        <input pInputText type="text" formControlName="model" [placeholder]="getModelPlaceholder()" />
      </div>
      <div class="field" [class]="fieldClass" *ngIf="!isInternal">
        <sm-label-tooltip tooltip="API endpoint URL">Base URL</sm-label-tooltip>
        <input pInputText type="text" formControlName="baseUrl" [placeholder]="getBaseUrlPlaceholder()" />
      </div>
      <div class="field" [class]="fieldClass" *ngIf="!isInternal">
        <sm-label-tooltip tooltip="API key for authentication">API Key</sm-label-tooltip>
        <input pInputText type="password" formControlName="apiKey" placeholder="sk-..." />
      </div>
    </div>
  `,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => LlmProviderConfigComponent),
      multi: true,
    },
  ],
})
export class LlmProviderConfigComponent implements ControlValueAccessor {
  @Input() showModel = true;
  @Input() fieldClass = 'col-12 md:col-3';

  private applicationService = inject(ApplicationService);

  providers = [
    { name: 'Internal', code: 'internal' },
    { name: 'OpenAI', code: 'openai' },
    { name: 'Anthropic', code: 'anthropic' },
    { name: 'OpenRouter', code: 'openrouter' },
    { name: 'Azure OpenAI', code: 'azure' },
    { name: 'vLLM', code: 'vllm' },
    { name: 'Ollama', code: 'ollama' },
  ];

  private defaultUrls: Record<string, string> = {
    internal: '',
    openai: 'https://api.openai.com/v1',
    anthropic: 'https://api.anthropic.com/v1',
    openrouter: 'https://openrouter.ai/api/v1',
    azure: '',
    vllm: '',
    ollama: 'http://localhost:11434/v1',
  };

  private modelPlaceholders: Record<string, string> = {
    internal: '',
    openai: 'gpt-4o-mini',
    anthropic: 'claude-3-5-sonnet-20241022',
    openrouter: 'openai/gpt-4o-mini',
    azure: 'gpt-4o-mini',
    vllm: 'meta-llama/Llama-3-8B-Instruct',
    ollama: 'llama3',
  };

  form = new FormGroup({
    provider: new FormControl<string>('openai'),
    model: new FormControl<string>(''),
    baseUrl: new FormControl<string>('https://api.openai.com/v1'),
    apiKey: new FormControl<string>(''),
    deployedModel: new FormControl<DeployedModelOption | null>(null),
    internalName: new FormControl<string>(''),
    namespace: new FormControl<string>(''),
  });

  private onChange: (value: LLMProviderConfig) => void = () => {};
  private onTouched: () => void = () => {};

  // Load deployed models for internal provider
  deployedModels = derivedAsync(() =>
    this.applicationService
      .query({ 'mode.equals': 'MODEL', 'status.equals': 'DEPLOYED' })
      .pipe(map(response => (response.body ?? []).map(app => this.withApiModelName(app)))),
  );

  private withApiModelName(app: IApplication): DeployedModelOption {
    const config = app.extraConfig ? JSON.parse(app.extraConfig) : {};

    // If LoRA is configured, use the adapter name
    if (config.loraSourceModel) {
      return { ...app, apiModelName: 'serve-lora' };
    }

    // Otherwise use base model name
    const apiModelName = config.source === 'hf' ? config.hfModelName : config.branchToDeploy ?? config.modelName;
    return { ...app, apiModelName };
  }

  get isInternal(): boolean {
    return this.form.get('provider')?.value === 'internal';
  }

  constructor() {
    this.form.valueChanges.subscribe(value => {
      this.onChange(value as LLMProviderConfig);
    });
  }

  getModelPlaceholder(): string {
    const provider = this.form.get('provider')?.value || 'openai';
    return this.modelPlaceholders[provider] || 'model-name';
  }

  getBaseUrlPlaceholder(): string {
    const provider = this.form.get('provider')?.value || 'openai';
    return this.defaultUrls[provider] || 'https://api.example.com/v1';
  }

  onProviderChange() {
    const provider = this.form.get('provider')?.value;
    if (provider && this.defaultUrls[provider] !== undefined) {
      this.form.get('baseUrl')?.setValue(this.defaultUrls[provider]);
    }
    if (provider === 'internal') {
      this.form.patchValue({ model: '', baseUrl: '', apiKey: '', deployedModel: null });
    }
  }

  onDeployedModelChange(deployedModel: DeployedModelOption | null) {
    if (deployedModel) {
      this.form.patchValue(
        {
          model: deployedModel.apiModelName,
          baseUrl: '',
          apiKey: 'sk-no-key-required',
          internalName: deployedModel.internalName,
          namespace: deployedModel.deployedNamespace,
        },
        { emitEvent: false },
      );
      this.onChange(this.form.value as LLMProviderConfig);
    }
  }

  private loadDeployedModelByInternalName(internalName: string, namespace?: string): void {
    this.applicationService.query({ 'internalName.equals': internalName }).subscribe(response => {
      const models = response.body || [];
      let match = models.find(m => m.internalName === internalName);

      if (namespace && models.length > 1) {
        match = models.find(m => m.internalName === internalName && m.deployedNamespace === namespace) || match;
      }

      if (match) {
        this.form.patchValue({ deployedModel: this.withApiModelName(match) }, { emitEvent: false });
      }
    });
  }

  writeValue(value: LLMProviderConfig | null): void {
    if (value) {
      this.form.patchValue(value, { emitEvent: false });

      if (value.provider === 'internal' && value.internalName) {
        this.loadDeployedModelByInternalName(value.internalName, value.namespace);
      }
    } else {
      this.form.reset(
        {
          provider: 'openai',
          model: '',
          baseUrl: this.defaultUrls['openai'],
          apiKey: '',
          deployedModel: null,
        },
        { emitEvent: false },
      );
    }
  }

  registerOnChange(fn: (value: LLMProviderConfig) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    isDisabled ? this.form.disable() : this.form.enable();
  }
}
