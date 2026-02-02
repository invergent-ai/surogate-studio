// src/app/shared/model/enum/api-key.enum.ts

export type ApiKeyType = 'LLM' | 'CLOUD';

export const ApiKeyProvider = {
  // LLM Providers
  OPENAI: 'openai',
  ANTHROPIC: 'anthropic',
  OPENROUTER: 'openrouter',
  AZURE: 'azure',
  VLLM: 'vllm',
  OLLAMA: 'ollama',
  INTERNAL: 'internal',

  // Cloud Providers
  AWS: 'aws',
  GCP: 'gcp',
  OCI: 'oci',
  RUNPOD: 'runpod',
} as const;

export type ApiKeyProvider = (typeof ApiKeyProvider)[keyof typeof ApiKeyProvider];

// Providers that support saved API keys
export const LLM_PROVIDERS_WITH_SAVED_KEYS: ApiKeyProvider[] = [
  ApiKeyProvider.OPENAI,
  ApiKeyProvider.ANTHROPIC,
  ApiKeyProvider.OPENROUTER,
  ApiKeyProvider.AZURE,
];

export const CLOUD_PROVIDERS: ApiKeyProvider[] = [ApiKeyProvider.AWS, ApiKeyProvider.GCP, ApiKeyProvider.OCI, ApiKeyProvider.RUNPOD];

export const ALL_LLM_PROVIDERS: ApiKeyProvider[] = [
  ApiKeyProvider.INTERNAL,
  ApiKeyProvider.OPENAI,
  ApiKeyProvider.ANTHROPIC,
  ApiKeyProvider.OPENROUTER,
  ApiKeyProvider.AZURE,
  ApiKeyProvider.VLLM,
  ApiKeyProvider.OLLAMA,
];
