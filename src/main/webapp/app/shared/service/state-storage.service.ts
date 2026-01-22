import {Injectable} from '@angular/core';
import {ISystemConfiguration} from "../model/system-configuration.model";
import {AppConfig, DEFAULT_THEME} from "./theme/app-layout.service";

@Injectable({ providedIn: 'root' })
export class StateStorageService {
  private previousUrlKey = 'previousUrl';
  private referrerKey = 'referrer';
  private authenticationKey = 'sm-authenticationToken';
  private localeKey = 'locale';
  private settingsKey = 'settings';
  private themeKey = 'theme-preferences';

  innerStoreSettings(settings: ISystemConfiguration[]): void {
    sessionStorage.setItem(this.settingsKey, JSON.stringify(settings));
  }

  innerGetSettings(): ISystemConfiguration[] | null {
    const settings = sessionStorage.getItem(this.settingsKey);
    return settings ? (JSON.parse(settings) as ISystemConfiguration[] | null) : null;
  }

  clearSettings(): void {
    sessionStorage.removeItem(this.settingsKey);
  }

  storeUrl(url: string): void {
    sessionStorage.setItem(this.previousUrlKey, JSON.stringify(url));
  }

  storeReferrer(referrer: string): void {
    sessionStorage.setItem(this.referrerKey, JSON.stringify(referrer));
  }

  getUrl(): string | null {
    const previousUrl = sessionStorage.getItem(this.previousUrlKey);
    return previousUrl ? (JSON.parse(previousUrl) as string | null) : previousUrl;
  }

  getReferrer(): string | null {
    const referrer = sessionStorage.getItem(this.referrerKey);
    return referrer ? (JSON.parse(referrer) as string | null) : referrer;
  }

  clearUrl(): void {
    sessionStorage.removeItem(this.previousUrlKey);
  }

  clearReferrer(): void {
    sessionStorage.removeItem(this.referrerKey);
  }

  storeAuthenticationToken(authenticationToken: string, rememberMe: boolean): void {
    authenticationToken = JSON.stringify(authenticationToken);
    this.clearAuthenticationToken();
    if (rememberMe) {
      localStorage.setItem(this.authenticationKey, authenticationToken);
    } else {
      sessionStorage.setItem(this.authenticationKey, authenticationToken);
    }
  }

  getAuthenticationToken(): string | null {
    const authenticationToken = localStorage.getItem(this.authenticationKey) ?? sessionStorage.getItem(this.authenticationKey);
    return authenticationToken ? (JSON.parse(authenticationToken) as string | null) : authenticationToken;
  }

  clearAuthenticationToken(): void {
    sessionStorage.removeItem(this.authenticationKey);
    localStorage.removeItem(this.authenticationKey);
  }

  getLocale(): string | null {
    return sessionStorage.getItem(this.localeKey);
  }

  storeThemePreferences(preferences: AppConfig): void {
    sessionStorage.setItem(this.themeKey, JSON.stringify(preferences));
  }

  getThemePreferences(): AppConfig | null {
    const preferences = sessionStorage.getItem(this.themeKey);
    return preferences ? JSON.parse(preferences) : null;
  }

  storeDefaultTheme(): void {
    this.storeThemePreferences(DEFAULT_THEME);
  }

  clearThemePreferences(): void {
    this.storeDefaultTheme();
    sessionStorage.removeItem(this.themeKey);
  }
}
