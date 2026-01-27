// src/app/features/evaluation/services/evaluation-form.service.ts
import { inject, Injectable } from '@angular/core';
import { FormArray, FormControl, FormGroup, Validators } from '@angular/forms';
import { ITaskRunParam } from '../../../shared/model/tasks.model';
import { IApplication } from '../../../shared/model/application.model';
import { LLMProviderConfig } from '../../../shared/components/llm-provider-config/llm-provider-config.component';
import { TaskRunType } from '../../../shared/model/enum/task-run-type.model';
import { TaskRunProvisioningStatus } from '../../../shared/model/enum/task-run-provision-status.model';
import { ApplicationService } from '../../../shared/service/application.service';
import { v4 as uuidv4 } from 'uuid';
import { newDatasetForm } from '../../../shared/components/dataset-table-chooser/dataset-table-chooser.component';
import { RefSelection } from '../../../private/hub/components/ref-selector.component';
import { CONVERSATION_METRICS, PERFORMANCE_METRICS } from '../../../private/training/evaluation/constants/metrics';
import { SECURITY_TESTS } from '../../../private/training/evaluation/constants/security';
import { BENCHMARKS } from '../../../private/training/evaluation/constants/benchmarks';

@Injectable({ providedIn: 'root' })
export class EvaluationFormService {
  private applicationService = inject(ApplicationService);

  createForm(): FormGroup {
    return new FormGroup({
      id: new FormControl<string | null>(null),
      name: new FormControl<string>('', [Validators.required, Validators.minLength(3)]),
      description: new FormControl<string>(''),
      modelUnderTest: new FormControl<LLMProviderConfig | null>(null, Validators.required), // Changed
      judgeConfig: new FormControl<LLMProviderConfig | null>(null),
      useSeparateSimulator: new FormControl<boolean>(false),
      simulatorConfig: new FormControl<LLMProviderConfig | null>(null),
      language: new FormControl<string>('en'),
      project: new FormControl(null),
      benchmarks: new FormArray<FormGroup>([]),
      performanceMetrics: new FormArray<FormGroup>([]),
      qualityMetrics: new FormArray<FormGroup>([]),
      conversationMetrics: new FormArray<FormGroup>([]),
      notify: new FormControl<string[]>([]),
      type: new FormControl<keyof typeof TaskRunType>('EVALUATION'),
      provisioningStatus: new FormControl<keyof typeof TaskRunProvisioningStatus>('CREATED'),
      deployedNamespace: new FormControl<string | null>(null),
      podName: new FormControl<string | null>(null),
      container: new FormControl<string | null>(null),
      internalName: new FormControl<string | null>(null),
      params: new FormControl<ITaskRunParam[] | null>(null),
      securityTests: new FormArray<FormGroup>([]),
      redTeamingConfig: new FormGroup({
        enabled: new FormControl<boolean>(false),
        attacksPerVulnerability: new FormControl<number>(3),
        maxConcurrent: new FormControl<number>(5),
        purpose: new FormControl<string>(''),
      }),
      customEvalDatasets: new FormArray<FormGroup>([]),
    });
  }

  formToParams(formValues: any): ITaskRunParam[] {
    const params: ITaskRunParam[] = [];

    this.addModelUnderTestParams(params, formValues);
    this.addJudgeParams(params, formValues);
    this.addSimulatorParams(params, formValues);
    this.addBasicParams(params, formValues);
    this.addBenchmarkParams(params, formValues);
    this.addMetricParams(params, formValues);
    this.addSecurityParams(params, formValues);
    this.addCustomEvalParams(params, formValues);

    return params;
  }

  private addModelUnderTestParams(params: ITaskRunParam[], formValues: any): void {
    const config = formValues.modelUnderTest as LLMProviderConfig;
    console.log('modelUnderTest config:', JSON.stringify(config, null, 2)); // Debug
    if (!config?.model) return;

    params.push({ key: 'DEPLOYED_MODEL_NAME', value: config.model });
    params.push({
      key: 'DEPLOYED_MODEL_PROVIDER',
      value: config.provider === 'internal' ? 'openai' : config.provider,
    });

    if (config.provider === 'internal') {
      if (config.internalName) params.push({ key: 'DEPLOYED_MODEL_INTERNAL_NAME', value: config.internalName });
      if (config.namespace) params.push({ key: 'DEPLOYED_MODEL_NAMESPACE', value: config.namespace });
      params.push({ key: 'DEPLOYED_MODEL_API', value: 'sk-no-key-required' });
    } else {
      if (config.baseUrl) params.push({ key: 'DEPLOYED_MODEL_BASE_URL', value: config.baseUrl });
      if (config.apiKey) params.push({ key: 'DEPLOYED_MODEL_API', value: config.apiKey });
    }

    if (config.tokenizer) {
      params.push({ key: 'MODEL_TOKENIZER', value: config.tokenizer });
    }

    // Add maxTokens
    if (config.maxTokens) {
      params.push({ key: 'MODEL_MAX_TOKENS', value: String(config.maxTokens) });
    }
  }

