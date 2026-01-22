import {RayJobProvisioningStatus} from "../enum/ray-job-provisioning-status.model";

export interface RayJobRunStatus {
  jobId?: string;
  stage?: string;
  provisioningStatus?: RayJobProvisioningStatus;
  message?: string;
  startTime?: string;
  completionTime?: string;
  podName?: string;
  container?: string;
  submissionId?: string;
}
