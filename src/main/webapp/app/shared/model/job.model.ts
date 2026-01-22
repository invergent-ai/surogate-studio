// Generic transient Job model that merges common attributes from ITaskRun and IRayJob

export interface IJob {
  id?: string | null;
  jobId?: string | null;
  type?: keyof typeof IJobType | null;
  provisioningStatus?: keyof typeof IJobStatus | null;
  createdDate?: string | null;
  startTime?: string | null;
  endTime?: string | null;
  podName?: string;
  container?: string;
  completedStatus?: string;
  // transient
  stage?: string;
  message?: string;
}

export interface IJobRunStatus {
  jobId?: string;
  stage?: string;
  provisioningStatus?: IJobStatus;
  message?: string;
  startTime?: string;
  completionTime?: string;
  podName?: string;
  container?: string;
  progress?: string;
  // Mapped
  taskId?: string;
  rayJobId?: string;
}

export enum IJobStatus {
  CREATED = 'CREATED',
  DEPLOYING = 'DEPLOYING',
  DEPLOYED = 'DEPLOYED',
  ERROR = 'ERROR',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED'
}

export enum IJobType {
  QUANTIZATION = "QUANTIZATION",
  EVALUATION = "EVALUATION",
  IMPORT_HF_MODEL = "IMPORT_HF_MODEL",
  IMPORT_HF_DATASET = "IMPORT_HF_DATASET",
  CHUNKING = 'CHUNKING',
  QUESTION_GENERATION = "QUESTION_GENERATION",
  ANSWER_GENERATION = "ANSWER_GENERATION",
  TRAIN = 'TRAIN',
  FINE_TUNE = 'FINE_TUNE'
}

export const JobTypeLabels: Record<IJobType, string> = {
  [IJobType.QUANTIZATION]: 'Quantization',
  [IJobType.EVALUATION]: 'Evaluation',
  [IJobType.IMPORT_HF_MODEL]: 'Import Model',
  [IJobType.IMPORT_HF_DATASET]: 'Import Dataset',
  [IJobType.CHUNKING]: 'Chunking',
  [IJobType.QUESTION_GENERATION]: 'Question generation',
  [IJobType.ANSWER_GENERATION]: 'Answer generation',
  [IJobType.TRAIN]: 'Training',
  [IJobType.FINE_TUNE]: 'Fine Tuning'
};
