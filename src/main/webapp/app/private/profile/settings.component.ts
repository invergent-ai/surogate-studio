import { Component, OnInit, ViewChild } from '@angular/core';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';

import SharedModule from 'app/shared/shared.module';
import { LANGUAGES } from 'app/config/constant/language.constants';
import { MessageModule } from 'primeng/message';
import { ConfirmationService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { Account } from '../../shared/model/account.model';
import { AccountService } from '../../shared/service/account.service';
import { InputSwitchModule } from 'primeng/inputswitch';
import { DropdownModule } from 'primeng/dropdown';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { MultiSelectModule } from 'primeng/multiselect';
import { fromDTO, NotificationPreferences, toDTO } from '../../shared/model/notification-settings.model';
import { ConfirmPopupModule } from 'primeng/confirmpopup';
import { LoginService } from '../../shared/service/login.service';
import { Router } from '@angular/router';
import { Accordion, AccordionModule } from 'primeng/accordion';
import { displayError } from '../../shared/util/error.util';
import { displaySuccess, displayWarning } from '../../shared/util/success.util';
import { Store } from '@ngxs/store';
import { PageComponent } from '../../shared/components/page/page.component';
import { PageLoadComponent } from '../../shared/components/page-load/page-load.component';
import { CardModule } from 'primeng/card';
import { LayoutService } from '../../shared/service/theme/app-layout.service';

const initialAccount: Account = {} as Account;
const initialNotificationPreferences: NotificationPreferences = {
  SERVICE_CONTROL: false,
  ACCOUNT: false,
  BILLING: false,
  NEWS: false,
  MAINTENANCE: false
};

@Component({
  selector: 'sm-settings',
  standalone: true,
  imports: [
    SharedModule,
    FormsModule,
    ReactiveFormsModule,
    MessageModule,
    ToastModule,
    InputSwitchModule,
    DropdownModule,
    ConfirmDialogModule,
    MultiSelectModule,
    ConfirmPopupModule,
    AccordionModule,
    PageComponent,
    PageLoadComponent,
    CardModule,
  ],
  templateUrl: './settings.component.html'
})
export default class SettingsComponent implements OnInit {
  readonly welcomeItems = [
    {title: 'What is Surogate ?', link: 'https://surogate.ai'},
  ];

  @ViewChild('passwordAccordion') passwordAccordion: Accordion;
  languages = LANGUAGES;
  showCurrentPassword = false;
  showNewPassword = false;
  showConfirmPassword = false;
  loading = true;

  settingsForm = new FormGroup({
    id: new FormControl<string | null>(initialAccount.id),
    firstName: new FormControl<string | null>(initialAccount.firstName, {
      validators: [Validators.required, Validators.minLength(1), Validators.maxLength(50)]
    }),
    lastName: new FormControl<string | null>(initialAccount.lastName, {
      validators: [Validators.required, Validators.minLength(1), Validators.maxLength(50)]
    }),
    email: new FormControl(initialAccount.email, {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(5), Validators.maxLength(254), Validators.email]
    }),
    langKey: new FormControl(initialAccount.langKey, { nonNullable: true }),
    activated: new FormControl(initialAccount.activated, { nonNullable: true }),
    authorities: new FormControl(initialAccount.authorities, { nonNullable: true }),
    imageUrl: new FormControl<string | null>(initialAccount.imageUrl),
    login: new FormControl(initialAccount.login, { nonNullable: true }),
    defaultProject: new FormControl(initialAccount.defaultProject, { nonNullable: true }),
    defaultZone: new FormControl(initialAccount.defaultZone, { nonNullable: true }),
    lockedUserTime: new FormControl(initialAccount.lockedUserTime, {}),
    lockedOperator: new FormControl(initialAccount.lockedOperator, {}),
    hasApps: new FormControl(initialAccount.hasApps, {}),
    cicdPipelineAutopublish: new FormControl(initialAccount.cicdPipelineAutopublish, {}),
    selectedNotifications: new FormControl<string[]>([], { nonNullable: true }),
    notificationPreferences: new FormControl<NotificationPreferences>(
      initialNotificationPreferences,
      { nonNullable: true }
    ),
    theme: new FormControl(initialAccount.theme, { nonNullable: true }),
    scale: new FormControl(initialAccount.scale, { nonNullable: true }),
    ripple: new FormControl(initialAccount.ripple, { nonNullable: true }),
    menuMode: new FormControl(initialAccount.menuMode, { nonNullable: true }),
    paymentMethod: new FormControl(initialAccount.paymentMethod, { nonNullable: true }),
    credits: new FormControl(initialAccount.credits, { nonNullable: true }),
    dataCenter: new FormControl(initialAccount.dataCenter, { nonNullable: true }),
    userType: new FormControl(initialAccount.userType, { nonNullable: true }),
    company: new FormControl(initialAccount.company),
    taxCode: new FormControl(initialAccount.taxCode),
    address: new FormControl(initialAccount.address),
    city: new FormControl(initialAccount.city),
    state: new FormControl(initialAccount.state),
    country: new FormControl(initialAccount.country),
    zip: new FormControl(initialAccount.zip),
    referralCode: new FormControl(initialAccount.referralCode),
    colorScheme: new FormControl(initialAccount.colorScheme, { nonNullable: true })
  });

  passwordForm = new FormGroup({
    currentPassword: new FormControl('', {
      validators: [Validators.required]
    }),
    newPassword: new FormControl('', {
      validators: [Validators.required, Validators.minLength(4), Validators.maxLength(50)]
    }),
    confirmPassword: new FormControl('', {
      validators: [Validators.required]
    })
  });

  constructor(
    private accountService: AccountService,
    private translateService: TranslateService,
    private store: Store,
    private confirmationService: ConfirmationService,
    private loginService: LoginService,
    private router: Router,
    private layoutService: LayoutService,
  ) {
  }

  ngOnInit(): void {
    this.layoutService.state.helpItems = this.welcomeItems;
    // First load account data
    this.accountService.identity().subscribe({
      next: account => {
        if (account) {
          this.settingsForm.patchValue({
            id: account.id,
            firstName: account.firstName,
            lastName: account.lastName,
            userType: account.userType,
            company: account.company,
            taxCode: account.taxCode,
            email: account.login,
            langKey: account.langKey,
            activated: account.activated,
            authorities: account.authorities,
            imageUrl: account.imageUrl,
            login: account.login,
            defaultProject: account.defaultProject,
            defaultZone: account.defaultZone,
            lockedUserTime: account.lockedUserTime,
            lockedOperator: account.lockedOperator,
            hasApps: account.hasApps,
            theme: account.theme,
            scale: account.scale,
            ripple: account.ripple,
            menuMode: account.menuMode,
            colorScheme: account.colorScheme,
            address: account.address,
            city: account.city,
            state: account.state,
            country: account.country,
            zip: account.zip,
          });

          // After account is loaded, load notification settings
          this.loadNotificationSettings();
          this.settingsForm.markAllAsTouched();

          this.loading = false;
        }
      },
      error: () => {
        displayError(this.store, 'Failed to load user profile');
      }
    });
  }

  private loadNotificationSettings(): void {
    this.accountService.getNotificationSettings().subscribe({
      next: settings => {
        const preferences = fromDTO(settings);
        this.settingsForm.patchValue({
          notificationPreferences: preferences
        });
      },
      error: () => {
        displayError(this.store, 'Failed to load notification preferences');
      }
    });
  }

  canChangePassword(): boolean {
    return (
      this.passwordForm.valid &&
      this.passwordForm.get('newPassword').value === this.passwordForm.get('confirmPassword').value
    );
  }

  changePassword(): void {
    if (this.canChangePassword()) {
      const currentPassword = this.passwordForm.get('currentPassword').value;
      const newPassword = this.passwordForm.get('newPassword').value;

      this.accountService.changePassword(currentPassword, newPassword).subscribe({
        next: () => {
          displaySuccess(this.store, 'Password changed successfully');

          // Reset password fields
          this.passwordForm.patchValue({
            currentPassword: '',
            newPassword: '',
            confirmPassword: ''
          });
          // Close the accordion
          if (this.passwordAccordion?.activeIndex !== null) {
            this.passwordAccordion.activeIndex = -1;
          }
        },
        error: () => {
          displayError(this.store, 'Failed to change password');
        }
      });
    }
  }

  async deleteAccountFirstConfirm(event: Event) {
    this.confirmationService.confirm({
      key: 'confirmDelete',
      target: event.target || new EventTarget,
      message: `Do you really want to delete your account?`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Yes',
      rejectLabel: 'No',
      accept: async () => this.deleteAccount(),
      reject: async () => {
      }
    });
  }

  deleteAccount(): void {
    this.confirmationService.confirm({
      header: 'Delete Account',
      message: 'Are you sure you want to delete your account?<br/> This action is irreversible. ' +
        'All of your apps will be deleted and nodes unregistered!<br/> You will not be able to access your platform wallet either!',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.accountService.deleteAccount().subscribe({
          next: async () => {
            displaySuccess(this.store, 'Account deleted successfully');
            this.loginService.logout();
            await this.router.navigate(['/login']);
          },
          error: () => {
            displayError(this.store, 'Failed to delete account');
          }
        });
      }
    });
  }

  async save(): Promise<void> {
    if (!this.settingsForm.valid) {
      displayWarning(this.store, 'Please check the form fields');
      return;
    }

    const formValue = this.settingsForm.getRawValue();
    try {
      const account = new Account(
        formValue.id,
        formValue.activated,
        formValue.authorities,
        formValue.email,
        formValue.firstName,
        formValue.langKey,
        formValue.lastName,
        formValue.login,
        formValue.imageUrl,
        formValue.defaultProject,
        formValue.defaultZone,
        formValue.lockedUserTime,
        formValue.lockedOperator,
        formValue.hasApps,
        formValue.cicdPipelineAutopublish,
        formValue.theme,
        formValue.colorScheme,
        formValue.scale,
        formValue.menuMode,
        formValue.ripple,
        formValue.paymentMethod,
        formValue.credits,
        formValue.dataCenter,
        formValue.userType,
        formValue.company,
        formValue.taxCode,
        formValue.address,
        formValue.city,
        formValue.state,
        formValue.country,
        formValue.zip,
        formValue.referralCode
      );

      // Save both account and notification settings
      await Promise.all([
        new Promise<void>((resolve, reject) => {
          this.accountService.save(account).subscribe({
            next: () => resolve(),
            error: reject
          });
        }),
        new Promise<void>((resolve, reject) => {
          const notificationSettings = toDTO(formValue.notificationPreferences);
          this.accountService.updateNotificationSettings(notificationSettings).subscribe({
            next: () => resolve(),
            error: reject
          });
        })
      ]);

      displaySuccess(this.store, 'Settings updated successfully');
      this.accountService.identity(true, false);
      if (account.langKey !== this.translateService.currentLang) {
        this.translateService.use(account.langKey);
      }
    } catch (error) {
      displayError(this.store, 'Failed to update settings');
    }
  }
}
