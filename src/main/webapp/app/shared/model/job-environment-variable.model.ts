import {IBaseEntity} from './base-entity.model';

export interface IJobEnvironmentVariable extends IBaseEntity {
  key?: string | null;
  value?: string | null;
}

export type NewJobEnvironmentVariable = Omit<IJobEnvironmentVariable, 'id'> & { id: null };
