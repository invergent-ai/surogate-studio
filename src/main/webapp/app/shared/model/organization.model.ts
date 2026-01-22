import { OrganizationType } from './enum/organization-type.model';
import { IBaseEntity } from './base-entity.model';

export interface IOrganization extends IBaseEntity {
  name?: string | null;
  type?: keyof typeof OrganizationType | null;
}

export type NewOrganization = Omit<IOrganization, 'id'> & { id: null };
