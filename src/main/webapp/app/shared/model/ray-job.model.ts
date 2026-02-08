import {IBaseResource} from './base-entity.model';
import {RayJobType} from "./enum/ray-job-type.model";
import {IJobEnvironmentVariable} from "./job-environment-variable.model";
import {RayJobProvisioningStatus} from "./enum/ray-job-provisioning-status.model";
import {ITrainingConfig} from "./training-config.model";
import {IRayClusterShape} from "./ray-cluster-shape.model";
import { ISkyConfig } from './sky-config.model';

export interface IRayJob extends IBaseResource {
  internalName?: string | null;
  description?: string | null;
  jobId?: string | null;
  submissionId?: string | null;
  deployedNamespace: string | null;
  chatHostName: string | null;
  workDirVolumeName: string | null;
  podName?: string;
  container?: string;
  completedStatus?: string;
  type?: keyof typeof RayJobType;
  runInTheSky?: boolean;
  skyToK8s?: boolean;
  useAxolotl?: boolean;
  provisioningStatus?: keyof typeof RayJobProvisioningStatus;
  trainingConfig?: string | null;
  rayClusterShape?: string | null;
  skyConfig?: string | null;
  envVars?: IJobEnvironmentVariable[] | null;
  createdDate?: string | null;
  startTime?: string | null;
  endTime?: string | null;
  // transient
  stage?: string;
  message?: string;
  trainingConfigPojo?: ITrainingConfig | null;
  rayClusterShapePojo?: IRayClusterShape | null;
  skyConfigPojo?: ISkyConfig | null;
}

export type NewRayJob = Omit<IRayJob, 'id'> & { id: null };