  private addJudgeParams(params: ITaskRunParam[], formValues: any): void {
    if (!formValues.judgeConfig?.model) return;

    params.push({ key: 'JUDGE_MODEL', value: formValues.judgeConfig.model });
    params.push({
      key: 'JUDGE_MODEL_PROVIDER',
      value: formValues.judgeConfig.provider === 'internal' ? 'openai' : formValues.judgeConfig.provider,
    });
    if (formValues.judgeConfig.baseUrl) params.push({ key: 'JUDGE_MODEL_BASE_URL', value: formValues.judgeConfig.baseUrl });
    params.push({ key: 'JUDGE_MODEL_API', value: formValues.judgeConfig.apiKey || 'sk-no-key-required' });
    if (formValues.judgeConfig.internalName) params.push({ key: 'JUDGE_MODEL_INTERNAL_NAME', value: formValues.judgeConfig.internalName });
    if (formValues.judgeConfig.namespace) params.push({ key: 'JUDGE_MODEL_NAMESPACE', value: formValues.judgeConfig.namespace });
  }

  private addSimulatorParams(params: ITaskRunParam[], formValues: any): void {
    const config =
      formValues.useSeparateSimulator && formValues.simulatorConfig?.model ? formValues.simulatorConfig : formValues.judgeConfig;

    if (!config?.model) return;

    params.push({ key: 'SIMULATOR_MODEL', value: config.model });
    params.push({ key: 'SIMULATOR_MODEL_PROVIDER', value: config.provider === 'internal' ? 'openai' : config.provider });
    if (config.baseUrl) params.push({ key: 'SIMULATOR_MODEL_BASE_URL', value: config.baseUrl });
    params.push({ key: 'SIMULATOR_MODEL_API', value: config.apiKey || 'sk-no-key-required' });
    if (config.internalName) params.push({ key: 'SIMULATOR_MODEL_INTERNAL_NAME', value: config.internalName });
    if (config.namespace) params.push({ key: 'SIMULATOR_MODEL_NAMESPACE', value: config.namespace });
  }

  private addBasicParams(params: ITaskRunParam[], formValues: any): void {
    if (formValues.name) params.push({ key: 'JOB_NAME', value: formValues.name });
    if (formValues.description) params.push({ key: 'JOB_DESCRIPTION', value: formValues.description });
    params.push({ key: 'USE_GATEWAY', value: String(formValues.useGateway ?? false) });
    params.push({ key: 'NOTIFY', value: Array.isArray(formValues.notify) ? formValues.notify.join(',') : String(formValues.notify ?? '') });
    if (formValues.language) params.push({ key: 'LANGUAGE', value: formValues.language });
  }

  private addBenchmarkParams(params: ITaskRunParam[], formValues: any): void {
    if (!formValues.benchmarks?.length) return;

    params.push({
      key: 'BENCHMARKS',
      value: JSON.stringify(
        formValues.benchmarks.map((b: any) => {
          let selectedTasks = Array.isArray(b.selectedTasks) ? b.selectedTasks : ['All'];
          if (selectedTasks.length > 1 && selectedTasks.includes('All')) {
            selectedTasks = selectedTasks.filter((t: string) => t !== 'All');
          }
          return {
            name: b.name,
            evalScopeName: b.evalScopeName,
            type: b.type,
            selectedTasks,
            shots: b.shots,
            limit: b.limit,
            supportsFewshot: b.supportsFewshot,
            useCustomDataset: b.useCustomDataset || false,
            datasetRepo: b.useCustomDataset ? b.datasetRepo : null,
            datasetRef: b.useCustomDataset ? b.datasetRef?.id || null : null,
            datasetSubset: b.useCustomDataset ? b.datasetSubset || 'default' : null,
          };
        }),
      ),
    });
  }

