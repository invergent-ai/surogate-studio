import { IBaseResource } from './base-entity.model';
import { TaskRunProvisioningStatus } from './enum/task-run-provision-status.model';
import { TaskRunType } from './enum/task-run-type.model';

export interface ITaskRun extends IBaseResource {
  internalName?: string | null;
  jobId?: string | null;
  deployedNamespace?: string | null;
  type?: keyof typeof TaskRunType | null;
  provisioningStatus?: keyof typeof TaskRunProvisioningStatus | null;
  params?: ITaskRunParam[] | null;
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

export interface ITaskRunParam {
  key?: string | null;
  value?: string | null;
}
