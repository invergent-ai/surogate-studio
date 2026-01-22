import {PullImageMode} from './enum/pull-image-mode.model';
import {IApplication} from './application.model';
import {IEnvironmentVariable} from "./environment-variable.model";
import {IPort} from "./port.model";
import {ContainerStatusStage} from "./enum/application-status.model";
import {IBaseEntity} from './base-entity.model';
import {IVolumeMount} from './volume-mount.model';
import {IFirewallEntry} from "./firewall-entry.model";
import {ContainerType} from "./enum/container-type.model";
import { IProbe } from './probe.model';
import { ContainerStatus } from './k8s/app-status.model';

export enum ResourceType {
  CPU = 'CPU',
  // Enable these when needed
  // GPU = 'GPU',
  // BOTH = 'BOTH'
}

export const DEFAULT_VALUES = {
  MEMORY: {
    request: '256',
    limit: '1024'
  },
  CPU: {
    request: 1,
    limit: 1
  },
  GPU: {
    type: 'nvidia',
    limit: 1
  }
};

export interface IContainer extends IBaseEntity {
  displayName?: string | null;
  imageName?: string | null;
  imageTag?: string | null;
  type?: keyof typeof ContainerType | null;
  pullImageMode?: keyof typeof PullImageMode | null;
  registryUrl?: string | null;
  registryUsername?: string | null;
  resourceType?: keyof typeof ResourceType | null;
  cpuRequest?: number | null;
  cpuLimit?: number | null;
  memRequest?: string | null;
  memLimit?: string | null;
  gpuLimit?: number | 0;
  gpuType?: string | null;
  startParameters?: string | null;
  startCommand?: string | null;
  envVars?: IEnvironmentVariable[] | null;
  firewallEntries?: IFirewallEntry[] | null;
  ports?: IPort[] | null;
  probes?: IProbe[] | null;
  volumeMounts?: IVolumeMount[] | null;
  application?: Pick<IApplication, 'id'> | null;
  // Transient
  registryPassword?: string | null;
}

export type NewContainer = Omit<IContainer, 'id'> & { id: null };
