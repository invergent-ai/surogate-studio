import { IBaseEntity } from './base-entity.model';

export interface ISystemConfiguration extends IBaseEntity {
  webDomain?: string | null;
}

export type NewSystemConfiguration = Omit<ISystemConfiguration, 'id'> & { id: null };
