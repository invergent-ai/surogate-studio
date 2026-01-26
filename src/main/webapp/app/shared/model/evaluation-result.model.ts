export interface IEvaluationResult {
  timestamp: string;
  project: {
    name: string;
    version: string;
    description: string;
  };
  summary: {
    total_targets: number;
    total_evaluations: number;
    total_test_cases: number;
  };
  targets: IEvaluationTarget[];
}

export interface IEvaluationTarget {
  name: string;
  type: string;
  model: string;
  provider: string;
  status: string;
  evaluations: IEvaluation[];
  benchmarks?: IBenchmarkResult[];
  red_teaming?: IRedTeamingResult;
}

export interface IEvaluation {
  name: string;
  dataset?: string;
  dataset_type?: string;
  num_test_cases?: number;
  num_metrics?: number;
  status?: string;
  metrics_summary?: { [key: string]: IMetricSummary };
  detailed_results?: IDetailedResult[];
}

export interface IMetricSummary {
  metric_name: string;
  metric_type: string;
  num_evaluations: number;
  avg_score: number;
  success_rate: number;
  results: IMetricResult[];
}

export interface IMetricResult {
  metric_name: string;
  metric_type: string;
  score: number;
  success: boolean;
  reason: string;
  metadata?: {
    deepeval_type?: string;
    evaluation_model?: string;
    is_conversational?: boolean;
    is_multimodal?: boolean;
    test_case_type?: string;
  };
}

export interface IDetailedResult {
  test_case_index: number;
  input: string;
  output: string;
  metrics: { [key: string]: IMetricResult };
}

export interface IBenchmarkResult {
  benchmark_name: string;
  backend: string;
  overall_score: number;
  num_samples: number;
  status: string;
  task_results: { [key: string]: ITaskResult };
  metadata: {
    backend: string;
    benchmark: string;
    num_datasets: number;
    dataset_name: string;
    model_name: string;
  };
}

export interface ITaskResult {
  score: number;
  accuracy: number;
  n_samples: number;
}

export interface IRedTeamingResult {
  target_name: string;
  timestamp: string;
  vulnerabilities: IVulnerabilityResult[];
  overview?: string;
  detailed_results?: IRedTeamingDetailedResult[];
}

export interface IVulnerabilityResult {
  vulnerability_name?: string;
  vulnerability_type: string;
  total_attacks: number;
  successful_attacks: number;
  failed_attacks: number;
  success_rate: number;
  severity: string;
  attack_breakdown?: { [key: string]: number };
}

export interface IRedTeamingDetailedResult {
  idx: number;
  vulnerability: string;
  vulnerability_type: string;
  attack_method: string;
  input: string;
  actual_output: string;
  expected_output: string | null;
  score: number;
  success: boolean;
  reason: string | null;
}

