export enum TrainingPrecisionType {
  FP32 = 'fp32',
  BF16 = 'bf16',
  FP16 = 'fp16',
  FP8 = 'fp8',
  FP4 = 'fp4',
}

export enum TrainingOptimizerType {
  AdamW = 'adamw_8bit',
  SGD = 'sgd',
  NorMuon = 'normuon'
}

export enum TrainingSchedulerType {
  Cosine = 'cosine',
  Linear = 'linear',
  WSD = 'wsd',
  Constant = 'constant',
}

export enum BatchSamplerType {
  NO_DUPLICATES = 'NO_DUPLICATES',
  GROUP_BY_LABEL = 'GROUP_BY_LABEL',
  DEFAULT = 'BATCH_SAMPLER',
}

export enum MultiDatasetBatchSamplerType {
  ROUND_ROBIN = 'ROUND_ROBIN',
  PROPORTIONAL = 'PROPORTIONAL',
}
