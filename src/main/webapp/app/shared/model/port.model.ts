import {IProtocol} from "./protocol.model";
import {IContainer} from "./container.model";
import { IBaseEntity } from './base-entity.model';

export interface IPort extends IBaseEntity {
  name?: string | null;
  containerPort?: number | null;
  servicePort?: number | null;
  ingressPort?: boolean | false;
  ingressHost?: string | null;
  protocol?: IProtocol | null;
  containerIndex?: number;
  container?: Pick<IContainer, 'id' | 'imageName'> | null;
}

export type NewPort = Omit<IPort, 'id'> & { id: null };
