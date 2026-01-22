import {Injectable} from "@angular/core";
import {AppConfig, ColorScheme, DEFAULT_THEME, LayoutService, MenuMode} from "./app-layout.service";

@Injectable({
  providedIn: 'root'
})
export class ThemeCoordinatorService {
  constructor(private layoutService: LayoutService) {}

  applyUserPreferences(preferences: AppConfig) {
    // Transform preferences to lowercase for frontend use
    const frontendPreferences = {
      ...preferences,
      colorScheme: preferences.colorScheme ? preferences.colorScheme.toLowerCase() as ColorScheme : DEFAULT_THEME.colorScheme,
      menuMode: preferences.menuMode ? preferences.menuMode.toLowerCase().replace('slim_plus', 'slim-plus') as MenuMode : DEFAULT_THEME.menuMode,
    };

    // Update theme link immediately
    const themeLink = document.getElementById('theme-link') as HTMLLinkElement;
    if (themeLink) {
      const currentHref = themeLink.getAttribute('href');
      if (!frontendPreferences.colorScheme) {
        frontendPreferences.colorScheme = DEFAULT_THEME.colorScheme.toUpperCase() as ColorScheme;
      }
      if (currentHref) {
        const newHref = currentHref
          .replace(/theme-(light|dark)/, `theme-${frontendPreferences.colorScheme.toLowerCase()}`);
        themeLink.setAttribute('href', newHref);
      }
    }

    // Update layout service config
    this.layoutService.config = frontendPreferences;
    this.layoutService.applyScale();
    this.layoutService.onConfigUpdate();
  }
}
