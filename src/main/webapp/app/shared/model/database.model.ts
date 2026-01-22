import { IBaseResource } from './base-entity.model';
import { DatabaseStatus } from './enum/database-status.model';
import { IVolumeMount } from './volume-mount.model';
import { IFirewallEntry } from './firewall-entry.model';

export interface IDatabase extends IBaseResource {
  internalName?: string | null;
  description?: string | null;
  hasIngress?: boolean;
  ingressHostName?: string | null;
  deployedNamespace?: string | null;
  errorMessageKey?: string | null;
  status?: keyof typeof DatabaseStatus | DatabaseStatus.ERROR;
  cpuLimit?: number | null;
  memLimit?: string | null;
  replicas?: number | null;
  volumeMounts?: IVolumeMount[] | null;
  firewallEntries?: IFirewallEntry[] | null;
  // Transient
  keepVolumes?: boolean;
}

export type NewDatabase = Omit<IDatabase, 'id'> & { id: null };