  private addMetricParams(params: ITaskRunParam[], formValues: any): void {
    if (formValues.performanceMetrics?.length) {
      params.push({
        key: 'PERFORMANCE_METRICS',
        value: JSON.stringify(formValues.performanceMetrics.map((m: any) => ({ name: m.name, threshold: m.threshold }))),
      });
    }
    if (formValues.qualityMetrics?.length) {
      params.push({
        key: 'QUALITY_METRICS',
        value: JSON.stringify(
          formValues.qualityMetrics.map((m: any) => ({
            name: m.name,
            datasetRepo: m.datasetRepo,
            datasetRef: m.datasetRef?.id || null,
            criteria: m.criteria || null,
            limit: m.limit || null,
          })),
        ),
      });
    }
    if (formValues.conversationMetrics?.length) {
      params.push({
        key: 'CONVERSATION_METRICS',
        value: JSON.stringify(
          formValues.conversationMetrics.map((m: any) => ({
            name: m.name,
            datasetRepo: m.datasetRepo,
            datasetRef: m.datasetRef?.id || null,
            configValue: m.configValue,
            configLabel: m.configLabel,
            limit: m.limit || null,
          })),
        ),
      });
    }
  }

  private addSecurityParams(params: ITaskRunParam[], formValues: any): void {
    if (!formValues.securityTests?.length) return;

    params.push({
      key: 'SECURITY_TESTS',
      value: JSON.stringify(
        formValues.securityTests.map((s: any) => ({
          name: s.name,
          evalScopeName: s.evalScopeName,
          category: s.category,
          selectedSubtypes: s.selectedSubtypes,
          attacks: s.attacks,
        })),
      ),
    });

    if (formValues.redTeamingConfig?.enabled) {
      params.push({
        key: 'RED_TEAMING_CONFIG',
        value: JSON.stringify({
          enabled: true,
          attacksPerVulnerability: formValues.redTeamingConfig.attacksPerVulnerability || 3,
          maxConcurrent: formValues.redTeamingConfig.maxConcurrent || 5,
          purpose: formValues.redTeamingConfig.purpose || '',
        }),
      });
    }
  }

  private addCustomEvalParams(params: ITaskRunParam[], formValues: any): void {
    if (!formValues.customEvalDatasets?.length) return;

    params.push({
      key: 'CUSTOM_EVAL_DATASETS',
      value: JSON.stringify(
        formValues.customEvalDatasets.map((d: any) => {
          const evalType = d.evalType || 'exact_match';

          // Build columns based on evalType
          const columns: any = {
            instruction: d.instructionColumn || 'instruction',
            answer: d.answerColumn || 'answer',
          };

          // Only include eval_type column for hybrid mode
          if (evalType === 'hybrid') {
            columns.eval_type = d.evalTypeColumn || 'eval_type';
            columns.judge_criteria = d.judgeCriteriaColumn || 'judge_criteria';
          }
          // Only include judge_criteria column for judge mode
          else if (evalType === 'judge') {
            columns.judge_criteria = d.judgeCriteriaColumn || 'judge_criteria';
          }
          // exact_match: no additional columns

          return {
            name: d.repoId,
            repoId: d.repoId,
            ref: d.ref?.id || null,
            split: d.split || 'test',
            limit: d.samples || null,
            columns,
            promptTemplate: d.usePromptTemplate ? d.promptTemplate : null,
            stopSequences: d.usePromptTemplate ? d.stopSequences : null,
            // Only include defaultJudgeCriteria for judge/hybrid
            defaultJudgeCriteria: evalType !== 'exact_match' ? d.defaultJudgeCriteria : null,
          };
        }),
      ),
    });
  }

  envVarsToForm(form: FormGroup, envVars: ITaskRunParam[]): void {
    const getValue = (key: string) => envVars.find(ev => ev.key === key)?.value;

    const language = getValue('LANGUAGE') || 'en';
    const notify = getValue('NOTIFY') || '';

    // Model under test config
    const modelUnderTest = this.buildLLMConfig(envVars, 'DEPLOYED_MODEL');

    // Judge config
    const judgeConfig = this.buildLLMConfig(envVars, 'JUDGE');
    const simulatorConfig = this.buildLLMConfig(envVars, 'SIMULATOR');
    const useSeparateSimulator = !!getValue('SIMULATOR_MODEL') && getValue('SIMULATOR_MODEL') !== judgeConfig.model;

    form.patchValue({
      modelUnderTest,
      judgeConfig,
      simulatorConfig,
      useSeparateSimulator,
      language,
      notify: typeof notify === 'string' && notify ? notify.split(',') : [],
    });

    // Parse arrays
    this.parseBenchmarks(form, getValue('BENCHMARKS'));
    this.parsePerformance(form, getValue('PERFORMANCE_METRICS'));
    this.parseQuality(form, getValue('QUALITY_METRICS'));
    this.parseConversation(form, getValue('CONVERSATION_METRICS'));
    this.parseSecurityTests(form, getValue('SECURITY_TESTS'));
    this.parseRedTeamingConfig(form, getValue('RED_TEAMING_CONFIG'));
    this.parseCustomEvalDatasets(form, getValue('CUSTOM_EVAL_DATASETS'));
  }

