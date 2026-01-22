import { IVolume } from './volume.model';
import { IContainer } from './container.model';
import { IBaseEntity } from './base-entity.model';
import {IDatabase} from "./database.model";

export interface IVolumeMount extends IBaseEntity {
  containerPath?: string | null;
  readOnly?: boolean | null;
  volume?: IVolume | null;
  container?: Pick<IContainer, 'id'> | null;
  database?: Pick<IDatabase, 'id'> | null;

  // Transient
  containerIndex: number;
  applicationId: string;
}

export type NewVolumeMount = Omit<IVolumeMount, 'id' | 'applicationId'> & { id: null };
