import dayjs from "dayjs/esm";
import { IBaseEntity } from './base-entity.model';

export interface IOnboardingErrorMessage {
  error?: string | null;
}

export interface INodeReservationError extends IBaseEntity {
  created?: dayjs.Dayjs | null;
  error?: string | null;
  errors?: IOnboardingErrorMessage[] | null;
}

export interface INodeReservation extends IBaseEntity {
  created?: dayjs.Dayjs | null;
  updated?: dayjs.Dayjs | null;
  accessToken?: string | null;
  internalName?: string | null;
  expireTime?: dayjs.Dayjs | null;
  smId?: string | null;
  shortSmId?: string | null;
  userKey?: string | null;
  deleted?: boolean | null;
  fulfilled?: boolean | null;
  errors?: INodeReservationError[] | null;
}

export type NewNodeReservation = Omit<INodeReservation, 'id'> & { id: null };