  private buildLLMConfig(envVars: ITaskRunParam[], prefix: string): LLMProviderConfig {
    const getValue = (key: string) => envVars.find(ev => ev.key === key)?.value;
    const internalName = getValue(`${prefix}_INTERNAL_NAME`);
    const namespace = getValue(`${prefix}_NAMESPACE`);
    const baseUrl = getValue(`${prefix}_BASE_URL`) || 'https://api.openai.com/v1';

    // Handle the inconsistent naming for DEPLOYED_MODEL
    const modelKey = prefix === 'DEPLOYED_MODEL' ? `${prefix}_NAME` : `${prefix}_MODEL`;
    const apiKey = prefix === 'DEPLOYED_MODEL' ? `${prefix}_API` : `${prefix}_MODEL_API`;
    const baseUrlKey = prefix === 'DEPLOYED_MODEL' ? `${prefix}_BASE_URL` : `${prefix}_MODEL_BASE_URL`;
    const internalNameKey = prefix === 'DEPLOYED_MODEL' ? `${prefix}_INTERNAL_NAME` : `${prefix}_MODEL_INTERNAL_NAME`;
    const namespaceKey = prefix === 'DEPLOYED_MODEL' ? `${prefix}_NAMESPACE` : `${prefix}_MODEL_NAMESPACE`;
    const maxTokensVal = getValue('MODEL_MAX_TOKENS');

    const internalNameVal = getValue(internalNameKey);
    const namespaceVal = getValue(namespaceKey);
    const baseUrlVal = getValue(baseUrlKey) || 'https://api.openai.com/v1';

    let provider: string;
    if (internalNameVal || namespaceVal) provider = 'internal';
    else if (baseUrlVal.includes('api.openai.com')) provider = 'openai';
    else provider = 'vllm';

    return {
      provider,
      model: getValue(modelKey) || '',
      baseUrl: baseUrlVal,
      apiKey: getValue(apiKey) || '',
      internalName: internalNameVal,
      namespace: namespaceVal,
      tokenizer: getValue('MODEL_TOKENIZER'),
      maxTokens: maxTokensVal ? parseInt(maxTokensVal, 10) : undefined,
    };
  }

  private parseBenchmarks(form: FormGroup, param: string | undefined): void {
    if (!param) return;
    try {
      const benchmarks = JSON.parse(param);
      const arr = form.get('benchmarks') as FormArray;
      arr.clear();
      benchmarks.forEach((b: any) => {
        const def = BENCHMARKS.find(bb => bb.name === b.name) || SECURITY_TESTS.find(ss => ss.name === b.name);
        const tasks = (def as any)?.tasks || [{ label: 'All', value: 'All' }];
        arr.push(
          new FormGroup({
            name: new FormControl(b.name),
            evalScopeName: new FormControl(b.evalScopeName || (def as any)?.evalScopeName),
            type: new FormControl(b.type),
            tasks: new FormControl(tasks),
            selectedTasks: new FormControl(b.selectedTasks || ['All']),
            shots: new FormControl(b.shots ?? (def as any)?.defaultShots ?? 0),
            limit: new FormControl(b.limit || null),
            supportsFewshot: new FormControl(b.supportsFewshot ?? (def as any)?.supportsFewshot ?? true),
            useCustomDataset: new FormControl<boolean>(b.useCustomDataset || false),
            datasetRepo: new FormControl<string | null>(b.datasetRepo || null),
            datasetRef: new FormControl<RefSelection | null>(b.datasetRef ? { id: b.datasetRef, type: 'branch' } : null),
            datasetSubset: new FormControl<string>(b.datasetSubset || 'default'),
          }),
        );
      });
    } catch (e) {
      console.warn('Benchmarks parse error', e);
    }
  }

  private parsePerformance(form: FormGroup, param: string | undefined): void {
    if (!param) return;
    try {
      const metrics = JSON.parse(param);
      const arr = form.get('performanceMetrics') as FormArray;
      arr.clear();
      metrics.forEach((m: any) => {
        const def = PERFORMANCE_METRICS.find(p => p.name === m.name);
        arr.push(
          new FormGroup({
            name: new FormControl(m.name),
            type: new FormControl('performance'),
            threshold: new FormControl(m.threshold || def?.thresholdDefault || 0),
          }),
        );
      });
    } catch (e) {
      console.warn('Performance metrics parse error', e);
    }
  }

