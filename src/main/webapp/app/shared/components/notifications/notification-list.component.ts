// notification-list.component.ts
import {Component, EventEmitter, OnDestroy, OnInit, Output, ViewChild} from '@angular/core';
import {CommonModule} from '@angular/common';
import {VirtualScroller, VirtualScrollerModule} from 'primeng/virtualscroller';
import {CardModule} from 'primeng/card';
import {ButtonModule} from 'primeng/button';
import {SkeletonModule} from 'primeng/skeleton';
import {INotification} from '../../model/notification.model';
import {ProgressSpinnerModule} from "primeng/progressspinner";
import {TooltipModule} from "primeng/tooltip";
import {DialogModule} from "primeng/dialog";
import {Store} from "@ngxs/store";
import {MarkAllAsRead, MarkAsRead, NotificationState} from "../../state/notification.state";
import {Observable, Subject} from "rxjs";
import {takeUntil} from "rxjs/operators";
import {NotificationType} from "../../model/enum/notificationType.model";
import dayjs from "dayjs/esm";
import {NotificationFacade} from "../../state/notification.facade";

@Component({
  selector: 'sm-notification-list',
  templateUrl: './notification-list.component.html',
  styleUrls: ['./notification-list.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    VirtualScrollerModule,
    CardModule,
    ButtonModule,
    SkeletonModule,
    ProgressSpinnerModule,
    TooltipModule,
    DialogModule
  ]
})
export class NotificationListComponent implements OnInit, OnDestroy {
  @Output() unreadCountChange = new EventEmitter<number>();
  @Output() notificationSelected = new EventEmitter<INotification>();

  notifications$: Observable<INotification[]>;
  unreadCount$: Observable<number>;
  loading = false;
  private destroy$ = new Subject<void>();

  constructor( private store: Store,
               private notificationFacade: NotificationFacade) {
    this.notifications$ = this.store.select(NotificationState.getNotifications);
    this.unreadCount$ = this.store.select(NotificationState.getUnreadCount);
  }

  ngOnInit(): void {
    this.unreadCount$.pipe(
      takeUntil(this.destroy$)
    ).subscribe(count => {
      this.unreadCountChange.emit(count);
    });
  }


  openNotificationDialog(notification: INotification): void {
    this.notificationSelected.emit(notification);
  }


  truncateMessage(message: string | null | undefined, maxLength: number = 70): string {
    if (!message) return '';
    return message.length > maxLength ? message.substring(0, maxLength) + '...' : message;
  }

  getFormattedDate(date: dayjs.Dayjs | string | Date | null | undefined): string {
    if (!date) return '';
    // Convert to dayjs if it's not already
    const dayjsDate = dayjs.isDayjs(date) ? date : dayjs(date);
    return dayjsDate.format('MMM D, YYYY, h:mm A');
  }
  getNotificationIcon(type: keyof typeof NotificationType | null | undefined): string {
    switch (type) {
      case NotificationType.SYSTEM:
        return 'pi pi-cog';
      case NotificationType.USER:
        return 'pi pi-user';
      case NotificationType.NONE:
      default:
        return 'pi pi-bell';
    }
  }
  markAsRead(notification: INotification): void {
    if (notification.id && !notification.read) {
      this.notificationFacade.markAsRead(notification.id).subscribe()
    }
  }

  markAllAsRead(): void {
    this.notificationFacade.markAllAsRead().subscribe();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
