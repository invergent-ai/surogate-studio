import {RoutingStrategy} from "./enum/routing-strategy.model";
import {DeploymentMode} from "./enum/deployment-mode.model";

export interface IModelConfig {
  modelName?: string;
  maxContextSize: number;

  enablePartitioning: boolean;
  partitions: number;

  l1Cache: boolean;
  l1CacheSize: number;
  l2Cache: boolean;
  l2CacheSize: number;

  enableReplication: boolean;
  replicas: number;

  routingStrategy: RoutingStrategy;
  routerReplicas: number;
  routerSessionKey?: string;

  deploymentMode: DeploymentMode;

  gpuMemory: number; // in MB

  hfConfig: any;
  hfTotalSafetensors: number;

  source?: string;
  hfToken?: string;
  hfModelName?: string;
  branchToDeploy?: string;
  branchToDeployDisplayName?: string;
  loraSourceModel?: string
}