  private parseQuality(form: FormGroup, param: string | undefined): void {
    if (!param) return;
    try {
      const metrics = JSON.parse(param);
      const arr = form.get('qualityMetrics') as FormArray;
      arr.clear();
      metrics.forEach((m: any) => {
        arr.push(
          new FormGroup({
            name: new FormControl(m.name),
            type: new FormControl('quality'),
            datasetRepo: new FormControl<string | null>(m.datasetRepo || null, Validators.required),
            datasetRef: new FormControl<RefSelection | null>(
              m.datasetRef ? { id: m.datasetRef, type: 'branch' } : null,
              Validators.required,
            ),
            criteria: new FormControl(m.criteria || ''),
            limit: new FormControl<number | null>(m.limit || null),
          }),
        );
      });
    } catch (e) {
      console.warn('Quality metrics parse error', e);
    }
  }

  private parseConversation(form: FormGroup, param: string | undefined): void {
    if (!param) return;
    try {
      const metrics = JSON.parse(param);
      const arr = form.get('conversationMetrics') as FormArray;
      arr.clear();
      metrics.forEach((m: any) => {
        const def = CONVERSATION_METRICS.find(c => c.name === m.name);
        arr.push(
          new FormGroup({
            name: new FormControl(m.name),
            type: new FormControl('conversation'),
            datasetRepo: new FormControl<string | null>(m.datasetRepo || null, Validators.required),
            datasetRef: new FormControl<RefSelection | null>(
              m.datasetRef ? { id: m.datasetRef, type: 'branch' } : null,
              Validators.required,
            ),
            hasConfig: new FormControl<boolean>(def?.hasConfig || false),
            configValue: new FormControl<number | null>(m.configValue || def?.configDefault || null),
            configLabel: new FormControl<string>(def?.configLabel || ''),
            limit: new FormControl<number | null>(m.limit || null),
          }),
        );
      });
    } catch (e) {
      console.warn('Conversation metrics parse error', e);
    }
  }

  private parseSecurityTests(form: FormGroup, param: string | undefined): void {
    if (!param) return;
    try {
      const securityTests = JSON.parse(param);
      const arr = form.get('securityTests') as FormArray;
      arr.clear();
      securityTests.forEach((s: any) => {
        const def = SECURITY_TESTS.find(sec => sec.evalScopeName === s.evalScopeName || sec.name === s.name);
        arr.push(
          new FormGroup({
            name: new FormControl(s.name),
            evalScopeName: new FormControl(s.evalScopeName),
            category: new FormControl(s.category),
            subtypesOptions: new FormControl(def?.subtypes || []),
            selectedSubtypes: new FormControl(s.selectedSubtypes || []),
            attacks: new FormControl(s.attacks || ['prompt_injection', 'roleplay', 'prompt_probing']),
          }),
        );
      });
    } catch (e) {
      console.warn('Security tests parse error', e);
    }
  }

  private parseRedTeamingConfig(form: FormGroup, param: string | undefined): void {
    if (!param) return;
    try {
      const config = JSON.parse(param);
      (form.get('redTeamingConfig') as FormGroup).patchValue({
        enabled: config.enabled ?? false,
        attacksPerVulnerability: config.attacksPerVulnerability ?? 3,
        maxConcurrent: config.maxConcurrent ?? 5,
        purpose: config.purpose ?? '',
      });
    } catch (e) {
      console.warn('Red teaming config parse error', e);
    }
  }

  private parseCustomEvalDatasets(form: FormGroup, param: string | undefined): void {
    if (!param) return;
    try {
      const datasets = JSON.parse(param);
      const arr = form.get('customEvalDatasets') as FormArray;
      arr.clear();
      datasets.forEach((d: any) => {
        const fg = newDatasetForm(uuidv4(), 'custom_evaluation', 'eval');
        fg.patchValue({
          name: d.name,
          repoId: d.repoId,
          ref: d.ref ? { id: d.ref, type: 'branch' } : null,
          split: d.split || 'test',
          samples: d.limit?.toString() || null,
          instructionColumn: d.columns?.instruction || 'instruction',
          answerColumn: d.columns?.answer || 'answer',
          evalTypeColumn: d.columns?.eval_type || 'eval_type',
          judgeCriteriaColumn: d.columns?.judge_criteria || 'judge_criteria',
          defaultJudgeCriteria:
            d.defaultJudgeCriteria || 'Evaluate if the response correctly answers the question based on the expected answer.',
        });
        arr.push(fg);
      });
    } catch (e) {
      console.warn('Custom eval datasets parse error', e);
    }
  }
}
