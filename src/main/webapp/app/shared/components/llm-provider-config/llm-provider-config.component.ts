// src/main/webapp/app/shared/components/llm-provider-config/llm-provider-config.component.ts

import { Component, forwardRef, inject, Input } from '@angular/core';
import { ControlValueAccessor, FormControl, FormGroup, NG_VALUE_ACCESSOR, ReactiveFormsModule } from '@angular/forms';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { LabelTooltipComponent } from '../label-tooltip/label-tooltip.component';
import { NgIf } from '@angular/common';
import { DeployedModelSelectorComponent } from '../deployed-model-selector/deployed-model-selector.component';
import { IApplication } from '../../model/application.model';
import { ApplicationService } from '../../service/application.service';

export interface LLMProviderConfig {
  provider: string;
  model: string;
  baseUrl: string;
  apiKey: string;
  internalName?: string;
  namespace?: string;
}
@Component({
  standalone: true,
  selector: 'sm-llm-provider-config',
  imports: [ReactiveFormsModule, DropdownModule, InputTextModule, LabelTooltipComponent, NgIf, DeployedModelSelectorComponent],
  template: `
    <div class="formgrid grid text-sm p-fluid" [formGroup]="form">
      <div class="field" [class]="fieldClass">
        <sm-label-tooltip tooltip="Select the LLM provider">Provider</sm-label-tooltip>
        <p-dropdown formControlName="provider" [options]="providers" optionLabel="name" optionValue="code" (onChange)="onProviderChange()">
        </p-dropdown>
      </div>
      <div class="field" [class]="fieldClass" *ngIf="isInternal">
        <sm-label-tooltip tooltip="Select a deployed model from your platform">Deployed Model</sm-label-tooltip>
        <sm-deployed-model-selector formControlName="deployedModel" [returnFull]="true" (ngModelChange)="onDeployedModelChange($event)">
        </sm-deployed-model-selector>
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
    deployedModel: new FormControl<IApplication | null>(null),
    internalName: new FormControl<string>(''),
    namespace: new FormControl<string>(''),
  });

  private onChange: (value: LLMProviderConfig) => void = () => {};
  private onTouched: () => void = () => {};

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
      this.form.patchValue({ model: '', baseUrl: '', apiKey: '' });
    }
  }

  onDeployedModelChange(deployedModel: IApplication | null) {
    if (deployedModel) {
      // Get the actual model name from extraConfig
      let modelName = deployedModel.internalName || '';
      if (deployedModel.extraConfig) {
        try {
          const extraConfig =
            typeof deployedModel.extraConfig === 'string' ? JSON.parse(deployedModel.extraConfig) : deployedModel.extraConfig;
          if (extraConfig.modelName) {
            modelName = extraConfig.modelName;
          }
        } catch (e) {
          console.warn('Failed to parse extraConfig', e);
        }
      }

      this.form.patchValue(
        {
          model: modelName, // Use actual model name
          baseUrl: '',
          apiKey: 'sk-no-key-required',
          internalName: deployedModel.internalName, // Keep for service discovery
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

      // If namespace provided, try to match more precisely
      if (namespace && models.length > 1) {
        match = models.find(m => m.internalName === internalName && m.deployedNamespace === namespace) || match;
      }

      if (match) {
        this.form.patchValue({ deployedModel: match }, { emitEvent: false });
      }
    });
  }

  writeValue(value: LLMProviderConfig | null): void {
    if (value) {
      this.form.patchValue(value, { emitEvent: false });

      // If internal provider with internalName, we need to find and set the deployed model
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
