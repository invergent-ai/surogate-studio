import { TaskRunProvisioningStatus } from '../enum/task-run-provision-status.model';

export interface TaskRunStatus {
  taskId?: string;
  stage?: string;
  provisioningStatus?: TaskRunProvisioningStatus;
  message?: string;
  startTime?: string;
  completionTime?: string;
  podName?: string;
  container?: string;
  progress?: string;
}
