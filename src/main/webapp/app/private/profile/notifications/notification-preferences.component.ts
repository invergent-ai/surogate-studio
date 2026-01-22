import {Component, forwardRef} from "@angular/core";
import {CommonModule} from "@angular/common";
import {SharedModule} from "primeng/api";
import {AccordionModule} from "primeng/accordion";
import {CheckboxModule} from "primeng/checkbox";
import {ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR} from "@angular/forms";
import TranslateDirective from "../../../shared/language/translate.directive";
import {NotificationCategory, NotificationPreferences} from "../../../shared/model/notification-settings.model";

@Component({
  selector: 'sm-notification-preferences',
  standalone: true,
  imports: [CommonModule, SharedModule, AccordionModule, CheckboxModule, FormsModule, TranslateDirective],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => NotificationPreferencesComponent),
      multi: true
    }
  ],
  template: `
    <div class="grid">
      <div class="col-12">
        <p class="text-sm text-500 mb-3">
          Select which notifications you want to receive by email
        </p>

        <p-accordion>
          <p-accordionTab *ngFor="let notification of notificationTypes">
            <ng-template pTemplate="header">
              <div class="flex align-items-center">
                <span class="font-medium">{{ notification.label }}</span>
              </div>
            </ng-template>

            <div class="notification-content mt-3">
              <div class="text-sm text-600 mb-3">
                {{ notification.description }}
              </div>

              <div class="flex align-items-center">
                <p-checkbox
                  [binary]="true"
                  [ngModel]="isEnabled(notification.type)"
                  (onChange)="toggleEmailPreference(notification.type)"
                  class="mr-2"
                ></p-checkbox>
                <span class="text-sm">Receive email notifications</span>
              </div>
            </div>
          </p-accordionTab>
        </p-accordion>
      </div>
    </div>
  `
})
export class NotificationPreferencesComponent implements ControlValueAccessor {
  notificationTypes = [
    {
      type: NotificationCategory.SERVICE_CONTROL,
      label: 'Service Control',
      description: 'Receive notifications about service status changes and important updates'
    },
    {
      type: NotificationCategory.ACCOUNT,
      label: 'Account',
      description: 'Updates about your account security and profile changes'
    },
    {
      type: NotificationCategory.BILLING,
      label: 'Billing',
      description: 'Invoices and payment related notifications'
    },
    {
      type: NotificationCategory.NEWS,
      label: 'News',
      description: 'Product updates and new feature announcements'
    },
    {
      type: NotificationCategory.MAINTENANCE,
      label: 'Maintenance',
      description: 'Scheduled maintenance and system updates'
    }
  ] as const;

  settings: NotificationPreferences = {
    [NotificationCategory.SERVICE_CONTROL]: false,
    [NotificationCategory.ACCOUNT]: false,
    [NotificationCategory.BILLING]: false,
    [NotificationCategory.NEWS]: false,
    [NotificationCategory.MAINTENANCE]: false
  };

  disabled = false;
  onChange = (_: NotificationPreferences) => {};
  onTouch = () => {};

  writeValue(settings: NotificationPreferences): void {
    this.settings = settings || {
      [NotificationCategory.SERVICE_CONTROL]: false,
      [NotificationCategory.ACCOUNT]: false,
      [NotificationCategory.BILLING]: false,
      [NotificationCategory.NEWS]: false,
      [NotificationCategory.MAINTENANCE]: false
    };
  }

  registerOnChange(fn: any): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: any): void {
    this.onTouch = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  isEnabled(type: NotificationCategory): boolean {
    return !!this.settings[type];
  }

  toggleEmailPreference(type: NotificationCategory): void {
    if (this.disabled) return;

    const newSettings = {...this.settings};
    newSettings[type] = !newSettings[type];

    this.settings = newSettings;
    this.onChange(newSettings);
    this.onTouch();
  }
}
