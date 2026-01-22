import {Component, OnDestroy, OnInit, Renderer2, ViewChild} from '@angular/core';
import {Subscription} from 'rxjs';
import {AppTopbarComponent} from '../topbar/app-topbar.component';
import {AppSidebarComponent} from '../sidebar/app-sidebar.component';
import {NavigationEnd, Router} from '@angular/router';
import {filter} from 'rxjs/operators';
import {LayoutService} from '../../../../shared/service/theme/app-layout.service';
import {PopulateStoreAction} from '../../../../shared/state/actions';
import {Store} from '@ngxs/store';
import {MenuService} from "../../service/app-menu.service";

@Component({
  templateUrl: './private-layout.component.html'
})
export class PrivateLayoutComponent implements OnInit, OnDestroy {
  overlayMenuOpenSubscription: Subscription;
  menuOutsideClickListener: any;
  menuScrollListener: any;

  @ViewChild(AppSidebarComponent) appSidebar!: AppSidebarComponent;
  @ViewChild(AppTopbarComponent) appTopbar!: AppTopbarComponent;

  constructor(
    public layoutService: LayoutService,
    public renderer: Renderer2,
    public router: Router,
    public store: Store,
    public menuService: MenuService
  ) {
    this.overlayMenuOpenSubscription = this.layoutService.overlayOpen$.subscribe(() => {
      if (!this.menuOutsideClickListener) {
        this.menuOutsideClickListener = this.renderer.listen('document', 'click', (event) => {
          const isOutsideClicked = !(
            this.appTopbar.el.nativeElement.isSameNode(event.target) ||
            this.appTopbar.el.nativeElement.contains(event.target) ||
            this.appTopbar.menuButton.nativeElement.isSameNode(event.target) ||
            this.appTopbar.menuButton.nativeElement.contains(event.target)
          );
          if (isOutsideClicked) {
            this.hideMenu();
          }
        });
      }
      if (this.layoutService.state.staticMenuMobileActive) {
        this.blockBodyScroll();
      }
    });

    this.router.events.pipe(filter(event => event instanceof NavigationEnd))
      .subscribe(() => {
        this.hideMenu();
      });
  }

  blockBodyScroll(): void {
    if (document.body.classList) {
      document.body.classList.add('blocked-scroll');
    } else {
      document.body.className += ' blocked-scroll';
    }
  }

  unblockBodyScroll(): void {
    if (document.body.classList) {
      document.body.classList.remove('blocked-scroll');
    } else {
      document.body.className = document.body.className.replace(new RegExp('(^|\\b)' +
        'blocked-scroll'.split(' ').join('|') + '(\\b|$)', 'gi'), ' ');
    }
  }

  hideMenu() {
    this.layoutService.state.overlayMenuActive = false;
    this.layoutService.state.staticMenuMobileActive = false;
    this.layoutService.state.menuHoverActive = false;

    if (this.menuOutsideClickListener) {
      this.menuOutsideClickListener();
      this.menuOutsideClickListener = null;
    }
    if (this.menuScrollListener) {
      this.menuScrollListener();
      this.menuScrollListener = null;
    }
    this.unblockBodyScroll();
  }

  get containerClass() {
    return {
      'layout-light': this.layoutService.config.colorScheme === 'light',
      'layout-dark': this.layoutService.config.colorScheme === 'dark',
      'layout-static': this.layoutService.config.menuMode === 'static',
      'layout-static-inactive': this.layoutService.state.staticMenuDesktopInactive && this.layoutService.config.menuMode === 'static',
      'layout-mobile-active': this.layoutService.state.staticMenuMobileActive,
      'p-ripple-disabled': !this.layoutService.config.ripple,
      'layout-sidebar-active': this.layoutService.state.sidebarActive,
      'layout-sidebar-anchored': this.layoutService.state.anchored
    };
  }

  ngOnInit() {
    this.store.dispatch(new PopulateStoreAction(this.router.url.indexOf('/projects?id=') === -1)); // Ugly hack
  }

  ngOnDestroy() {
    if (this.overlayMenuOpenSubscription) {
      this.overlayMenuOpenSubscription.unsubscribe();
    }

    if (this.menuOutsideClickListener) {
      this.menuOutsideClickListener();
    }
  }
}

