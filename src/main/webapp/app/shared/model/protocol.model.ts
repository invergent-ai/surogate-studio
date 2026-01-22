import { IBaseEntity } from './base-entity.model';

export enum ProtocolType {
  TCP = 'TCP',
  UDP = 'UDP',
  SCTP = 'SCTP'
}

export const ProtocolLabels: Record<ProtocolType, string> = {
  [ProtocolType.TCP]: 'TCP Protocol',
  [ProtocolType.UDP]: 'UDP Protocol',
  [ProtocolType.SCTP]: 'SCTP Protocol'
};

export interface IProtocol extends IBaseEntity {
  code?: ProtocolType | null;
  value?: string | null;
  port?: number | null;
}

export type NewProtocol = Omit<IProtocol, 'id'> & { id: null };
