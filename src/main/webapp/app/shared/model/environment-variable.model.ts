import {IContainer} from "./container.model";
import { IBaseEntity } from './base-entity.model';

export interface IEnvironmentVariable extends IBaseEntity{
  key?: string | null;
  value?: string | null;
  container?: Pick<IContainer, 'id'> | null;
  containerIndex?: number;

}

export type NewEnvironmentVariable = Omit<IEnvironmentVariable, 'id'> & { id: null };
