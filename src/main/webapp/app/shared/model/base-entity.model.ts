import { IProject } from './project.model';

export interface IBaseEntity {
  id: string | null;
}

export interface IBaseResource extends IBaseEntity {
  name?: string | null;
  project?: Pick<IProject, 'id' | 'name' | 'cluster' | 'zone'> | null;
}
