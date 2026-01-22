import {IZone} from './zone.model';
import { IBaseEntity } from './base-entity.model';

export interface ICluster extends IBaseEntity {
  name?: string | null;
  cid?: string | null;
  kubeConfig?: string | null;
  openCostUrl?: string | null;
  prometheusUrl?: string | null;
  redisUrl?: string | null;
  masterIp?: string | null;
  publicIp?: string | null;
  vpnAuth?: string | null;
  requestVsLimitsCoefficientCpu?: number | null;
  requestVsLimitsCoefficientMemory?: number | null;
  description?: string | null;
  zone?: Pick<IZone, 'id' | 'name' | 'zoneId'> | null;
}

export type NewCluster = Omit<ICluster, 'id'> & { id: null };
