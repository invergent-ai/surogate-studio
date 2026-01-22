import { IOrganization } from 'app/shared/model/organization.model';
import { IBaseEntity } from './base-entity.model';

export interface IZone extends IBaseEntity {
  name?: string;
  vpnApiKey?: string;
  iperfIp?: string;
  zoneId?: string;
  hasHPC?: boolean;
  hasGPU?: boolean;
  organization?: Pick<IOrganization, 'id' | 'name'> | null;
}

export type NewZone = Omit<IZone, 'id'> & { id: null };
