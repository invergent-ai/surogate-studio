// notification.state.ts
import {Action, Selector, State, StateContext} from '@ngxs/store';
import {Injectable} from '@angular/core';
import {INotification} from "../model/notification.model";

export interface NotificationStateModel {
  notifications: INotification[];
  unreadCount: number;
}

export class AddNotification {
  static readonly type = '[Notification] Add';
  constructor(public notification: INotification) {}
}

export class MarkAsRead {
  static readonly type = '[Notification] Mark As Read';
  constructor(public id: string) {}
}

export class MarkAllAsRead {
  static readonly type = '[Notification] Mark All As Read';
}

export class LoadNotifications {
  static readonly type = '[Notification] Load';
  constructor(public notifications: INotification[]) {}
}

// notification.state.ts
@State<NotificationStateModel>({
  name: 'notifications',
  defaults: {
    notifications: [],
    unreadCount: 0
  }
})
@Injectable()
export class NotificationState {
  @Selector()
  static getNotifications(state: NotificationStateModel) {
    return state.notifications;
  }

  @Selector()
  static getUnreadCount(state: NotificationStateModel) {
    return state.unreadCount;
  }

  @Action(LoadNotifications)
  load(ctx: StateContext<NotificationStateModel>, action: LoadNotifications) {
    const unreadCount = action.notifications.filter(n => !n.read).length;
    ctx.setState({
      notifications: action.notifications,
      unreadCount
    });
  }

  @Action(AddNotification)
  add(ctx: StateContext<NotificationStateModel>, action: AddNotification) {
    const state = ctx.getState();
    ctx.setState({
      ...state,
      notifications: [action.notification, ...state.notifications],
      unreadCount: state.unreadCount + (action.notification.read ? 0 : 1)
    });
  }

  @Action(MarkAsRead)
  markAsRead(ctx: StateContext<NotificationStateModel>, action: MarkAsRead) {
    const state = ctx.getState();
    const notifications = state.notifications.map(n =>
      n.id === action.id ? { ...n, read: true } : n
    );
    ctx.setState({
      ...state,
      notifications,
      unreadCount: state.unreadCount - 1
    });
  }

  @Action(MarkAllAsRead)
  markAllAsRead(ctx: StateContext<NotificationStateModel>) {
    ctx.setState({
      notifications: [],
      unreadCount: 0
    });
  }
}
