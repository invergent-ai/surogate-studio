import { AppStatusWithResources } from './app-status.model';
import { ApplicationStatus } from '../enum/application-status.model';

export interface ModelStatusWithResources {
  applicationId: string;
  status: keyof typeof ApplicationStatus;
  router: AppStatusWithResources;
  worker: AppStatusWithResources;
  cache: AppStatusWithResources;
}
