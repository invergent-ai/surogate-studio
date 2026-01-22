import { ApplicationType } from './enum/application-type.model';
import { ApplicationStatus } from './enum/application-status.model';
import { WorkloadType } from './enum/workload-type.model';
import { UpdateStrategy } from './enum/update-strategy.model';
import { SchedulingRule } from './enum/scheduling-rule.model';
import { IContainer } from './container.model';
import { IAnnotation } from './annotation.model';
import { IBaseResource } from './base-entity.model';
import { ApplicationMode } from './enum/application-mode.model';

export interface IApplication extends IBaseResource {
  internalName?: string | null;
  alias?: string | null; // Deprecated
  description?: string | null;
  ingressHostName?: string | null;
  deployedNamespace?: string | null;
  type?: keyof typeof ApplicationType | null;
  mode?: keyof typeof ApplicationMode | null;
  status?: keyof typeof ApplicationStatus;
  workloadType?: keyof typeof WorkloadType | null;
  yamlConfig?: string | null;
  replicas?: number | null;
  updateStrategy?: keyof typeof UpdateStrategy | null;
  schedulingRule?: keyof typeof SchedulingRule | null;
  extraConfig?: string;
  containers?: IContainer[] | null;
  annotations?: IAnnotation[] | null;
  monthlyAppCosts?: number | null;
  firstTx?: Date | null;
  fromTemplate?: boolean;
  freeTier?: boolean;
  totalAppCosts?: number | null;
  errorMessageKey?: string | null;
  // Transient
  message?: string;
  keepVolumes?: boolean;
  costPerHour?: number;
}

export type NewApplication = Omit<IApplication, 'id'> & { id: null };
