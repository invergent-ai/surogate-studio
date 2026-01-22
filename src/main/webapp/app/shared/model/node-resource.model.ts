import {INode} from "./node.model";
import { IBaseEntity } from './base-entity.model';

export interface INodeResource extends IBaseEntity {
  rxMbps?: number | null;
  txMbps?: number | null;
  allocatableCpu?: number | null;
  allocatableMemory?: number | null;
  allocatableEphemeralStorage?: number | null;
  capacityCpu?: number | null;
  capacityMemory?: number | null;
  capacityEphemeralStorage?: number | null;
  node?: Pick<INode, 'id' | 'name'> | null;
}

export type NewNodeResource = Omit<INodeResource, 'id'> & { id: null };
