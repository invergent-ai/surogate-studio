import dayjs from 'dayjs/esm';
import {IBaseEntity} from './base-entity.model';

export interface IUser extends IBaseEntity {
  login?: string;
  fullName?: string | null;
  company?: string | null;
  taxCode?: string | null;
  userType?: string | null;
  mobilePhone?: string | null;
  country?: string | null;
  state?: string | null;
  city?: string | null;
  address?: string | null;
  zip?: string | null;
  agreedTerms?: boolean | null;
  activated?: boolean | null;
  deleted?: boolean | null;
  lockedUserTime?: dayjs.Dayjs | null;
  lockedOperator?: boolean | null;
  language?: string | null;
  icon?: string | null;
  activationKey?: string | null;
  resetKey?: string | null;
  resetDate?: dayjs.Dayjs | null;
  notificationSettings?: string | null;
  authorities?: string[] | null;
}

export class User implements IUser {
  constructor(
    public id: string,
    public login: string,
    public authorities?: string[] | null
  ) {}
}
