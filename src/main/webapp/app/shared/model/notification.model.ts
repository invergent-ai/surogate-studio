import dayjs from 'dayjs/esm';
import {IUser} from "./user.model";
import {NotificationType} from "./enum/notificationType.model";
import { IBaseEntity } from './base-entity.model';


export interface INotification extends IBaseEntity {
  message?: string | null;
  read?: boolean | null;
  mailSent?: boolean | null;
  createdTime?: dayjs.Dayjs | null;
  type?: keyof typeof NotificationType | null;
  extraProperties?: string | null;
  meshUser?: Pick<IUser, 'id' | 'login'> & {
    fullName?: string | null;
  } | null;
}

export type NewNotification = Omit<INotification, 'id'> & { id: null };
