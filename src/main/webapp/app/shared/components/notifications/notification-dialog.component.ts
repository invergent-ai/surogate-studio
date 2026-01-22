// notification-dialog.component.ts
import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DialogModule } from 'primeng/dialog';
import { ButtonModule } from 'primeng/button';
import { INotification } from '../../model/notification.model';

@Component({
  selector: 'sm-notification-dialog',
  standalone: true,
  imports: [CommonModule, DialogModule, ButtonModule],
  template: `
    <p-dialog
      [(visible)]="visible"
      [modal]="true"
      [breakpoints]="{ '960px': '75vw', '640px': '90vw' }"
      [style]="{ width: '450px' }"
      header="Notification Details"
      [closeOnEscape]="true"
      [dismissableMask]="true"
      styleClass="overflow-hidden"
      (onHide)="onHide()">
      <ng-container *ngIf="notification">
        <div class="p-4">
          <div class="flex align-items-center gap-2 mb-4">
            <i [class]="getNotificationIcon(notification.type) + ' text-xl'"
               [ngClass]="{
                 'text-primary': notification.type === 'SYSTEM',
                 'text-500': notification.type === 'USER',
                 'text-700': notification.type === 'NONE'
               }"></i>
            <span class="text-sm text-600">{{notification.type}}</span>
          </div>

          <div class="text-lg mb-4 line-height-3 overflow-hidden text-break">
            {{notification.message}}
          </div>

          <div class="flex justify-content-between align-items-center flex-wrap gap-2">
            <span class="text-sm text-500">
              {{getFormattedDate(notification.createdTime) | date:'MMM d, y, h:mm a'}}
            </span>

            <div class="flex gap-2">
              <button *ngIf="notification && !notification.read"
                      pButton
                      type="button"
                      icon="pi pi-check"
                      class="p-button-rounded p-button-text"
                      (click)="onMarkAsRead()">
              </button>
              <button pButton
                      type="button"
                      icon="pi pi-times"
                      class="p-button-rounded p-button-text"
                      (click)="visible = false">
              </button>
            </div>
          </div>
        </div>
      </ng-container>
    </p-dialog>
  `
})
export class NotificationDialogComponent {
  @Input() visible = false;
  @Input() notification: INotification | null = null;
  @Output() visibleChange = new EventEmitter<boolean>();
  @Output() hide = new EventEmitter<void>();
  @Output() markAsRead = new EventEmitter<INotification>();

  onHide(): void {
    this.visible = false;
    this.visibleChange.emit(false);
    this.hide.emit();
  }

  onMarkAsRead(): void {
    if (this.notification) {
      this.markAsRead.emit(this.notification);
      this.visible = false;
    }
  }

  getNotificationIcon(type: string | null | undefined): string {
    switch (type) {
      case 'SYSTEM': return 'pi pi-cog';
      case 'INFO': return 'pi pi-info-circle';
      case 'WARNING': return 'pi pi-exclamation-triangle';
      case 'ERROR': return 'pi pi-times-circle';
      default: return 'pi pi-bell';
    }
  }

  getFormattedDate(date: any): string {
    if (!date) return '';
    return typeof date.toISOString === 'function' ? date.toISOString() : date;
  }
}
