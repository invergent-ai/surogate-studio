import {AfterViewInit, Component, ElementRef, OnInit, ViewChild} from '@angular/core';
import {FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators} from '@angular/forms';
import {ActivatedRoute, Router, RouterModule} from '@angular/router';

import SharedModule from 'app/shared/shared.module';
import { AppConfig, ColorScheme, LayoutService } from '../../../shared/service/theme/app-layout.service';
import {finalize} from 'rxjs/operators';
import {displayError} from '../../../shared/util/error.util';
import {revalidateForm} from '../../../shared/util/form.util';
import {LoginService} from '../../../shared/service/login.service';
import {AccountService} from '../../../shared/service/account.service';
import {ThemeCoordinatorService} from "../../../shared/service/theme/theme-coordinator.service";
import {StateStorageService} from "../../../shared/service/state-storage.service";
import { Store } from '@ngxs/store';
import { environment } from 'environments/environment';
import { LucideAngularModule, Moon, Sun } from 'lucide-angular';

@Component({
  selector: 'sm-login',
  standalone: true,
  imports: [SharedModule, FormsModule, ReactiveFormsModule, RouterModule, LucideAngularModule],
  templateUrl: './login.page.html',
  styleUrls: ['./login.component.scss']
})
export default class LoginPage implements OnInit, AfterViewInit {
  protected readonly environment = environment;

  @ViewChild('username', {static: false})
  username!: ElementRef;

  cliSession: string;
  cliSessionId: string;
  returnToCli: boolean;
  referralCode: string;

  loading = false;
  loginForm = new FormGroup({
    username: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email] }),
    password: new FormControl('', {
      nonNullable: true, validators:
        [Validators.required, Validators.minLength(4), Validators.maxLength(100)]
    }),
    rememberMe: new FormControl(false, { nonNullable: true, validators: [Validators.required] }),
  });

  get colorScheme(): ColorScheme {
    return this.layoutService.config.colorScheme;
  }

  set colorScheme(_val: ColorScheme) {
    this.layoutService.changeColorScheme(_val);
  }

  constructor(
    public layoutService: LayoutService,
    private accountService: AccountService,
    private loginService: LoginService,
    private router: Router,
    private route: ActivatedRoute,
    private themeCoordinator: ThemeCoordinatorService,
    private store: Store,
    private stateStorageService: StateStorageService) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(async params => {
      this.referralCode = params['ref'];

      if (params['sess'] && params['sessId']) {
        this.cliSession = params['sess'];
        this.cliSessionId = params['sessId'];
        // CLI session
      } else {
        // if already authenticated then navigate to home page
        this.accountService.identity().subscribe(() => {
          if (this.accountService.isAuthenticated()) {
            this.router.navigate(['']);
          }
        });
      }
    });
  }

  ngAfterViewInit(): void {
    this.username.nativeElement.focus();
  }

  login(): void {
    revalidateForm(this.loginForm);
    if (!this.loginForm.valid) {
      return;
    }

    this.loading = true;
    const loginData = {
      ...this.loginForm.getRawValue(),
      cliSession: this.cliSession,
      cliHostname: this.cliSessionId
    }
    this.loginService.login(loginData)
      .pipe(finalize(() => this.loading = false))
      .subscribe({
        next: (user) => {
          if (user) {
            // Apply theme before navigation
            const userPreferences: AppConfig = {
              colorScheme: user.colorScheme,
              menuMode: user.menuMode,
              ripple: user.ripple,
              scale: user.scale,
            };
            if (userPreferences.colorScheme) {
              this.themeCoordinator.applyUserPreferences(userPreferences);
            }

            Promise.resolve(null).then(() => {
              if (!this.stateStorageService.getUrl()) {
                this.stateStorageService.clearReferrer();
                if (!this.cliSession) {
                  this.router.navigate(['']);
                } else {
                  this.returnToCli = true;
                  setTimeout(() => window.close(), 1000);
                }
              } else {
                this.stateStorageService.clearUrl();
              }
            });
          }
        },
        error: (error) => {
          displayError(this.store, error);
        }
      });
  }

  async socialLogin(type: string) {
    let redirectParam = `${location.origin}/auth`;
    const externalUrl = `${environment.SERVER_API_URL}/oauth2/authorization/${type}`;
    let redirectTo = `${externalUrl}?redirect_uri=${redirectParam}`;
    if (this.referralCode) {
      redirectTo += `&ref=${this.referralCode}`;
    }
    location.href = redirectTo;
  }

  gotoRegister() {
    this.router.navigate(['/', 'register'], { queryParams: { ref: this.referralCode } });
  }

  protected readonly Moon = Moon;
  protected readonly Sun = Sun;
}
