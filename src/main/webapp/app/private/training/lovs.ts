import { ApiKeyProvider } from '../../shared/model/enum/api-key.enum';

export const ACCELERATORS: any[] = [
  { label: 'A10', value: 'A10' },
  { label: 'A10G', value: 'A10G' },
  { label: 'A100', value: 'A100' },
  { label: 'A100-80GB', value: 'A100-80GB' },
  { label: 'B200', value: 'B200' },
  { label: 'H100', value: 'H100' },
  { label: 'H100-SXM', value: 'H100-SXM' },
  { label: 'H200-SXM', value: 'H200-SXM' },
  { label: 'H200', value: 'H200' },
  { label: 'L4', value: 'L4' },
  { label: 'L40', value: 'L40' },
  { label: 'L40S', value: 'L40S' },
  { label: 'T4', value: 'T4' },
  { label: 'V100', value: 'V100' },
  { label: 'V100-32GB', value: 'V100-32GB' },
  { label: 'RTX3060', value: 'RTX3060' },
  { label: 'RTX3090', value: 'RTX3090' },
  { label: 'RTX4060', value: 'RTX4060' },
  { label: 'RTX4070', value: 'RTX4070' },
  { label: 'RTX4090', value: 'RTX4090' },
  { label: 'RTX5070', value: 'RTX5070' },
  { label: 'RTX5090', value: 'RTX5090' },
  { label: 'RTX6000', value: 'RTX6000' },
  { label: 'RTX6000Ada', value: 'RTX6000Ada' }
];

export const CLOUD_INFRA_PROVIDERS: any[] = [
  { label: 'AWS', value: ApiKeyProvider.AWS },
  { label: 'GCP', value: ApiKeyProvider.GCP },
  { label: 'OCI', value: ApiKeyProvider.OCI },
  { label: 'RunPod', value: ApiKeyProvider.RUNPOD },
];
