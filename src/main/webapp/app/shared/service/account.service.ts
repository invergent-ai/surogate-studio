import {Injectable} from '@angular/core';
import {Router} from '@angular/router';
import {HttpClient, HttpParams, HttpResponse} from '@angular/common/http';
import {TranslateService} from '@ngx-translate/core';
import {Observable, of, ReplaySubject, throwError} from 'rxjs';
import {catchError, shareReplay, tap} from 'rxjs/operators';
import {Account} from '../model/account.model';
import {StateStorageService} from './state-storage.service';
import {IUser} from "../model/user.model";
import {NotificationSettingsDTO} from "../model/notification-settings.model";
import {Selectors} from '../state/selectors';
import {Store} from '@ngxs/store';

interface IQueryParams {
  page?: number;
  size?: number;
  sort?: string;
  [key: string]: any;
}

@Injectable({ providedIn: 'root' })
export class AccountService {
  private accountResourceUrl: string;
  private userResourceUrl: string;
  private stripeResourceUrl: string;
  private userIdentity: Account | null = null;
  private authenticationState = new ReplaySubject<Account | null>(1);
  private accountCache$?: Observable<Account> | null;

  constructor(
    private translateService: TranslateService,
    private http: HttpClient,
    private stateStorageService: StateStorageService,
    private router: Router,
    store: Store,
  ) {
    store.select(Selectors.getEndpointFor('api/account')).subscribe((url) => {
      this.accountResourceUrl = url;
    });
    store.select(Selectors.getEndpointFor('api/users')).subscribe((url) => {
      this.userResourceUrl = url;
    });
    store.select(Selectors.getEndpointFor('api/stripe')).subscribe((url) => {
      this.stripeResourceUrl = url;
    });
  }

  save(account: Account): Observable<{}> {
    return this.http.post(this.accountResourceUrl, account);
  }

  authenticate(identity: Account | null): void {
    this.userIdentity = identity;
    this.authenticationState.next(this.userIdentity);
    if (!identity) {
      this.accountCache$ = null;
    }
  }

  deleteAccount(): Observable<void> {
    return this.http.delete<void>(`${this.accountResourceUrl}`);
  }


  changePassword(currentPassword: string, newPassword: string): Observable<void> {
    return this.http.post<void>(
      `${this.accountResourceUrl}/change-password`,
      { currentPassword, newPassword }
    );
  }

  getNotificationSettings(): Observable<NotificationSettingsDTO> {
    return this.http.get<NotificationSettingsDTO>(
      `${this.accountResourceUrl}/notification-settings`
    );
  }

  updateNotificationSettings(settings: NotificationSettingsDTO): Observable<void> {
    return this.http.put<void>(
      `${this.accountResourceUrl}/notification-settings`,
      settings
    );
  }

  query(req?: IQueryParams): Observable<HttpResponse<IUser[]>> {
    return this.http
      .get<IUser[]>(this.userResourceUrl, {
        observe: 'response',
        params: req ? this.createHttpParams(req) : new HttpParams()
      })
      .pipe(
        catchError(error => {
          console.error('Error fetching users:', error);
          return throwError(() => error);
        })
      );
  }

  search(query: string): Observable<IUser[]> {
    return this.http.get<IUser[]>(`${this.userResourceUrl}/search`, {
      params: {query},
    });
  }

  private createHttpParams(req: IQueryParams): HttpParams {
    let params = new HttpParams();
    Object.keys(req).forEach(key => {
      if (req[key] !== null && req[key] !== undefined) {
        params = params.set(key, req[key].toString());
      }
    });
    return params;
  }

  hasAnyAuthority(authorities: string[] | string): boolean {
    if (!this.userIdentity) {
      return false;
    }
    if (!Array.isArray(authorities)) {
      authorities = [authorities];
    }
    return this.userIdentity.authorities.some((authority: string) => authorities.includes(authority));
  }

  identity(force = false, navigateToPreviousUrl = true): Observable<Account | null> {
    if (!this.accountCache$ || force) {
      this.accountCache$ = this.fetch().pipe(
        tap((account: Account) => {
          this.authenticate(account);

          // After retrieve the account info, the language will be changed to
          // the user's preferred language configured in the account setting
          // unless user have choosed other language in the current session
          if (!this.stateStorageService.getLocale()) {
            this.translateService.use(account.langKey);
          }

          if (navigateToPreviousUrl)
            this.navigateToStoredUrl();
        }),
        shareReplay(),
      );
    }
    return this.accountCache$.pipe(catchError(() => of(null)));
  }

  isAuthenticated(): boolean {
    return this.userIdentity !== null;
  }

  getAuthenticationState(): Observable<Account | null> {
    return this.authenticationState.asObservable();
  }

  private fetch(): Observable<Account> {
    return this.http.get<Account>(this.accountResourceUrl);
  }

  private navigateToStoredUrl(): void {
    // previousState can be set in the authExpiredInterceptor and in the userRouteAccessService
    // if login is successful, go to stored previousState and clear previousState
    const previousUrl = this.stateStorageService.getUrl();
    if (previousUrl) {
      this.router.navigateByUrl(previousUrl);
    }
  }

}
