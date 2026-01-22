export enum NotificationCategory {
  SERVICE_CONTROL = 'SERVICE_CONTROL',
  ACCOUNT = 'ACCOUNT',
  BILLING = 'BILLING',
  NEWS = 'NEWS',
  MAINTENANCE = 'MAINTENANCE'
}

export type NotificationPreferences = Record<NotificationCategory, boolean>;

export interface NotificationSettingsDTO {
  settings: Partial<Record<NotificationCategory, boolean>>;
}

// Helper functions
export function toDTO(preferences: NotificationPreferences): NotificationSettingsDTO {
  return {
    settings: {
      SERVICE_CONTROL: !!preferences.SERVICE_CONTROL,
      ACCOUNT: !!preferences.ACCOUNT,
      BILLING: !!preferences.BILLING,
      NEWS: !!preferences.NEWS,
      MAINTENANCE: !!preferences.MAINTENANCE
    }
  };
}

export function fromDTO(dto: NotificationSettingsDTO): NotificationPreferences {
  return {
    SERVICE_CONTROL: !!dto.settings.SERVICE_CONTROL,
    ACCOUNT: !!dto.settings.ACCOUNT,
    BILLING: !!dto.settings.BILLING,
    NEWS: !!dto.settings.NEWS,
    MAINTENANCE: !!dto.settings.MAINTENANCE
  };
}
