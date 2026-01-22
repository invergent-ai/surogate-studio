import { Component, ElementRef, HostBinding, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { animate, state, style, transition, trigger } from '@angular/animations';
import { Subscription } from 'rxjs';
import { MenuService } from '../../service/app-menu.service';
import { LayoutService } from '../../../../shared/service/theme/app-layout.service';
import { AccountService } from '../../../../shared/service/account.service';

@Component({
  // eslint-disable-next-line @angular-eslint/component-selector
  selector: '[sm-menuitem]',
  template: `
    <ng-container>
      <div *ngIf="root && item.visible !== false && hasAccess(item.authorities) && !hiddenForRoles(item.hideAuthorities)"
           class="layout-menuitem-root-text" [ngClass]="item.class">{{ item.label }}</div>
      <a
        *ngIf="(!item.routerLink || item.items) && item.visible !== false && hasAccess(item.authorities) && !hiddenForRoles(item.hideAuthorities)"
        style="text-decoration: none"
        [attr.href]="item.url"
        (click)="itemClick($event)"
        [ngClass]="item.class"
        [attr.target]="item.target"
        tabindex="0"
        pRipple
        [pTooltip]="item.label"
        [tooltipDisabled]="true"
      >
        @if (item.lucide) {
          <i-lucide [img]="item.lucide" class="w-1.5rem h-1.5rem layout-menuitem-icon"></i-lucide>
        } @else {
          <i [ngClass]="item.icon" class="layout-menuitem-icon"></i>
        }
        <span class="layout-menuitem-text text-sm">{{ item.label }}</span>
        <i class="pi pi-angle-down layout-submenu-toggler" *ngIf="item.items"></i>
      </a>
      <a
        *ngIf="item.routerLink && !item.items && item.visible !== false && hasAccess(item.authorities) && !hiddenForRoles(item.hideAuthorities)"
        style="text-decoration: none"
        (click)="itemClick($event)"
        [ngClass]="item.class"
        [routerLink]="item.routerLink"
        routerLinkActive="active-route-out"
        [class.active-route]="active"
        [routerLinkActiveOptions]="item.routerLinkActiveOptions || { paths: 'exact', queryParams: 'ignored', matrixParams: 'ignored', fragment: 'ignored' }"
        [fragment]="item.fragment"
        [queryParamsHandling]="item.queryParamsHandling"
        [preserveFragment]="item.preserveFragment"
        [skipLocationChange]="item.skipLocationChange"
        [replaceUrl]="item.replaceUrl"
        [state]="item.state"
        [queryParams]="item.queryParams"
        [attr.target]="item.target"
        tabindex="0"
        pRipple
        [pTooltip]="item.label"
        [tooltipDisabled]="true"
      >
        <img *ngIf="item.image" [src]="item.image" [alt]="item.label" width="20" height="15" class="inline-block mr-2 border-1 border-300"/>
        @if (item.lucide) {
          <i-lucide [img]="item.lucide" class="w-1.5rem h-1.5rem layout-menuitem-icon"></i-lucide>
        } @else {
          <i [ngClass]="item.icon" class="layout-menuitem-icon"></i>
        }
        <span class="layout-menuitem-text text-sm">{{ item.label }}</span>
        <i class="pi pi-angle-down layout-submenu-toggler" *ngIf="item.items"></i>
      </a>

      <ul #submenu *ngIf="item.items && item.visible !== false && hasAccess(item.authorities) && !hiddenForRoles(item.hideAuthorities)"
          [@children]="submenuAnimation">
        <ng-template ngFor let-child let-i="index" [ngForOf]="item.items">
          <li sm-menuitem [item]="child" [index]="i" [parentKey]="key" [class]="child.badgeClass"></li>
        </ng-template>
      </ul>
    </ng-container>
  `,
  animations: [
    trigger('children', [
      state('collapsed', style({
        height: '0'
      })),
      state('expanded', style({
        height: '*'
      })),
      state('hidden', style({
        display: 'none'
      })),
      state('visible', style({
        display: 'block'
      })),
      transition('collapsed <=> expanded', animate('300ms cubic-bezier(0.86, 0, 0.07, 1)'))
    ])
  ]
})
export class AppMenuitemComponent implements OnInit, OnDestroy {
  @Input() item: any;
  @Input() index!: number;
  @Input() @HostBinding('class.layout-root-menuitem') root!: boolean;
  @Input() parentKey!: string;
  @ViewChild('submenu') submenu!: ElementRef;

  active = false;
  menuSourceSubscription: Subscription;
  menuResetSubscription: Subscription;
  masterSelectionResetSubscription: Subscription;
  routeEventsSubscription: Subscription;
  key: string = '';

  constructor(
    public layoutService: LayoutService,
    public router: Router,
    public menuService: MenuService,
    public accountService: AccountService
  ) {
    this.menuSourceSubscription = this.menuService.menuSource$.subscribe(value => {
      Promise.resolve(null).then(() => {
        const isCurrentItem = !!(value.key === this.key || value.key.startsWith(this.key + '-'));
        if (this.item.keepSelection) {
          if (isCurrentItem) {
            this.active = !this.active;
            if (this.active) {
              this.menuService.loadResources(this.item?.queryParams?.id);
            } else {
              this.menuService.loadResources();
              this.menuService.onMenuStateChange({ key: '0-0' });
              this.router.navigate(['']);
            }
          } else if (value.keepSelection) {
            this.active = false;
          }
        } else {
          this.active = isCurrentItem;
          if (this.active && this.item?.reset) {
            this.menuService.loadResources();
          }
        }
      });
    });

    this.menuResetSubscription = this.menuService.resetSource$.subscribe(() => {
      this.active = false;
    });
    this.masterSelectionResetSubscription = this.menuService.resetMasterSelection$.subscribe((id) => {
      this.active = this.item?.keepSelection && this.item?.queryParams?.id === id;
    });
  }

  ngOnInit() {
    this.key = this.parentKey ? this.parentKey + '-' + this.index : String(this.index);
    if (this.item.routerLink) {
      this.updateActiveStateFromRoute();
    }
  }

  updateActiveStateFromRoute() {
    let activeRoute = this.router.isActive(this.item.routerLink[0],
      { paths: 'exact', queryParams: 'ignored', matrixParams: 'ignored', fragment: 'ignored' });

    if (activeRoute && !this.item.noroute) {
      this.menuService.onMenuStateChange({ key: this.key, routeEvent: true, keepSelection: this.item?.keepSelection });
    } else if (this.item.noroute) {
      if (this.item.routerLink?.length) {
        let url = this.item.routerLink[0];
        if (this.item.queryParams) {
          let params = '';
          for (const [key, value] of Object.entries(this.item.queryParams)) {
            if (params) {
              params += '&';
            }
            params += key + '=' + value;
          }
          url += '?' + params;
        }
        if (this.router.url.indexOf(url) === 0) {
          this.menuService.onMenuStateChange({ key: this.key, routeEvent: true, keepSelection: this.item?.keepSelection });
        }
      }
    }
  }

  itemClick(event: Event) {
    // avoid processing disabled items
    if (this.item.disabled) {
      event.preventDefault();
      return;
    }

    // execute command
    if (this.item.command) {
      this.item.command({ originalEvent: event, item: this.item });
    }

    // toggle active state
    if (this.item.items) {
      this.active = !this.active;
    } else {
      if (this.layoutService.isMobile()) {
        this.layoutService.state.staticMenuMobileActive = false;
      }
    }

    if (this.item?.reset) {
      this.menuService.reset();
    }
    this.menuService.onMenuStateChange({ key: this.key, keepSelection: this.item?.keepSelection });
  }

  get submenuAnimation() {
    return this.root ? 'expanded' : (this.active ? 'expanded' : 'collapsed');
  }

  @HostBinding('class.active-menuitem')
  get activeClass() {
    return this.active && !this.root;
  }

  public hasAccess(authorities: string[]) {
    return !authorities || !authorities.length ||
      this.accountService.hasAnyAuthority(authorities);
  }

  public hiddenForRoles(authorities: string[]) {
    return authorities?.length &&
      this.accountService.hasAnyAuthority(authorities);
  }

  ngOnDestroy() {
    if (this.menuSourceSubscription) {
      this.menuSourceSubscription.unsubscribe();
    }
    if (this.menuResetSubscription) {
      this.menuResetSubscription.unsubscribe();
    }
    if (this.masterSelectionResetSubscription) {
      this.masterSelectionResetSubscription.unsubscribe();
    }
    if (this.routeEventsSubscription) {
      this.routeEventsSubscription.unsubscribe();
    }
  }
}
