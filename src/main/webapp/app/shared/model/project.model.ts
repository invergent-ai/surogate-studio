import {IOrganization} from './organization.model';
import {ICluster} from './cluster.model';
import {IUser} from './user.model';
import {IBaseEntity} from './base-entity.model';
import {IZone} from './zone.model';

export enum IProjectResourceType {
  APPLICATION = "APPLICATION",
  VIRTUAL_INSTANCE = "VIRTUAL_INSTANCE",
  VOLUME = "VOLUME",
  DATABASE = "DATABASE"
}

export interface IProjectResource extends IBaseEntity {
  name?: string | null;
  type?: string | null;
}

export interface IProject extends IBaseEntity {
  name?: string | null;
  alias?: string | null;
  description?: string | null;
  namespace?: string | null;
  datacenterName?: string | null;
  rayCluster?: string | null;
  profile?: string | null;
  deleted?: boolean | null;
  user?: Pick<IUser, 'id' | 'fullName'> | null;
  organization?: Pick<IOrganization, 'id' | 'name'> | null;
  cluster?: Pick<ICluster, 'id' | 'name' | 'zone' | 'publicIp'> | null;
  zone?: Pick<IZone, 'id' | 'name' | 'zoneId'> | null;
}

export type NewProject = Omit<IProject, 'id'> & { id: null };
