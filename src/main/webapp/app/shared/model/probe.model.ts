import { IContainer } from './container.model';
import { IBaseEntity } from './base-entity.model';
import { ProbeType } from './enum/probe-type.model';

export interface IProbe extends IBaseEntity {
  type?: keyof typeof ProbeType | null;
  initialDelaySeconds?: number | null;
  periodSeconds?: number | null;
  failureThreshold?: number | null;
  successThreshold?: number | null;
  timeoutSeconds?: number | null;
  terminationGracePeriodSeconds?: number | null;
  httpPath?: string | null;
  httpPort?: number | null;
  tcpHost?: string | null;
  tcpPort?: number | null;
  execCommand?: string[] | null;
  container?: Pick<IContainer, 'id' | 'imageName'> | null;
}

export type NewProbe = Omit<IProbe, 'id'> & { id: null };
