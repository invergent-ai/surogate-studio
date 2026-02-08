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
import { PaginatorModule } from 'primeng/paginator';
import { AccordionModule } from 'primeng/accordion';
import { LucideAngularModule, SlidersHorizontal } from 'lucide-angular';
import { TooltipModule } from 'primeng/tooltip';
import { UserApiKeyService } from '../../service/user-api-key.service';
import { ApiKeyProvider, LLM_PROVIDERS_WITH_SAVED_KEYS } from '../../model/enum/api-key.enum';

export interface LLMProviderConfig {
  provider: string;
  model: string;
  baseUrl: string;
  apiKey: string;
  useSavedKey?: boolean;
  internalName?: string;
  namespace?: string;
  tokenizer?: string;
  maxTokens?: number;
  temperature?: number;
  topP?: number;
  topK?: number;
  minP?: number;
  presencePenalty?: number;
  enableThinking?: boolean;
}

interface DeployedModelOption extends IApplication {
  apiModelName: string;
  hfModelName?: string; // Add
}

@Component({
  standalone: true,
  selector: 'sm-llm-provider-config',
  imports: [
    ReactiveFormsModule,
    DropdownModule,
    InputTextModule,
    LabelTooltipComponent,
    NgIf,
    PaginatorModule,
    AccordionModule,
    LucideAngularModule,
    TooltipModule,
  ],
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
        @if (form.get('useSavedKey')?.value) {
          <div class="flex align-items-center gap-2 p-2 surface-100 border-round">
            <i class="pi pi-check-circle text-green-500"></i>
            <span class="text-sm">Using saved {{ form.get('provider')?.value }} key</span>
            <a class="text-xs text-500 cursor-pointer ml-auto" (click)="clearSavedKey()">Use different key</a>
          </div>
        } @else {
          <input pInputText type="password" formControlName="apiKey" placeholder="sk-..." />
        }
      </div>

      <!-- Advanced toggle - only for model under test -->
      <div class="field flex align-items-end" [class]="fieldClass" *ngIf="showAdvancedToggle">
        <a
          (click)="showAdvanced = !showAdvanced"
          class="text-sm text-500 cursor-pointer hover:text-primary flex align-items-center gap-2 mb-2"
        >
          <i-lucide [img]="SlidersHorizontal" class="w-1rem h-1rem"></i-lucide>
          Advanced
        </a>
      </div>

      <!-- Advanced Generation Parameters - new row below -->
      <div class="col-12" *ngIf="showAdvanced && showAdvancedToggle">
        <div class="formgrid grid">
          <div class="field col-6 md:col-2">
            <sm-label-tooltip tooltip="Controls randomness (0.0-2.0)">Temperature</sm-label-tooltip>
            <p-inputNumber
              formControlName="temperature"
              [min]="0"
              [max]="2"
              [minFractionDigits]="1"
              [maxFractionDigits]="2"
              placeholder="0.7"
            ></p-inputNumber>
          </div>
          <div class="field col-6 md:col-2">
            <sm-label-tooltip tooltip="Nucleus sampling (0.0-1.0)">Top P</sm-label-tooltip>
            <p-inputNumber
              formControlName="topP"
              [min]="0"
              [max]="1"
              [minFractionDigits]="1"
              [maxFractionDigits]="2"
              placeholder="0.8"
            ></p-inputNumber>
          </div>
          <div class="field col-6 md:col-2">
            <sm-label-tooltip tooltip="Limits vocabulary to top K">Top K</sm-label-tooltip>
            <p-inputNumber formControlName="topK" [min]="1" [max]="100" placeholder="20"></p-inputNumber>
          </div>
          <div class="field col-6 md:col-2">
            <sm-label-tooltip tooltip="Min probability (0.0-1.0)">Min P</sm-label-tooltip>
            <p-inputNumber
              formControlName="minP"
              [min]="0"
              [max]="1"
              [minFractionDigits]="1"
              [maxFractionDigits]="2"
              placeholder="0.0"
            ></p-inputNumber>
          </div>
          <div class="field col-6 md:col-2">
            <sm-label-tooltip tooltip="Penalizes repetition (-2.0 to 2.0)">Presence</sm-label-tooltip>
            <p-inputNumber
              formControlName="presencePenalty"
              [min]="-2"
              [max]="2"
              [minFractionDigits]="1"
              [maxFractionDigits]="2"
              placeholder="0.0"
            ></p-inputNumber>
          </div>
          <div class="field col-6 md:col-2">
            <sm-label-tooltip tooltip="Thinking mode for Qwen3">Thinking</sm-label-tooltip>
            <p-dropdown
              formControlName="enableThinking"
              [options]="thinkingOptions"
              optionLabel="label"
              optionValue="value"
              placeholder="Default"
            ></p-dropdown>
          </div>
        </div>
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
  @Input() showAdvancedToggle = false;

  private applicationService = inject(ApplicationService);
  private userApiKeyService = inject(UserApiKeyService);

  showAdvanced = false;
  protected readonly SlidersHorizontal = SlidersHorizontal;

  providers = [
    { name: 'Internal', code: ApiKeyProvider.INTERNAL },
    { name: 'OpenAI', code: ApiKeyProvider.OPENAI },
    { name: 'Anthropic', code: ApiKeyProvider.ANTHROPIC },
    { name: 'OpenRouter', code: ApiKeyProvider.OPENROUTER },
    { name: 'Azure OpenAI', code: ApiKeyProvider.AZURE },
    { name: 'vLLM', code: ApiKeyProvider.VLLM },
    { name: 'Ollama', code: ApiKeyProvider.OLLAMA },
  ];

  thinkingOptions = [
    { label: 'Default', value: null },
    { label: 'Enabled', value: true },
    { label: 'Disabled', value: false },
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
    useSavedKey: new FormControl<boolean>(false),
    model: new FormControl<string>(''),
    baseUrl: new FormControl<string>('https://api.openai.com/v1'),
    apiKey: new FormControl<string>(''),
    deployedModel: new FormControl<DeployedModelOption | null>(null),
    internalName: new FormControl<string>(''),
    namespace: new FormControl<string>(''),
    tokenizer: new FormControl<string>(''),
    maxTokens: new FormControl<number | null>(null),
    // Generation parameters
    temperature: new FormControl<number | null>(null),
    topP: new FormControl<number | null>(null),
    topK: new FormControl<number | null>(null),
    minP: new FormControl<number | null>(null),
    presencePenalty: new FormControl<number | null>(null),
    enableThinking: new FormControl<boolean | null>(null),
  });

  private onChange: (value: LLMProviderConfig) => void = () => {};
  private onTouched: () => void = () => {};

  // Load deployed models for internal provider
  deployedModels = derivedAsync(() =>
    this.applicationService
      .query({ 'mode.equals': 'MODEL', 'status.equals': 'DEPLOYED' })
      .pipe(map(response => (response.body ?? []).map(app => this.withApiModelName(app)))),
  );

  clearSavedKey() {
    this.form.patchValue({ useSavedKey: false, apiKey: '' });
  }

  private withApiModelName(app: IApplication): DeployedModelOption {
    const config = app.extraConfig ? JSON.parse(app.extraConfig) : {};

    if (config.loraSourceModel) {
      return { ...app, apiModelName: 'serve-lora', hfModelName: config.hfModelName };
    }

    if (config.source === 'hub') {
      return { ...app, apiModelName: `/models/${config.branchToDeploy}`, hfModelName: config.hfModelName };
    }

    const apiModelName = config.source === 'hf' ? config.hfModelName : config.branchToDeploy ?? config.modelName;
    return { ...app, apiModelName, hfModelName: config.hfModelName };
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

  onProviderChange() {
    const provider = this.form.get('provider')?.value as ApiKeyProvider;
    if (provider && this.defaultUrls[provider] !== undefined) {
      this.form.get('baseUrl')?.setValue(this.defaultUrls[provider]);
    }
    if (provider === ApiKeyProvider.INTERNAL) {
      this.form.patchValue({ model: '', baseUrl: '', apiKey: '', deployedModel: null, useSavedKey: false });
    } else if (LLM_PROVIDERS_WITH_SAVED_KEYS.includes(provider)) {
      this.checkAndApplySavedKey(provider);
    } else {
      this.form.patchValue({ useSavedKey: false, apiKey: '' });
    }
  }

  getBaseUrlPlaceholder(): string {
    const provider = this.form.get('provider')?.value || 'openai';
    return this.defaultUrls[provider] || 'https://api.example.com/v1';
  }

  onDeployedModelChange(deployedModel: DeployedModelOption | null) {
    if (deployedModel) {
      const config = deployedModel.extraConfig ? JSON.parse(deployedModel.extraConfig) : {};

      const tokenizer =
        config.source === 'hub' && config.branchToDeploy
          ? `lakefs://${config.branchToDeploy}/tokenizer.json`
          : deployedModel.hfModelName || '';

      const maxContextSize = config.maxContextSize || 4096;
      const maxTokens = Math.floor(maxContextSize * 0.9);

      const isQwen = (deployedModel.hfModelName || '').toLowerCase().includes('qwen');

      this.form.patchValue(
        {
          model: deployedModel.apiModelName,
          baseUrl: '',
          apiKey: 'sk-no-key-required',
          internalName: deployedModel.internalName,
          namespace: deployedModel.deployedNamespace,
          tokenizer,
          maxTokens,
          temperature: 0.7,
          topP: 0.8,
          topK: 20,
          minP: 0.0,
          presencePenalty: 1.0,
          enableThinking: isQwen ? false : null,
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
      this.form.patchValue({ ...value, useSavedKey: false }, { emitEvent: false });

      const provider = value.provider as ApiKeyProvider;
      if (provider === ApiKeyProvider.INTERNAL && value.internalName) {
        this.loadDeployedModelByInternalName(value.internalName, value.namespace);
      } else if (provider && LLM_PROVIDERS_WITH_SAVED_KEYS.includes(provider) && !value.apiKey) {
        this.checkAndApplySavedKey(provider);
      }
    } else {
      this.form.reset(
        {
          provider: ApiKeyProvider.OPENAI,
          useSavedKey: false,
          model: '',
          baseUrl: this.defaultUrls[ApiKeyProvider.OPENAI],
          apiKey: '',
          deployedModel: null,
        },
        { emitEvent: false },
      );
      this.checkAndApplySavedKey(ApiKeyProvider.OPENAI);
    }
  }

  private checkAndApplySavedKey(provider: ApiKeyProvider): void {
    this.userApiKeyService.hasKey(provider, 'LLM').subscribe({
      next: exists => {
        this.form.patchValue({ useSavedKey: exists, apiKey: '' });
      },
      error: () => {
        this.form.patchValue({ useSavedKey: false });
      },
    });
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
