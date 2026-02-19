import {TrainingOptimizerType, TrainingPrecisionType, TrainingSchedulerType} from "./training.model";

export interface ITrainingConfig {
  loraR?: number;
  loraAlpha?: number;
  loraDropout?: number;
  loraTargetModules?: string[];
  datasets?: IDatasetConfig[];
  testDatasets?: IDatasetConfig[];
  numEpochs?: number;
  microBatchSize?: number;
  gradientAccumulationSteps?: number;
  trainOnInputs?: boolean;
  learningRate?: string;
  optimizer?: TrainingOptimizerType;
  valSetSize?: number;
  evalSteps?: number;
  loggingSteps?: number;
  maxSteps?: number;
  saveSteps?: number;
  savesPerEpoch?: number;
  weightDecay?: number;
  maxGradNorm?: number;
  sequenceLen?: number;
  samplePacking?: boolean;
  lrScheduler?: TrainingSchedulerType;
  warmupSteps?: number;
  warmupRatio?: number;
  cooldownSteps?: number;
  finalLrFraction?: number;
  gradientCheckpointing?: boolean;
  skipQuantFirstLayers?: number;
  skipQuantLastLayers?: number;
  debugTimeBreakdown?: boolean;
  debugMemoryBreakdown?: boolean;
  recipe?: TrainingPrecisionType;
  zeroLevel?: number;
  lora?: boolean;
  qloraFp8?: boolean;
  qloraFp4?: boolean;
  qloraBnb?: boolean;
  mergeLora?: boolean;
  mergeIteratively?: boolean;
}

export interface IDatasetConfig {
  repoId?: string;
  ref?: string;
  path?: string;
  type?: string;
  subset?: string;
  split?: string;
  samples?: string;
  textField?: string;
  instructionField?: string;
  inputField?: string;
  outputField?: string;
  systemPromptType?: string;
  systemPromptField?: string;
  systemPrompt?: string;
  promptFormat?: string;
  promptFormatNoInput?: string;
  messagesField?: string;
  systemField?: string;
  toolsField?: string;
  messagePropertyMappings?: IMessagePropertyMapping;
}

export interface IMessagePropertyMapping {
  role?: string;
  content?: string;
}
