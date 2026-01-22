// notification.facade.ts
import {Injectable} from "@angular/core";
import {Store} from "@ngxs/store";
import {NotificationService} from "../service/notification.service";
import {Observable} from "rxjs";
import {map, tap} from "rxjs/operators";
import {AddNotification, LoadNotifications, MarkAllAsRead, MarkAsRead} from "./notification.state";
import {INotification} from "../model/notification.model";

@Injectable({ providedIn: 'root' })
export class NotificationFacade {
  constructor(
    private store: Store,
    private notificationService: NotificationService
  ) {}

  markAsRead(id: string): Observable<void> {
    return this.notificationService.markAsRead(id).pipe(
      map(() => void 0),
      tap(() => {
        this.store.dispatch(new MarkAsRead(id));
      })
    );
  }

  markAllAsRead(): Observable<void> {
    return this.notificationService.markAllAsRead().pipe(
      map(() => void 0),
      tap(() => {
        this.store.dispatch(new MarkAllAsRead());
      })
    );
  }

  loadNotifications(params: any): Observable<void> {
    return this.notificationService.getUserNotifications(params).pipe(
      map(response => response.body ?? []),
      tap(notifications => {
        this.store.dispatch(new LoadNotifications(notifications));
      }),
      map(() => void 0)
    );
  }

  addNotification(notification: INotification): void {
    this.store.dispatch(new AddNotification(notification));
  }
}
