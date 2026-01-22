import { VolumeType } from './enum/volume-type.model';
import { IVolumeMount } from './volume-mount.model';
import { IBaseResource } from './base-entity.model';

export interface ProjectVolumesDTO {
  projectId: string;
  volumes: IVolume[];
}

export interface IVolume extends IBaseResource {
  path?: string | null;
  type?: VolumeType | null;
  size?: number | null;
  bucketUrl?: string | null;
  accessKey?: string | null;
  region?: string | null;
  accessSecret?: string | null;
  volumeMounts?: IVolumeMount[];
}

export type NewVolume = Omit<IVolume, 'id'> & { id: null };
