import { IBaseEntity } from './base-entity.model';

export interface ISSHKey extends IBaseEntity {
  name?: string | null;
  publicKey?: string | null;
  fingerprint?: string | null;
}

export type NewSSHKey = Omit<ISSHKey, 'id' | 'fingerprint'> & { };
