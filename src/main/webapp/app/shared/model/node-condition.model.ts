import { INode } from './node.model';
import { IBaseEntity } from './base-entity.model';

export interface INodeCondition extends IBaseEntity {
  memoryPressure?: boolean | null;
  diskPressure?: boolean | null;
  pidPressure?: boolean | null;
  kubeletNotReady?: boolean | null;
  node?: Pick<INode, 'id' | 'name'> | null;
}

export type NewNodeCondition = Omit<INodeCondition, 'id'> & { id: null };
