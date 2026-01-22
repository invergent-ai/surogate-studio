import { ApplicationStatus, ResourceStatusStage, ContainerStatusStage } from '../enum/application-status.model';
import { IApplication } from '../application.model';
import { IContainer } from '../container.model';

export interface AppStatusWithResources {
  status: keyof typeof ApplicationStatus;
  resourceStatus: ResourceStatus[];
  applicationId: string;
}

export interface ResourceStatus {
  podName: string;
  component?: string;
  stage: ResourceStatusStage;
  message: string;
  details: string[];
  containerStatuses: ContainerStatus[];
}

export interface ContainerStatus {
  containerId: string;
  containerName: string;
  init: boolean;
  stage: ContainerStatusStage;
  lastStage: ContainerStatusStage;
  waitingMessage: string;
  lastStageTerminatedMessage: string;
  lastStageTerminatedReason: string;
  // transient
  container: IContainer;
}

export function appStatusWithContainers(app: IApplication, status: AppStatusWithResources): AppStatusWithResources {
  if (app && status) {
    return {
      ...status,
      resourceStatus: status.resourceStatus.map(rs => {
        return {
          ...rs,
          containerStatuses: rs.containerStatuses ? rs.containerStatuses.map(cs => {
            return {
              ...cs,
              container: app.containers.find(c => c.id === cs.containerId)
            };
          }) : []
        };
      })
    };
  }
  return status;
}

export function aggregatedModelStatus(appStatus: AppStatusWithResources[]): keyof typeof ApplicationStatus | null {
  // Guard: empty or null entries
  if (!appStatus || appStatus.length === 0 || appStatus.some(s => s == null)) {
    return null;
  }
  // any error dominates
  if (appStatus.some(s => s.status === ApplicationStatus.ERROR)) {
    return ApplicationStatus.ERROR;
  }
  // any initialized -> initialized
  if (appStatus.some(s => s.status === ApplicationStatus.INITIALIZED)) {
    return ApplicationStatus.INITIALIZED;
  }
  // any deploying -> deploying
  if (appStatus.some(s => s.status === ApplicationStatus.DEPLOYING)) {
    return ApplicationStatus.DEPLOYING;
  }
  // any deployed -> deployed
  if (appStatus.some(s => s.status === ApplicationStatus.DEPLOYED)) {
    return ApplicationStatus.DEPLOYED;
  }

  // common statuses
  if (appStatus.every(s => s.status === ApplicationStatus.CREATED)) {
    return ApplicationStatus.CREATED;
  }

  // Fallback (should be unreachable given current enum set). Keep null to avoid misleading status.
  return null;
}
