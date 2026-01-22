import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { PageComponent } from '../../../shared/components/page/page.component';
import { PageLoadComponent } from '../../../shared/components/page-load/page-load.component';
import { CardModule } from 'primeng/card';
import { CardComponent } from '../../../shared/components/card/card.component';
import { CheckboxModule } from 'primeng/checkbox';
import { DropdownModule } from 'primeng/dropdown';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { ApplicationService } from '../../../shared/service/application.service';
import { IApplication } from '../../../shared/model/application.model';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { LabelTooltipComponent } from '../../../shared/components/label-tooltip/label-tooltip.component';
import {
  Activity,
  ArrowRight,
  Beaker,
  Bot,
  ClipboardList,
  Database,
  Flame,
  GalleryVerticalEnd,
  Gauge,
  Info,
  LucideAngularModule,
  MessageSquare,
  Save,
  Settings,
  ShieldCheck,
  Sparkles,
  Trash,
  Zap
} from 'lucide-angular';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import {
  AbstractControl,
  FormArray,
  FormControl,
  FormGroup,
  FormsModule,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import {
  BASE_RUN_NAME,
  EVA_PII_LEAKAGE,
  EVA_PROMPT_LEAKAGE,
  EVAL_ARC,
  EVAL_BIAS,
  EVAL_BIG_BENCH_HARD,
  EVAL_COHERENCE,
  EVAL_COMMONSENSEQA,
  EVAL_COMPETITION,
  EVAL_CONTEXT_RETENTION,
  EVAL_CONV_COHERENCE,
  EVAL_CONV_QUALITY,
  EVAL_CONVERSATION_DATASET,
  EVAL_CORRECTNESS,
  EVAL_CUSTOM_CRITERIA,
  EVAL_DROP,
  EVAL_GRAPHIC_CONTENT,
  EVAL_GSM_8K,
  EVAL_HELLASWAG,
  EVAL_HUMANEVAL,
  EVAL_IFEVAL,
  EVAL_ILLEGAL_ACTIVITY,
  EVAL_INTELLECTUAL_PROPERTY,
  EVAL_JUDGE_API_KEY,
  EVAL_JUDGE_URL,
  EVAL_LANGUAGE,
  EVAL_LATENCY,
  EVAL_LOGIQA,
  EVAL_MATH_QA,
  EVAL_MBPP,
  EVAL_MISINFORMATION,
  EVAL_MMLU,
  EVAL_MODEL_REPOSITORY,
  EVAL_PERSONAL_SAFETY,
  EVAL_PIQA,
  EVAL_PUBMEDQA,
  EVAL_QUALITY_DATASET,
  EVAL_RACE,
  EVAL_RELEVANCE,
  EVAL_SCIQ,
  EVAL_SHOTS,
  EVAL_SIQA,
  EVAL_TASKS,
  EVAL_THROUGHPUT,
  EVAL_TOKEN_SPEED,
  EVAL_TOXICITY,
  EVAL_TRIVIAQA,
  EVAL_TRUTHFUL_QA,
  EVAL_TURN_ANALYSIS,
  EVAL_USE_GATEWAY_MODEL,
  EVAL_WINNOGRANDE
} from '../tooltips';
import { ButtonDirective } from 'primeng/button';
import { NgForOf, NgIf, NgTemplateOutlet } from '@angular/common';
import { MMLU_TASKS } from './mmlu';
import { BIG_BENCH_HARD_TASKS } from './bigbenchhard';
import { MultiSelectModule } from 'primeng/multiselect';
import { RefSelection, RefSelectorComponent } from '../../hub/components/ref-selector.component';
import { RepoSelectorComponent } from '../../../shared/components/repo-selector/repo-selector.component';
import { EvaluationJobService } from '../../../shared/service/evaluation-job.service';
import { ActivatedRoute, Router } from '@angular/router';
import { revalidateForm } from '../../../shared/util/form.util';
import { displayError, displayErrorAndRethrow } from '../../../shared/util/error.util';
import { Store } from '@ngxs/store';
import { injectParams } from 'ngxtension/inject-params';
import { TaskRunType } from '../../../shared/model/enum/task-run-type.model';
import { TaskRunProvisioningStatus } from '../../../shared/model/enum/task-run-provision-status.model';
import { lastValueFrom, of } from 'rxjs';
import { v4 as uuidv4 } from 'uuid';
import { TaskRunService } from '../../../shared/service/task-run.service';
import { ITaskRun, ITaskRunParam } from '../../../shared/model/tasks.model';
import { derivedAsync } from 'ngxtension/derived-async';
import { catchError, tap } from 'rxjs/operators';
import { LayoutService } from '../../../shared/service/theme/app-layout.service';
import { displaySuccess } from '../../../shared/util/success.util';
import { AccountService } from '../../../shared/service/account.service';
import {
  DeployedModelSelectorComponent
} from '../../../shared/components/deployed-model-selector/deployed-model-selector.component';
import { EvaluationResultsComponent } from '../../../shared/components/evaluation-results/evaluation-results.component';
import { MessageModule } from 'primeng/message';
import { DividerModule } from 'primeng/divider';
import { TooltipModule } from 'primeng/tooltip';
import {
  LLMProviderConfig,
  LlmProviderConfigComponent
} from '../../../shared/components/llm-provider-config/llm-provider-config.component';
import {
  DatasetTableChooserComponent,
  newDatasetForm,
} from '../../../shared/components/dataset-table-chooser/dataset-table-chooser.component';

@Component({
  standalone: true,
  selector: 'sm-evaluation-page',
  templateUrl: './evaluation.page.html',
  imports: [
    PageComponent,
    PageLoadComponent,
    CardModule,
    CardComponent,
    CheckboxModule,
    DropdownModule,
    InputNumberModule,
    InputTextModule,
    InputTextareaModule,
    LabelTooltipComponent,
    LucideAngularModule,
    TableModule,
    TagModule,
    FormsModule,
    ButtonDirective,
    NgTemplateOutlet,
    NgForOf,
    MultiSelectModule,
    RefSelectorComponent,
    ReactiveFormsModule,
    RepoSelectorComponent,
    NgIf,
    DeployedModelSelectorComponent,
    EvaluationResultsComponent,
    MessageModule,
    DividerModule,
    TooltipModule,
    LlmProviderConfigComponent,
    DatasetTableChooserComponent,
  ],
  styles: [
    `
      ::ng-deep .p-datatable .p-datatable-thead > tr > th {
        background-color: var(--surface-50);
      }

      ::ng-deep p-checkbox .p-checkbox-label {
        font-weight: 400;
      }
    `,
  ],
})
export class EvaluationPage implements OnInit {
  welcomeItems = [
    { title: 'What is Surogate ?', link: 'https://surogate.ai/' },
    { title: 'Evaluate models', link: 'https://surogate.ai/' },
  ];

  languages = [
    { name: 'Romanian', code: 'ro' },
    { name: 'English', code: 'en' },
  ];

  // ============================================================================
  // ACCURACY BENCHMARKS
  // ============================================================================
  benchmarks: any[] = [
    {
      img: Beaker,
      name: 'MMLU',
      evalScopeName: 'mmlu',
      type: 'accuracy',
      tooltip: EVAL_MMLU,
      tasks: MMLU_TASKS,
      defaultShots: 5,
      supportsFewshot: true,
    },
    {
      img: Beaker,
      name: 'HellaSwag',
      evalScopeName: 'hellaswag',
      type: 'accuracy',
      tooltip: EVAL_HELLASWAG,
      tasks: null,
      defaultShots: 0,
      supportsFewshot: false,
    },
    {
      img: Beaker,
      name: 'BIG-Bench Hard',
      evalScopeName: 'bbh',
      type: 'accuracy',
      tooltip: EVAL_BIG_BENCH_HARD,
      tasks: BIG_BENCH_HARD_TASKS,
      defaultShots: 3,
      supportsFewshot: true,
    },
    {
      img: Beaker,
      name: 'DROP',
      evalScopeName: 'drop',
      type: 'accuracy',
      tooltip: EVAL_DROP,
      tasks: null,
      defaultShots: 3,
      supportsFewshot: true,
    },
    {
      img: Beaker,
      name: 'TruthfulQA',
      evalScopeName: 'truthfulqa',
      type: 'accuracy',
      tooltip: EVAL_TRUTHFUL_QA,
      tasks: null,
      defaultShots: 0,
      supportsFewshot: false,
    },
    {
      img: Beaker,
      name: 'IFEval',
      evalScopeName: 'ifeval',
      type: 'accuracy',
      tooltip: EVAL_IFEVAL,
      tasks: null,
      defaultShots: 0,
      supportsFewshot: false,
    },
    {
      img: Beaker,
      name: 'GSM8K',
      evalScopeName: 'gsm8k',
      type: 'accuracy',
      tooltip: EVAL_GSM_8K,
      tasks: null,
      defaultShots: 5,
      supportsFewshot: true,
    },
    {
      img: Beaker,
      name: 'MathQA',
      evalScopeName: 'mathqa',
      type: 'accuracy',
      tooltip: EVAL_MATH_QA,
      tasks: null,
      defaultShots: 0,
      supportsFewshot: false,
      defaultLimit: 100,
    },
    {
      img: Beaker,
      name: 'LogiQA',
      evalScopeName: 'logiqa',
      type: 'accuracy',
      tooltip: EVAL_LOGIQA,
      tasks: null,
      defaultShots: 1,
      supportsFewshot: true,
    },
    {
      img: Beaker,
      name: 'ARC',
      evalScopeName: 'arc',
      type: 'accuracy',
      tooltip: EVAL_ARC,
      tasks: null,
      defaultShots: 25,
      supportsFewshot: true,
    },
    {
      img: Beaker,
      name: 'Winogrande',
      evalScopeName: 'winogrande',
      type: 'accuracy',
      tooltip: EVAL_WINNOGRANDE,
      tasks: null,
      defaultShots: 0,
      supportsFewshot: false,
    },
    {
      img: Beaker,
      name: 'HumanEval',
      evalScopeName: 'humaneval',
      type: 'coding',
      tooltip: EVAL_HUMANEVAL,
      tasks: null,
      defaultShots: 0,
      supportsFewshot: false,
    },
    {
      img: Beaker,
      name: 'MBPP',
      evalScopeName: 'mbpp',
      type: 'coding',
      tooltip: EVAL_MBPP,
      tasks: null,
      defaultShots: 0,
      supportsFewshot: false,
    },
    {
      img: Beaker,
      name: 'PIQA',
      evalScopeName: 'piqa',
      type: 'accuracy',
      tooltip: EVAL_PIQA,
      tasks: null,
      defaultShots: 0,
      supportsFewshot: true,
    },
    {
      img: Beaker,
      name: 'SIQA',
      evalScopeName: 'siqa',
      type: 'accuracy',
      tooltip: EVAL_SIQA,
      tasks: null,
      defaultShots: 0,
      supportsFewshot: true,
    },
    {
      img: Beaker,
      name: 'CommonsenseQA',
      evalScopeName: 'commonsenseqa',
      type: 'accuracy',
      tooltip: EVAL_COMMONSENSEQA,
      tasks: null,
      defaultShots: 0,
      supportsFewshot: true,
    },
    {
      img: Beaker,
      name: 'TriviaQA',
      evalScopeName: 'triviaqa',
      type: 'accuracy',
      tooltip: EVAL_TRIVIAQA,
      tasks: null,
      defaultShots: 0,
      supportsFewshot: true,
      minContextLength: 16000,
    },
    {
      img: Beaker,
      name: 'RACE',
      evalScopeName: 'race',
      type: 'accuracy',
      tooltip: EVAL_RACE,
      tasks: null,
      defaultShots: 0,
      supportsFewshot: true,
    },
    {
      img: Beaker,
      name: 'PubMedQA',
      evalScopeName: 'pubmedqa',
      type: 'accuracy',
      tooltip: EVAL_PUBMEDQA,
      tasks: null,
      defaultShots: 0,
      supportsFewshot: true,
    },
    {
      img: Beaker,
      name: 'SciQ',
      evalScopeName: 'sciq',
      type: 'accuracy',
      tooltip: EVAL_SCIQ,
      tasks: null,
      defaultShots: 0,
      supportsFewshot: true,
    },
  ];
  // ============================================================================
  // SECURITY BENCHMARKS
  // ============================================================================
  security: any[] = [
    // ============================================================================
    // DATA PRIVACY
    // ============================================================================
    {
      img: Beaker,
      name: 'PII Leakage',
      evalScopeName: 'pii_leakage',
      type: 'data privacy',
      tooltip: EVA_PII_LEAKAGE,
      subtypes: [
        { label: 'API & Database Access', value: 'api_and_database_access' },
        { label: 'Direct Disclosure', value: 'direct_disclosure' },
        { label: 'Session Leak', value: 'session_leak' },
        { label: 'Social Manipulation', value: 'social_manipulation' },
      ],
    },
    {
      img: Beaker,
      name: 'Prompt Leakage',
      evalScopeName: 'prompt_leakage',
      type: 'data privacy',
      tooltip: EVA_PROMPT_LEAKAGE,
      subtypes: [
        { label: 'Secrets & Credentials', value: 'secrets_and_credentials' },
        { label: 'Instructions', value: 'instructions' },
        { label: 'Guard Exposure', value: 'guard_exposure' },
        { label: 'Permissions & Roles', value: 'permissions_and_roles' },
      ],
    },

    // ============================================================================
    // RESPONSIBLE AI
    // ============================================================================
    {
      img: Beaker,
      name: 'Bias',
      evalScopeName: 'bias',
      type: 'responsible ai',
      tooltip: EVAL_BIAS,
      subtypes: [
        { label: 'Religion', value: 'religion' },
        { label: 'Politics', value: 'politics' },
        { label: 'Gender', value: 'gender' },
        { label: 'Race', value: 'race' },
      ],
    },
    {
      img: Beaker,
      name: 'Toxicity',
      evalScopeName: 'toxicity',
      type: 'responsible ai',
      tooltip: EVAL_TOXICITY,
      subtypes: [
        { label: 'Profanity', value: 'profanity' },
        { label: 'Insults', value: 'insults' },
        { label: 'Threats', value: 'threats' },
        { label: 'Mockery', value: 'mockery' },
      ],
    },
    {
      img: Beaker,
      name: 'Fairness',
      evalScopeName: 'fairness',
      type: 'responsible ai',
      tooltip: 'Tests for fairness and equal treatment across different groups',
      subtypes: [
        { label: 'Equality & Consistency', value: 'equality_consistency' },
        { label: 'Procedural Opportunity', value: 'procedural_opportunity' },
        { label: 'Temporal Outcome', value: 'temporal_outcome' },
      ],
    },
    {
      img: Beaker,
      name: 'Ethics',
      evalScopeName: 'ethics',
      type: 'responsible ai',
      tooltip: 'Tests for ethical behavior and moral integrity',
      subtypes: [
        { label: 'Moral Integrity', value: 'moral_integrity' },
        { label: 'Responsible Transparency', value: 'responsible_transparency' },
        { label: 'Harm Prevention', value: 'harm_prevention' },
      ],
    },

    // ============================================================================
    // SAFETY
    // ============================================================================
    {
      img: Beaker,
      name: 'Illegal Activity',
      evalScopeName: 'illegal_activity',
      type: 'safety',
      tooltip: EVAL_ILLEGAL_ACTIVITY,
      subtypes: [
        { label: 'Weapons', value: 'weapons' },
        { label: 'Illegal Drugs', value: 'illegal_drugs' },
        { label: 'Violent Crimes', value: 'violent_crimes' },
        { label: 'Non-Violent Crimes', value: 'non_violent_crimes' },
        { label: 'Sex Crimes', value: 'sex_crimes' },
        { label: 'Cybercrime', value: 'cybercrime' },
        { label: 'Child Exploitation', value: 'child_exploitation' },
      ],
    },
    {
      img: Beaker,
      name: 'Graphic Content',
      evalScopeName: 'graphic_content',
      type: 'safety',
      tooltip: EVAL_GRAPHIC_CONTENT,
      subtypes: [
        { label: 'Sexual Content', value: 'sexual_content' },
        { label: 'Graphic Content', value: 'graphic_content' },
        { label: 'Pornographic Content', value: 'pornographic_content' },
      ],
    },
    {
      img: Beaker,
      name: 'Personal Safety',
      evalScopeName: 'personal_safety',
      type: 'safety',
      tooltip: EVAL_PERSONAL_SAFETY,
      subtypes: [
        { label: 'Bullying', value: 'bullying' },
        { label: 'Self Harm', value: 'self_harm' },
        { label: 'Unsafe Practices', value: 'unsafe_practices' },
        { label: 'Dangerous Challenges', value: 'dangerous_challenges' },
        { label: 'Stalking', value: 'stalking' },
      ],
    },
    {
      img: Beaker,
      name: 'Child Protection',
      evalScopeName: 'child_protection',
      type: 'safety',
      tooltip: 'Tests for content harmful to minors and child safety violations',
      subtypes: [
        { label: 'Age Verification', value: 'age_verification' },
        { label: 'Data Privacy', value: 'data_privacy' },
        { label: 'Exposure & Interaction', value: 'exposure_interaction' },
      ],
    },

    // ============================================================================
    // BUSINESS
    // ============================================================================
    {
      img: Beaker,
      name: 'Misinformation',
      evalScopeName: 'misinformation',
      type: 'business',
      tooltip: EVAL_MISINFORMATION,
      subtypes: [
        { label: 'Factual Errors', value: 'factual_errors' },
        { label: 'Unsupported Claims', value: 'unsupported_claims' },
        { label: 'Expertise Misrepresentation', value: 'expertize_misrepresentation' },
      ],
    },
    {
      img: Beaker,
      name: 'Intellectual Property',
      evalScopeName: 'intellectual_property',
      type: 'business',
      tooltip: EVAL_INTELLECTUAL_PROPERTY,
      subtypes: [
        { label: 'Imitation', value: 'imitation' },
        { label: 'Copyright Violations', value: 'copyright_violations' },
        { label: 'Trademark Infringement', value: 'trademark_infringement' },
        { label: 'Patent Disclosure', value: 'patent_disclosure' },
      ],
    },
    {
      img: Beaker,
      name: 'Competition',
      evalScopeName: 'competition',
      type: 'business',
      tooltip: EVAL_COMPETITION,
      subtypes: [
        { label: 'Competitor Mention', value: 'competitor_mention' },
        { label: 'Market Manipulation', value: 'market_manipulation' },
        { label: 'Discreditation', value: 'discreditation' },
        { label: 'Confidential Strategies', value: 'confidential_strategies' },
      ],
    },

    // ============================================================================
    // SECURITY
    // ============================================================================
    {
      img: Beaker,
      name: 'BFLA',
      evalScopeName: 'bfla',
      type: 'security',
      tooltip: 'Broken Function Level Authorization - Tests for unauthorized function access',
      subtypes: [
        { label: 'Privilege Escalation', value: 'privilege_escalation' },
        { label: 'Function Bypass', value: 'function_bypass' },
        { label: 'Authorization Bypass', value: 'authorization_bypass' },
      ],
    },
    {
      img: Beaker,
      name: 'BOLA',
      evalScopeName: 'bola',
      type: 'security',
      tooltip: 'Broken Object Level Authorization - Tests for unauthorized object access',
      subtypes: [
        { label: 'Object Access Bypass', value: 'object_access_bypass' },
        { label: 'Cross-Customer Access', value: 'cross_customer_access' },
        { label: 'Unauthorized Object Manipulation', value: 'unauthorized_object_manipulation' },
      ],
    },
    {
      img: Beaker,
      name: 'RBAC',
      evalScopeName: 'rbac',
      type: 'security',
      tooltip: 'Role-Based Access Control - Tests for role and permission bypasses',
      subtypes: [
        { label: 'Role Bypass', value: 'role_bypass' },
        { label: 'Privilege Escalation', value: 'privilege_escalation' },
        { label: 'Unauthorized Role Assumption', value: 'unauthorized_role_assumption' },
      ],
    },
    {
      img: Beaker,
      name: 'Debug Access',
      evalScopeName: 'debug_access',
      type: 'security',
      tooltip: 'Tests for unauthorized access to debug and development features',
      subtypes: [
        { label: 'Debug Mode Bypass', value: 'debug_mode_bypass' },
        { label: 'Development Endpoint Access', value: 'development_endpoint_access' },
        { label: 'Administrative Interface Exposure', value: 'administrative_interface_exposure' },
      ],
    },
    {
      img: Beaker,
      name: 'Shell Injection',
      evalScopeName: 'shell_injection',
      type: 'security',
      tooltip: 'Tests for command injection and shell escape vulnerabilities',
      subtypes: [
        { label: 'Command Injection', value: 'command_injection' },
        { label: 'System Command Execution', value: 'system_command_execution' },
        { label: 'Shell Escape Sequences', value: 'shell_escape_sequences' },
      ],
    },
    {
      img: Beaker,
      name: 'SQL Injection',
      evalScopeName: 'sql_injection',
      type: 'security',
      tooltip: 'Tests for SQL injection vulnerabilities',
      subtypes: [
        { label: 'Blind SQL Injection', value: 'blind_sql_injection' },
        { label: 'Union-Based Injection', value: 'union_based_injection' },
        { label: 'Error-Based Injection', value: 'error_based_injection' },
      ],
    },
    {
      img: Beaker,
      name: 'SSRF',
      evalScopeName: 'ssrf',
      type: 'security',
      tooltip: 'Server-Side Request Forgery - Tests for unauthorized server-side requests',
      subtypes: [
        { label: 'Internal Service Access', value: 'internal_service_access' },
        { label: 'Cloud Metadata Access', value: 'cloud_metadata_access' },
        { label: 'Port Scanning', value: 'port_scanning' },
      ],
    },

    // ============================================================================
    // AGENTIC
    // ============================================================================
    {
      img: Beaker,
      name: 'Goal Theft',
      evalScopeName: 'goal_theft',
      type: 'agentic',
      tooltip: 'Tests for extraction of agent objectives and mission parameters',
      subtypes: [
        { label: 'Escalating Probing', value: 'escalating_probing' },
        { label: 'Cooperative Dialogue', value: 'cooperative_dialogue' },
        { label: 'Social Engineering', value: 'social_engineering' },
      ],
    },
    {
      img: Beaker,
      name: 'Recursive Hijacking',
      evalScopeName: 'recursive_hijacking',
      type: 'agentic',
      tooltip: 'Tests for goal modification through recursive objective chaining',
      subtypes: [
        { label: 'Self-Modifying Goals', value: 'self_modifying_goals' },
        { label: 'Recursive Objective Chaining', value: 'recursive_objective_chaining' },
        { label: 'Goal Propagation Attacks', value: 'goal_propagation_attacks' },
      ],
    },
    {
      img: Beaker,
      name: 'Robustness',
      evalScopeName: 'robustness',
      type: 'agentic',
      tooltip: 'Tests for agent robustness against adversarial inputs',
      subtypes: [
        { label: 'Input Overreliance', value: 'input_overreliance' },
        { label: 'Hijacking', value: 'hijacking' },
      ],
    },
    {
      img: Beaker,
      name: 'Excessive Agency',
      evalScopeName: 'excessive_agency',
      type: 'agentic',
      tooltip: 'Tests for agents taking unauthorized actions beyond scope',
      subtypes: [
        { label: 'Functionality', value: 'functionality' },
        { label: 'Permissions', value: 'permissions' },
        { label: 'Autonomy', value: 'autonomy' },
      ],
    },
  ];

  attacks: any[] = [
    { label: 'Prompt Injection', value: 'prompt_injection' },
    { label: 'Prompt Probing', value: 'prompt_probing' },
    { label: 'Roleplay', value: 'roleplay' },
    { label: 'Gray Box', value: 'gray_box' },
    { label: 'Math Problem', value: 'math_problem' },
    { label: 'Multilingual', value: 'multilingual' },
    { label: 'Base64', value: 'base64' },
    { label: 'Leetspeak', value: 'leetspeak' },
    { label: 'ROT13', value: 'rot13' },
    { label: 'Linear Jailbreaking', value: 'linear_jailbreaking' },
    { label: 'Tree Jailbreaking', value: 'tree_jailbreaking' },
    { label: 'Crescendo', value: 'crescendo_jailbreaking' },
  ];

  // ============================================================================
  // PERFORMANCE METRICS
  // ============================================================================
  performance: any[] = [
    {
      img: Zap,
      name: 'Latency',
      type: 'performance',
      tooltip: EVAL_LATENCY,
      hasThreshold: true,
      thresholdLabel: 'Max (ms)',
      thresholdDefault: 8000,
    },
    {
      img: Gauge,
      name: 'Token Generation Speed',
      type: 'performance',
      tooltip: EVAL_TOKEN_SPEED,
      hasThreshold: true,
      thresholdLabel: 'Min tokens/sec',
      thresholdDefault: 15,
    },
    {
      img: Activity,
      name: 'Throughput',
      type: 'performance',
      tooltip: EVAL_THROUGHPUT,
      hasThreshold: true,
      thresholdLabel: 'Min req/sec',
      thresholdDefault: 0.3,
    },
  ];

  // ============================================================================
  // QUALITY METRICS
  // ============================================================================
  quality: any[] = [
    { img: Sparkles, name: 'Correctness', type: 'quality', tooltip: EVAL_CORRECTNESS, tasks: ['All'] },
    { img: Sparkles, name: 'Relevance', type: 'quality', tooltip: EVAL_RELEVANCE, tasks: ['All'] },
    { img: Sparkles, name: 'Coherence', type: 'quality', tooltip: EVAL_COHERENCE, tasks: ['All'] },
  ];

  // ============================================================================
  // CONVERSATION METRICS
  // ============================================================================
  conversation: any[] = [
    {
      img: MessageSquare,
      name: 'Conversation Quality',
      type: 'conversation',
      tooltip: EVAL_CONV_QUALITY,
    },
    {
      img: MessageSquare,
      name: 'Conversation Coherence',
      type: 'conversation',
      tooltip: EVAL_CONV_COHERENCE,
      hasConfig: true,
      configLabel: 'Window Size',
      configTooltip: 'Number of recent turns to check for logical flow (default: 3)',
      configDefault: 3,
    },
    {
      img: MessageSquare,
      name: 'Context Retention',
      type: 'conversation',
      tooltip: EVAL_CONTEXT_RETENTION,
      hasConfig: true,
      configLabel: 'Threshold',
      configTooltip: 'Minimum % of key information the model must retain from earlier turns (0.0-1.0, default: 0.7)',
      configDefault: 0.7,
    },
    {
      img: MessageSquare,
      name: 'Turn Analysis',
      type: 'conversation',
      tooltip: EVAL_TURN_ANALYSIS,
    },
  ];

  providers = [
    { name: 'OpenAI', code: 'openai' },
    { name: 'Anthropic', code: 'anthropic' },
    { name: 'vLLM', code: 'vllm' },
    { name: 'OpenRouter', code: 'openrouter' },
    { name: 'Azure', code: 'azure' },
    { name: 'Custom', code: 'custom' },
  ];

  advanced = signal(false);
  selected = signal<any[]>([]);
  taskRunId = injectParams('id');

  evaluationJobService = inject(EvaluationJobService);
  route = inject(ActivatedRoute);
  isEditMode = signal(false);
  isSaving = signal(false);
  isLaunching = signal(false);
  taskRunService = inject(TaskRunService);
  layoutService = inject(LayoutService);
  readonly accountService = inject(AccountService);
  readonly router = inject(Router);
  readonly store = inject(Store);
  readonly applicationService = inject(ApplicationService);
  taskForm = new FormGroup({
    id: new FormControl<string | null>(null),
    name: new FormControl<string>('', [Validators.required, Validators.minLength(3)]),
    description: new FormControl<string>(''),
    deployedModel: new FormControl<IApplication | null>(null, Validators.required),
    judgeConfig: new FormControl<LLMProviderConfig | null>(null),
    judgeModel: new FormControl<string>(''),
    judgeModelApi: new FormControl<string>(''),
    judgeModelBaseUrl: new FormControl<string>(''),
    judgeModelProvider: new FormControl<string>('openai'),
    useSeparateSimulator: new FormControl<boolean>(false),
    simulatorConfig: new FormControl<LLMProviderConfig | null>(null),
    simulatorModel: new FormControl<string>(''),
    simulatorModelApi: new FormControl<string>(''),
    simulatorModelBaseUrl: new FormControl<string>(''),
    simulatorModelProvider: new FormControl<string>('openai'),
    useGateway: new FormControl<boolean>(false),
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

  user = derivedAsync(() => this.accountService.identity(true));

  taskRun = derivedAsync(() => {
    if (this.taskRunId()) {
      return this.taskRunService.find(this.taskRunId()).pipe(
        catchError(e => displayErrorAndRethrow(this.store, e)),
        tap(taskRun => {
          console.log('taskRun from API:', taskRun);
          console.log('taskRun.name:', taskRun?.name);
          console.log('taskRun.params:', taskRun?.params);
          this.initForm(taskRun);
        }),
      );
    }
    return of({
      type: TaskRunType.EVALUATION,
      provisioningStatus: TaskRunProvisioningStatus.CREATED,
      project: this.user()?.defaultProject ?? null,
    } as ITaskRun);
  });

  provisioningStatus = computed(() => {
    return this.taskRun()?.provisioningStatus ?? null;
  });

  mustRelaunch = computed(() => {
    return (
      this.provisioningStatus() === TaskRunProvisioningStatus.DEPLOYED ||
      this.provisioningStatus() === TaskRunProvisioningStatus.COMPLETED ||
      this.provisioningStatus() === TaskRunProvisioningStatus.CANCELLED ||
      this.provisioningStatus() === TaskRunProvisioningStatus.ERROR
    );
  });

  ngOnInit() {
    this.layoutService.state.helpItems = this.welcomeItems;
  }

  initForm(taskRun: ITaskRun) {
    this.taskForm.patchValue(taskRun);
    this.envVarsToForm(taskRun.params);
    revalidateForm(this.taskForm);
  }

  getStatusSeverity(): 'success' | 'info' | 'warning' | 'danger' | 'secondary' {
    switch (this.provisioningStatus()) {
      case 'COMPLETED':
        return 'success';
      case 'DEPLOYED':
        return 'info';
      case 'ERROR':
        return 'danger';
      case 'CANCELLED':
        return 'warning';
      default:
        return 'secondary';
    }
  }

  get securityTestsArray(): FormArray {
    return this.taskForm.get('securityTests') as FormArray;
  }

  get redTeamingConfig(): FormGroup {
    return this.taskForm.get('redTeamingConfig') as FormGroup;
  }

  get groupedAttacks() {
    return [
      { label: 'Single-Turn', items: this.attacks.filter(a => a.category === 'single-turn') },
      { label: 'Encoding', items: this.attacks.filter(a => a.category === 'encoding') },
      { label: 'Multi-Turn', items: this.attacks.filter(a => a.category === 'multi-turn') },
    ];
  }

  envVarsToForm(envVars: ITaskRunParam[]) {
    const deployedModelId = envVars.find(ev => ev.key === 'DEPLOYED_MODEL_ID')?.value || '';
    const useGateway = envVars.find(ev => ev.key === 'USE_GATEWAY')?.value === 'true';
    const language = envVars.find(ev => ev.key === 'LANGUAGE')?.value || 'en';
    const notify: string | any[] = envVars.find(ev => ev.key === 'NOTIFY')?.value || [];

    // Judge config
    const judgeInternalName = envVars.find(ev => ev.key === 'JUDGE_MODEL_INTERNAL_NAME')?.value;
    const judgeNamespace = envVars.find(ev => ev.key === 'JUDGE_MODEL_NAMESPACE')?.value;
    const judgeBaseUrl = envVars.find(ev => ev.key === 'JUDGE_MODEL_BASE_URL')?.value || 'https://api.openai.com/v1';
    const customEvalParam = envVars.find(ev => ev.key === 'CUSTOM_EVAL_DATASETS')?.value;

    if (customEvalParam) {
      this.parseCustomEvalDatasetsParam(customEvalParam);
    }

    let judgeProvider: string;
    if (judgeInternalName || judgeNamespace) {
      judgeProvider = 'internal';
    } else if (judgeBaseUrl.includes('api.openai.com')) {
      judgeProvider = 'openai';
    } else {
      judgeProvider = 'vllm';
    }

    const judgeConfig: LLMProviderConfig = {
      provider: judgeProvider,
      model: envVars.find(ev => ev.key === 'JUDGE_MODEL')?.value || '',
      baseUrl: judgeBaseUrl,
      apiKey: envVars.find(ev => ev.key === 'JUDGE_MODEL_API')?.value || '',
      internalName: judgeInternalName,
      namespace: judgeNamespace,
    };

    // Simulator config
    const simulatorInternalName = envVars.find(ev => ev.key === 'SIMULATOR_MODEL_INTERNAL_NAME')?.value;
    const simulatorNamespace = envVars.find(ev => ev.key === 'SIMULATOR_MODEL_NAMESPACE')?.value;
    const simulatorBaseUrl = envVars.find(ev => ev.key === 'SIMULATOR_MODEL_BASE_URL')?.value || 'https://api.openai.com/v1';
    const simulatorModel = envVars.find(ev => ev.key === 'SIMULATOR_MODEL')?.value || '';

    let simulatorProvider: string;
    if (simulatorInternalName || simulatorNamespace) {
      simulatorProvider = 'internal';
    } else if (simulatorBaseUrl.includes('api.openai.com')) {
      simulatorProvider = 'openai';
    } else {
      simulatorProvider = 'vllm';
    }

    const simulatorConfig: LLMProviderConfig = {
      provider: simulatorProvider,
      model: simulatorModel,
      baseUrl: simulatorBaseUrl,
      apiKey: envVars.find(ev => ev.key === 'SIMULATOR_MODEL_API')?.value || '',
      internalName: simulatorInternalName,
      namespace: simulatorNamespace,
    };

    // Determine if separate simulator is used
    const useSeparateSimulator = !!simulatorModel && simulatorModel !== judgeConfig.model;

    let notifyValue: string[] = [];
    if (typeof notify === 'string') {
      notifyValue = notify.split(',');
    }

    this.taskForm.patchValue({
      judgeConfig,
      simulatorConfig,
      useSeparateSimulator,
      useGateway,
      language,
      notify: notifyValue,
    });

    // Load deployed model if ID exists
    if (deployedModelId) {
      this.applicationService.find(deployedModelId).subscribe(response => {
        if (response.body) {
          this.taskForm.patchValue({ deployedModel: response.body });
        }
      });
    }

    // Parse benchmarks
    const benchmarksParam = envVars.find(ev => ev.key === 'BENCHMARKS')?.value;
    if (benchmarksParam) {
      this.parseBenchmarksParam(benchmarksParam);
    }

    // Parse performance metrics
    const performanceParam = envVars.find(ev => ev.key === 'PERFORMANCE_METRICS')?.value;
    if (performanceParam) {
      this.parsePerformanceParam(performanceParam);
    }

    // Parse quality metrics
    const qualityParam = envVars.find(ev => ev.key === 'QUALITY_METRICS')?.value;
    if (qualityParam) {
      this.parseQualityParam(qualityParam);
    }

    // Parse conversation metrics
    const conversationParam = envVars.find(ev => ev.key === 'CONVERSATION_METRICS')?.value;
    if (conversationParam) {
      this.parseConversationParam(conversationParam);
    }

    // Parse security tests
    const securityTestsParam = envVars.find(ev => ev.key === 'SECURITY_TESTS')?.value;
    if (securityTestsParam) {
      this.parseSecurityTestsParam(securityTestsParam);
    }

    // Parse red teaming config
    const redTeamingParam = envVars.find(ev => ev.key === 'RED_TEAMING_CONFIG')?.value;
    if (redTeamingParam) {
      this.parseRedTeamingConfigParam(redTeamingParam);
    }
  }

  addSecurityTest(item: any) {
    const exists = this.securityTestsArray.value.some((s: any) => s.name === item.name);
    if (exists) return;

    // Get all subtype values for default selection
    const allSubtypeValues = item.subtypes.map((s: any) => s.value);

    // Default attacks
    const defaultAttacks = ['prompt_injection', 'roleplay', 'prompt_probing'];

    this.securityTestsArray.push(
      new FormGroup({
        name: new FormControl(item.name),
        evalScopeName: new FormControl(item.evalScopeName),
        category: new FormControl(item.category),
        // Store the full subtypes array for options (label/value objects)
        subtypesOptions: new FormControl(item.subtypes),
        // Store selected values (just the value strings)
        selectedSubtypes: new FormControl(allSubtypeValues),
        // Store selected attack values
        attacks: new FormControl(defaultAttacks),
      }),
    );

    if (this.securityTestsArray.length === 1) {
      this.redTeamingConfig.get('enabled')?.setValue(true);
    }
  }

  removeSecurityTest(name: string) {
    const index = this.securityTestsArray.value.findIndex((s: any) => s.name === name);
    if (index !== -1) {
      this.securityTestsArray.removeAt(index);
    }
    if (this.securityTestsArray.length === 0) {
      this.redTeamingConfig.get('enabled')?.setValue(false);
    }
  }

  parseBenchmarksParam(benchmarksParam: string) {
    try {
      const benchmarks = JSON.parse(benchmarksParam);
      const arr = this.taskForm.get('benchmarks') as FormArray;
      arr.clear();
      benchmarks.forEach((b: any) => {
        const currentBenchmark = this.benchmarks.find(bb => bb.name === b.name) || this.security.find(ss => ss.name === b.name);
        const tasks = currentBenchmark?.tasks || [{ label: 'All', value: 'All' }];

        arr.push(
          new FormGroup({
            name: new FormControl(b.name),
            evalScopeName: new FormControl(b.evalScopeName || currentBenchmark?.evalScopeName),
            type: new FormControl(b.type),
            tasks: new FormControl(tasks),
            selectedTasks: new FormControl(b.selectedTasks || ['All']),
            shots: new FormControl(b.shots ?? currentBenchmark?.defaultShots ?? 0),
            limit: new FormControl(b.limit || null),
            supportsFewshot: new FormControl(b.supportsFewshot ?? currentBenchmark?.supportsFewshot ?? true),
            useCustomDataset: new FormControl<boolean>(b.useCustomDataset || false),
            datasetRepo: new FormControl<string | null>(b.datasetRepo || null),
            datasetRef: new FormControl<RefSelection | null>(b.datasetRef ? { id: b.datasetRef, type: 'branch' } : null),
            datasetSubset: new FormControl<string>(b.datasetSubset || 'default'), // NEW
          }),
        );
      });
    } catch (e) {
      console.warn('Benchmarks parse error', e);
    }
  }

  parsePerformanceParam(param: string) {
    try {
      const metrics = JSON.parse(param);
      const arr = this.performanceMetricsArray;
      arr.clear();
      metrics.forEach((m: any) => {
        const def = this.performance.find(p => p.name === m.name);
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

  parseQualityParam(param: string) {
    try {
      const metrics = JSON.parse(param);
      const arr = this.qualityMetricsArray;
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

  parseSecurityTestsParam(param: string) {
    try {
      const securityTests = JSON.parse(param);
      const arr = this.securityTestsArray;
      arr.clear();
      securityTests.forEach((s: any) => {
        // Find the original security item to get subtypes options
        const securityItem = this.security.find(sec => sec.evalScopeName === s.evalScopeName || sec.name === s.name);

        arr.push(
          new FormGroup({
            name: new FormControl(s.name),
            evalScopeName: new FormControl(s.evalScopeName),
            category: new FormControl(s.category),
            subtypesOptions: new FormControl(securityItem?.subtypes || []),
            selectedSubtypes: new FormControl(s.selectedSubtypes || []),
            attacks: new FormControl(s.attacks || ['prompt_injection', 'roleplay', 'prompt_probing']),
          }),
        );
      });
    } catch (e) {
      console.warn('Security tests parse error', e);
    }
  }

  parseRedTeamingConfigParam(param: string) {
    try {
      const config = JSON.parse(param);
      this.redTeamingConfig.patchValue({
        enabled: config.enabled ?? false,
        attacksPerVulnerability: config.attacksPerVulnerability ?? 3,
        maxConcurrent: config.maxConcurrent ?? 5,
        purpose: config.purpose ?? '',
      });
    } catch (e) {
      console.warn('Red teaming config parse error', e);
    }
  }

  parseConversationParam(param: string) {
    try {
      const metrics = JSON.parse(param);
      const arr = this.conversationMetricsArray;
      arr.clear();
      metrics.forEach((m: any) => {
        const def = this.conversation.find(c => c.name === m.name);
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

  getBenchmarkMinContext(name: string): number | null {
    const benchmark = this.benchmarks.find(b => b.name === name);
    return benchmark?.minContextLength || null;
  }

  formToParams(formValues: any): ITaskRunParam[] {
    const params: ITaskRunParam[] = [];

    // Deployed model params
    if (formValues.deployedModel) {
      params.push({ key: 'DEPLOYED_MODEL_ID', value: formValues.deployedModel.id });

      let modelName = formValues.deployedModel.internalName;
      if (formValues.deployedModel.extraConfig) {
        try {
          const extraConfig =
            typeof formValues.deployedModel.extraConfig === 'string'
              ? JSON.parse(formValues.deployedModel.extraConfig)
              : formValues.deployedModel.extraConfig;
          if (extraConfig.modelName) {
            modelName = extraConfig.modelName;
          }
        } catch (e) {
          console.warn('Failed to parse extraConfig', e);
        }
      }

      params.push({ key: 'DEPLOYED_MODEL_NAME', value: modelName });
      params.push({ key: 'DEPLOYED_MODEL_INTERNAL_NAME', value: formValues.deployedModel.internalName });
      params.push({ key: 'DEPLOYED_MODEL_NAMESPACE', value: formValues.deployedModel.deployedNamespace });
      params.push({ key: 'INGRESS_HOST_NAME', value: formValues.deployedModel.ingressHostName });
    }

    // Judge config
    if (formValues.judgeConfig?.model) {
      params.push({ key: 'JUDGE_MODEL', value: formValues.judgeConfig.model });
      params.push({
        key: 'JUDGE_MODEL_PROVIDER',
        value: formValues.judgeConfig.provider === 'internal' ? 'openai' : formValues.judgeConfig.provider,
      });
      if (formValues.judgeConfig.baseUrl) {
        params.push({ key: 'JUDGE_MODEL_BASE_URL', value: formValues.judgeConfig.baseUrl });
      }
      params.push({
        key: 'JUDGE_MODEL_API',
        value: formValues.judgeConfig.apiKey || 'sk-no-key-required',
      });
      // Add internal model info for service discovery
      if (formValues.judgeConfig.internalName) {
        params.push({ key: 'JUDGE_MODEL_INTERNAL_NAME', value: formValues.judgeConfig.internalName });
      }
      if (formValues.judgeConfig.namespace) {
        params.push({ key: 'JUDGE_MODEL_NAMESPACE', value: formValues.judgeConfig.namespace });
      }
    }

    // Simulator config - only if using separate model
    if (formValues.useSeparateSimulator && formValues.simulatorConfig?.model) {
      // Separate simulator model
      params.push({ key: 'SIMULATOR_MODEL', value: formValues.simulatorConfig.model });
      params.push({
        key: 'SIMULATOR_MODEL_PROVIDER',
        value: formValues.simulatorConfig.provider === 'internal' ? 'openai' : formValues.simulatorConfig.provider,
      });
      if (formValues.simulatorConfig.baseUrl) {
        params.push({ key: 'SIMULATOR_MODEL_BASE_URL', value: formValues.simulatorConfig.baseUrl });
      }
      params.push({
        key: 'SIMULATOR_MODEL_API',
        value: formValues.simulatorConfig.apiKey || 'sk-no-key-required',
      });
      if (formValues.simulatorConfig.internalName) {
        params.push({ key: 'SIMULATOR_MODEL_INTERNAL_NAME', value: formValues.simulatorConfig.internalName });
      }
      if (formValues.simulatorConfig.namespace) {
        params.push({ key: 'SIMULATOR_MODEL_NAMESPACE', value: formValues.simulatorConfig.namespace });
      }
    } else if (formValues.judgeConfig?.model) {
      // Use judge model as simulator (same model for both)
      params.push({ key: 'SIMULATOR_MODEL', value: formValues.judgeConfig.model });
      params.push({
        key: 'SIMULATOR_MODEL_PROVIDER',
        value: formValues.judgeConfig.provider === 'internal' ? 'openai' : formValues.judgeConfig.provider,
      });
      if (formValues.judgeConfig.baseUrl) {
        params.push({ key: 'SIMULATOR_MODEL_BASE_URL', value: formValues.judgeConfig.baseUrl });
      }
      params.push({
        key: 'SIMULATOR_MODEL_API',
        value: formValues.judgeConfig.apiKey || 'sk-no-key-required',
      });
      if (formValues.judgeConfig.internalName) {
        params.push({ key: 'SIMULATOR_MODEL_INTERNAL_NAME', value: formValues.judgeConfig.internalName });
      }
      if (formValues.judgeConfig.namespace) {
        params.push({ key: 'SIMULATOR_MODEL_NAMESPACE', value: formValues.judgeConfig.namespace });
      }
    }

    if (formValues.name) {
      params.push({ key: 'JOB_NAME', value: formValues.name });
    }
    if (formValues.description) {
      params.push({ key: 'JOB_DESCRIPTION', value: formValues.description });
    }

    params.push({ key: 'USE_GATEWAY', value: String(formValues.useGateway ?? false) });
    params.push({ key: 'NOTIFY', value: Array.isArray(formValues.notify) ? formValues.notify.join(',') : String(formValues.notify ?? '') });

    if (formValues.language) {
      params.push({ key: 'LANGUAGE', value: formValues.language });
    }

    // Benchmarks (accuracy)
    if (formValues.benchmarks && formValues.benchmarks.length > 0) {
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
              datasetSubset: b.useCustomDataset ? b.datasetSubset || 'default' : null, // NEW
            };
          }),
        ),
      });
    }

    // Performance metrics
    if (formValues.performanceMetrics && formValues.performanceMetrics.length > 0) {
      params.push({
        key: 'PERFORMANCE_METRICS',
        value: JSON.stringify(
          formValues.performanceMetrics.map((m: any) => ({
            name: m.name,
            threshold: m.threshold,
          })),
        ),
      });
    }

    // Quality metrics
    if (formValues.qualityMetrics && formValues.qualityMetrics.length > 0) {
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

    // Conversation metrics
    if (formValues.conversationMetrics && formValues.conversationMetrics.length > 0) {
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

    // Security tests (Red Teaming)
    if (formValues.securityTests && formValues.securityTests.length > 0) {
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

      // Red teaming config (only if security tests exist)
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

    if (formValues.customEvalDatasets && formValues.customEvalDatasets.length > 0) {
      params.push({
        key: 'CUSTOM_EVAL_DATASETS',
        value: JSON.stringify(
          formValues.customEvalDatasets.map((d: any) => ({
            name: d.name,
            repoId: d.repoId,
            ref: d.ref?.id || null,
            split: d.split || 'test',
            limit: d.limit || null,
            columns: {
              instruction: d.instructionColumn,
              answer: d.answerColumn,
              eval_type: d.evalTypeColumn || null,
              judge_criteria: d.judgeCriteriaColumn || null,
            },
            promptTemplate: d.usePromptTemplate ? d.promptTemplate : null,
            stopSequences: d.usePromptTemplate ? d.stopSequences : null,
            defaultJudgeCriteria: d.defaultJudgeCriteria,
          })),
        ),
      });
    }

    return params;
  }

  async save(launch?: boolean): Promise<ITaskRun> {
    revalidateForm(this.taskForm);
    if (this.taskForm.invalid) {
      displayError(this.store, 'Please fill in all required fields');
      return Promise.reject();
    }

    const taskRunValue = this.taskForm.getRawValue();
    const existing = this.taskRun();
    try {
      this.isSaving.set(true);
      taskRunValue.params = this.formToParams(taskRunValue);
      taskRunValue.benchmarks = [];
      taskRunValue.project = existing.project;
      const saved = await lastValueFrom(this.taskRunService.submit(taskRunValue));
      if (launch) {
        this.isSaving.set(false);
        this.isLaunching.set(true);
        try {
          await lastValueFrom(this.taskRunService.submit(saved));
          displaySuccess(this.store, 'Job launched successfully');
          await this.router.navigate(['/train/jobs/tekton/evaluation']);
        } catch (err) {
          console.log(err);
        }
      } else {
        displaySuccess(this.store, 'Job saved successfully');
        await this.router.navigate(['/train/jobs/tekton/evaluation']);
      }
      return Promise.resolve(taskRunValue);
    } catch (e) {
      displayError(this.store, e);
    } finally {
      this.isSaving.set(false);
      this.isLaunching.set(false);
    }
    return Promise.reject();
  }

  parseCustomEvalDatasetsParam(param: string) {
    try {
      const datasets = JSON.parse(param);
      const arr = this.customEvalDatasetsArray;
      arr.clear();
      datasets.forEach((d: any) => {
        const form = newDatasetForm('custom_evaluation', uuidv4(), 'eval');
        form.patchValue({
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
        arr.push(form);
      });
    } catch (e) {
      console.warn('Custom eval datasets parse error', e);
    }
  }

  // ============================================================================
  // FORM ARRAY GETTERS
  // ============================================================================

  get benchmarksArray(): FormArray {
    return this.taskForm.get('benchmarks') as FormArray;
  }

  get performanceMetricsArray(): FormArray {
    return this.taskForm.get('performanceMetrics') as FormArray;
  }

  get qualityMetricsArray(): FormArray {
    return this.taskForm.get('qualityMetrics') as FormArray;
  }

  get conversationMetricsArray(): FormArray {
    return this.taskForm.get('conversationMetrics') as FormArray;
  }

  get accuracyBenchmarks(): FormGroup[] {
    return this.benchmarksArray.controls.filter(
      aa => aa.get('type')?.value === 'accuracy' || aa.get('type')?.value === 'coding',
    ) as FormGroup[];
  }

  get securityBenchmarks(): FormGroup[] {
    return this.benchmarksArray.controls.filter(aa =>
      ['data privacy', 'responsible ai', 'safety', 'business'].includes(aa.get('type')?.value),
    ) as FormGroup[];
  }

  // ============================================================================
  // ADD/REMOVE METHODS
  // ============================================================================

  add(item: any) {
    const exists = this.benchmarksArray.value.some((b: any) => b.name === item.name);
    if (exists) return;

    const tasks = Array.isArray(item.tasks) ? item.tasks : [{ label: 'All', value: 'All' }];

    const benchmarkGroup = new FormGroup({
      name: new FormControl(item.name, Validators.required),
      evalScopeName: new FormControl(item.evalScopeName, Validators.required),
      type: new FormControl(item.type),
      tasks: new FormControl(tasks),
      selectedTasks: new FormControl(['All'], Validators.required),
      shots: new FormControl(item.defaultShots ?? 0, [Validators.required, Validators.min(0)]),
      limit: new FormControl(item.defaultLimit || null),
      supportsFewshot: new FormControl(item.supportsFewshot ?? true),
      useCustomDataset: new FormControl<boolean>(false),
      datasetRepo: new FormControl<string | null>(null),
      datasetRef: new FormControl<RefSelection | null>(null),
      datasetSubset: new FormControl<string>('default'), // NEW
    });

    this.benchmarksArray.push(benchmarkGroup);
  }

  addQualityMetric(item: any) {
    const exists = this.qualityMetricsArray.value.some((m: any) => m.name === item.name);
    if (!exists) {
      this.qualityMetricsArray.push(
        new FormGroup({
          name: new FormControl(item.name),
          type: new FormControl('quality'),
          datasetRepo: new FormControl<string | null>(null, Validators.required),
          datasetRef: new FormControl<RefSelection | null>(null, Validators.required),
          criteria: new FormControl<string>(''),
          limit: new FormControl<number | null>(null), // ADD limit
        }),
      );
    }
  }

  addConversationMetric(item: any) {
    const exists = this.conversationMetricsArray.value.some((m: any) => m.name === item.name);
    if (!exists) {
      this.conversationMetricsArray.push(
        new FormGroup({
          name: new FormControl(item.name),
          type: new FormControl('conversation'),
          datasetRepo: new FormControl<string | null>(null, Validators.required),
          datasetRef: new FormControl<RefSelection | null>(null, Validators.required),
          hasConfig: new FormControl<boolean>(item.hasConfig || false),
          configValue: new FormControl<number | null>(item.configDefault || null),
          configLabel: new FormControl<string>(item.configLabel || ''),
          configTooltip: new FormControl<string>(item.configTooltip || ''),
          limit: new FormControl<number | null>(null), // ADD limit
        }),
      );
    }
  }

  removeBenchmarkByName(name: string) {
    const index = this.benchmarksArray.value.findIndex((aa: any) => aa.name == name);
    if (index !== -1) {
      this.benchmarksArray.removeAt(index);
    }
  }

  removeQualityMetric(name: string) {
    const index = this.qualityMetricsArray.value.findIndex((m: any) => m.name === name);
    if (index !== -1) {
      this.qualityMetricsArray.removeAt(index);
    }
  }

  removeConversationMetric(name: string) {
    const index = this.conversationMetricsArray.value.findIndex((m: any) => m.name === name);
    if (index !== -1) {
      this.conversationMetricsArray.removeAt(index);
    }
  }

  get customEvalDatasetsArray(): FormArray<FormGroup> {
    return this.taskForm.controls.customEvalDatasets;
  }

  onNotifyChange(values: string[]) {
    this.taskForm.get('notify')?.setValue(values);
  }

  isInvalid(control: AbstractControl | null): boolean {
    return !!(control && control.invalid && (control.touched || control.dirty));
  }

  get judgeConfig(): LLMProviderConfig | null {
    return this.taskForm.get('judgeConfig')?.value ?? null;
  }

  get simulatorConfig(): LLMProviderConfig | null {
    return this.taskForm.get('simulatorConfig')?.value ?? null;
  }

  get useSeparateSimulator(): boolean {
    return this.taskForm.get('useSeparateSimulator')?.value ?? false;
  }

  // ============================================================================
  // PROTECTED REFERENCES
  // ============================================================================

  protected readonly BASE_RUN_NAME = BASE_RUN_NAME;
  protected readonly Flame = Flame;
  protected readonly ClipboardList = ClipboardList;
  protected readonly Trash = Trash;
  protected readonly EVAL_MODEL_REPOSITORY = EVAL_MODEL_REPOSITORY;
  protected readonly EVAL_JUDGE_URL = EVAL_JUDGE_URL;
  protected readonly EVAL_USE_GATEWAY_MODEL = EVAL_USE_GATEWAY_MODEL;
  protected readonly EVAL_JUDGE_API_KEY = EVAL_JUDGE_API_KEY;
  protected readonly GalleryVerticalEnd = GalleryVerticalEnd;
  protected readonly ShieldCheck = ShieldCheck;
  protected readonly Save = Save;
  protected readonly ArrowRight = ArrowRight;
  protected readonly EVAL_LANGUAGE = EVAL_LANGUAGE;
  protected readonly Sparkles = Sparkles;
  protected readonly MessageSquare = MessageSquare;
  protected readonly EVAL_LATENCY = EVAL_LATENCY;
  protected readonly EVAL_TASKS = EVAL_TASKS;
  protected readonly EVAL_SHOTS = EVAL_SHOTS;
  protected readonly EVAL_CONVERSATION_DATASET = EVAL_CONVERSATION_DATASET;
  protected readonly EVAL_QUALITY_DATASET = EVAL_QUALITY_DATASET;
  protected readonly EVAL_CUSTOM_CRITERIA = EVAL_CUSTOM_CRITERIA;
  protected readonly Settings = Settings;
  protected readonly Bot = Bot;
  protected readonly Info = Info;
  protected readonly Database = Database;
}
