import {Injectable} from '@angular/core';
import {BehaviorSubject, Subject} from 'rxjs';
import {HttpClient} from '@angular/common/http';
import {StateStorageService} from "../state-storage.service";
import { Store } from '@ngxs/store';
import { Selectors } from '../../state/selectors';

export type MenuMode = 'static';
export type ColorScheme = 'light' | 'dark';

export interface AppConfig {
  colorScheme: ColorScheme;
  ripple: boolean;
  menuMode: MenuMode;
  scale: number;
}

interface LayoutState {
  staticMenuDesktopInactive: boolean;
  overlayMenuActive: boolean;
  rightMenuActive: boolean;
  staticMenuMobileActive: boolean;
  menuHoverActive: boolean;
  sidebarActive: boolean;
  helpItems: HelpItem[];
  anchored: boolean;
}

export interface HelpItem {
  title: string;
  link: string;
}

export const DEFAULT_THEME: AppConfig = {
  ripple: true,
  menuMode: 'static',
  colorScheme: 'light',
  scale: 13,
};

@Injectable({
  providedIn: 'root',
})
export class LayoutService {
  config: AppConfig = {...DEFAULT_THEME};
  state: LayoutState = {
    staticMenuDesktopInactive: false,
    overlayMenuActive: false,
    rightMenuActive: false,
    staticMenuMobileActive: false,
    menuHoverActive: false,
    sidebarActive: false,
    helpItems: [] as HelpItem[],
    anchored: false
  };

  private configUpdate = new Subject<AppConfig>();
  private overlayOpen = new Subject<any>();
  private themeReset = new BehaviorSubject<boolean>(false);

  configUpdate$ = this.configUpdate.asObservable();
  overlayOpen$ = this.overlayOpen.asObservable();

  protected resourceUrl: string;

  constructor(
    private http: HttpClient,
    private store: Store,
    private stateStorage: StateStorageService
  ) {
    store.select(Selectors.getEndpointFor('api/account')).subscribe((url) => {
      this.resourceUrl = url;
    });

    this.initializeTheme();
  }

  private initializeTheme() {
    const storedPreferences = this.stateStorage.getThemePreferences();

    if (storedPreferences) {
      this.config = storedPreferences;
      this.applyTheme(storedPreferences);
    }
  }

  private applyTheme(preferences: AppConfig) {
    const themeLink = document.getElementById('theme-link') as HTMLLinkElement;
    if (themeLink) {
      const currentHref = themeLink.getAttribute('href');
      if (!preferences.colorScheme) {
        preferences.colorScheme = DEFAULT_THEME.colorScheme;
      }
      if (currentHref) {
        const newHref = currentHref
          .replace(/theme-(light|dark)/, `theme-${preferences.colorScheme.toLowerCase()}`)
        themeLink.setAttribute('href', newHref);
      }
    }
    this.applyScale();
    this.configUpdate.next(preferences);
  }

  private prepareBackendConfig(config: AppConfig) {
    return {
      ...config,
      colorScheme: config.colorScheme ? config.colorScheme.toUpperCase() : DEFAULT_THEME.colorScheme.toUpperCase(),
      menuMode: config.menuMode ? config.menuMode.toUpperCase() : DEFAULT_THEME.menuMode.toUpperCase(),
    };
  }

  onConfigUpdate() {
    this.configUpdate.next(this.config);
    this.stateStorage.storeThemePreferences(this.config);

    const backendConfig = this.prepareBackendConfig(this.config);
    this.http.put(`${this.resourceUrl}/theme-settings`, backendConfig).subscribe({
      error: (error) => console.error('Error saving theme settings:', error)
    });
  }

  resetThemeLocal() {
    this.config = {...DEFAULT_THEME};
    this.state = {
      staticMenuDesktopInactive: false,
      overlayMenuActive: false,
      rightMenuActive: false,
      staticMenuMobileActive: false,
      menuHoverActive: false,
      sidebarActive: false,
      helpItems: [] as HelpItem[],
      anchored: false
    };

    const themeLink = document.getElementById('theme-link') as HTMLLinkElement;
    if (themeLink) {
      const newHref = themeLink.getAttribute('href')!
        .replace(/theme-(light|dark)/, `theme-${this.config.colorScheme.toLowerCase()}`);
      themeLink.setAttribute('href', newHref);
    }

    this.applyScale();
    this.stateStorage.clearThemePreferences();
    this.themeReset.next(true);
  }

  onMenuToggle() {
    if (this.isDesktop()) {
      this.state.staticMenuDesktopInactive = !this.state.staticMenuDesktopInactive;
    } else {
      this.state.staticMenuMobileActive = !this.state.staticMenuMobileActive;
      if (this.state.staticMenuMobileActive) {
        this.overlayOpen.next(null);
      }
    }

    console.log(this.state);
  }

  isDesktop() {
    return window.innerWidth > 991;
  }

  isMobile() {
    return !this.isDesktop();
  }

  applyScale() {
    document.documentElement.style.fontSize = this.config.scale + "px";
  }

  public changeColorScheme(colorScheme: ColorScheme) {
    const themeLink = <HTMLLinkElement>(
      document.getElementById("theme-link")
    );
    const themeLinkHref = themeLink.getAttribute("href");
    const currentColorScheme =
      "theme-" + this.config.colorScheme;
    const newColorScheme = "theme-" + colorScheme;
    document.documentElement.classList.remove(currentColorScheme);
    document.documentElement.classList.add(newColorScheme);
    const newHref = themeLinkHref!.replace(
      currentColorScheme,
      newColorScheme
    );
    this.replaceThemeLink(
      newHref,
      () => {
        this.config.colorScheme = colorScheme;
        this.onConfigUpdate();
      },
      "theme-link"
    );
  }

  private replaceThemeLink(href: string, onComplete: Function, linkId: string) {
    const id = linkId;
    const themeLink = <HTMLLinkElement>document.getElementById(id);
    const cloneLinkElement = <HTMLLinkElement>themeLink.cloneNode(true);

    cloneLinkElement.setAttribute("href", href);
    cloneLinkElement.setAttribute("id", id + "-clone");

    themeLink.parentNode!.insertBefore(
      cloneLinkElement,
      themeLink.nextSibling
    );

    cloneLinkElement.addEventListener("load", () => {
      themeLink.remove();
      cloneLinkElement.setAttribute("id", id);
      onComplete();
    });
  }
}
