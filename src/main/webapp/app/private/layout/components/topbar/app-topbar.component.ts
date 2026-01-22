import { INotification } from '../../../../shared/model/notification.model';
import { map } from 'rxjs/operators';
import { lastValueFrom } from 'rxjs';
import { TrackerService } from '../../../../shared/service/tracker.service';
import { AccountService } from '../../../../shared/service/account.service';
import { NotificationFacade } from '../../../../shared/state/notification.facade';
import { Store } from '@ngxs/store';
import { AfterViewInit, Component, ElementRef, ViewChild } from '@angular/core';
import { ColorScheme, LayoutService } from '../../../../shared/service/theme/app-layout.service';
import { NotificationListComponent } from '../../../../shared/components/notifications/notification-list.component';
import { AppSidebarComponent } from '../sidebar/app-sidebar.component';
import { OverlayPanel } from 'primeng/overlaypanel';
import { environment } from '../../../../../environments/environment';
import { Bell, Expand, Moon, PanelLeftClose, PanelLeftOpen, Pencil, Shrink, Sun, SunMoon } from 'lucide-angular';

@Component({
  selector: 'sm-topbar',
  templateUrl: './app-topbar.component.html',
  styleUrls: ['./app-topbar.component.scss'],
})
export class AppTopbarComponent implements AfterViewInit {
  @ViewChild('menubutton') menuButton!: ElementRef;
  @ViewChild('op') overlayPanel!: OverlayPanel;
  @ViewChild(AppSidebarComponent) appSidebar!: AppSidebarComponent;
  @ViewChild('notificationList') notificationList?: NotificationListComponent;

  get sidebarOpen() {
    return this.layoutService.state.staticMenuMobileActive || !this.layoutService.state.staticMenuDesktopInactive;
  }

  get scale(): number {
    return this.layoutService.config.scale;
  }

  set scale(_val: number) {
    this.layoutService.config.scale = _val;
    this.applyScale();
    this.layoutService.onConfigUpdate();
  }

  get colorScheme(): ColorScheme {
    return this.layoutService.config.colorScheme;
  }

  set colorScheme(_val: ColorScheme) {
    this.layoutService.changeColorScheme(_val);
  }

  dialogVisible = false;
  selectedNotification: INotification | null = null;
  unreadCount = 0;

  constructor(
    public layoutService: LayoutService,
    public el: ElementRef,
    private store: Store,
    private notificationFacade: NotificationFacade,
    private accountService: AccountService,
    private trackerService: TrackerService,
  ) {
    this.initializeWebSocket();
  }

  ngAfterViewInit() {}

  private async initializeWebSocket() {
    const user = await lastValueFrom(this.accountService.identity());

    // Load initial notifications
    await lastValueFrom(
      this.notificationFacade.loadNotifications({
        page: 0,
        size: 20,
        sort: ['createdTime,desc']
      })
    );

    // Listen for new notifications via WebSocket
    this.trackerService.stomp
      .watch('/topic/notifications/' + user.login)
      .pipe(
        map(message => {
          const parsedMessage = JSON.parse(message.body);
          return parsedMessage as INotification;
        })
      ).subscribe({
      next: (notification) => {
        this.notificationFacade.addNotification(notification);
      },
      error: (error) => {
        console.error('WebSocket error:', error);
      }
    });
  }

  onUnreadCountChange(count: number): void {
    this.unreadCount = count;
  }

  onMarkAsRead(notification: INotification): void {
    if (notification.id) {
      this.notificationFacade.markAsRead(notification.id).subscribe();
    }
  }

  onDialogHide(): void {
    setTimeout(() => {
      this.selectedNotification = null;
    }, 100);
  }

  onMenuButtonClick(): void {
    this.layoutService.onMenuToggle();
  }

  decrementScale() {
    this.scale--;
    this.applyScale();
  }

  incrementScale() {
    this.scale++;
    this.applyScale();
  }

  applyScale() {
    document.documentElement.style.fontSize = this.scale + "px";
  }

  protected readonly environment = environment;
  protected readonly PanelLeftClose = PanelLeftClose;
  protected readonly Moon = Moon;
  protected readonly Expand = Expand;
  protected readonly Shrink = Shrink;
  protected readonly Bell = Bell;
  protected readonly Sun = Sun;
  protected readonly PanelLeftOpen = PanelLeftOpen;
}
