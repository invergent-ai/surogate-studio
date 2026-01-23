// src/app/shared/components/evaluation-results/models/evaluation-results.models.ts

export interface ICustomDetailedResult {
  original_idx: number;
  eval_type: string;
  instruction: string;
  expected: string;
  output: string;
  raw_output: string;
  score: number;
  success: boolean;
  reason: string;
  format: string;
  criteria?: string;
}

export interface ICustomTaskResult {
  total?: number;
  correct?: number;
  accuracy?: number;
  avg_score?: number;
  success_rate?: number;
}

export interface ICustomBenchmark {
  benchmark_name: string;
  backend: string;
  overall_score: number;
  num_samples: number;
  task_results: { [key: string]: ICustomTaskResult };
  detailed_results: ICustomDetailedResult[];
  metadata: any;
  status: string;
}
