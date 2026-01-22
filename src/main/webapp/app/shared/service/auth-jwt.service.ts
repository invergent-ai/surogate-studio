import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { StateStorageService } from './state-storage.service';
import { Login } from '../model/login.model';
import { SystemConfigurationService } from './system-configuration.service';
import { Store } from '@ngxs/store';
import { Selectors } from '../state/selectors';

type JwtToken = {
  id_token: string;
};

@Injectable({ providedIn: 'root' })
export class AuthServerProvider {
  private resourceUrl: string;

  constructor(
    private http: HttpClient,
    private stateStorageService: StateStorageService,
    private systemConfigurationService: SystemConfigurationService,
    store: Store,
  ) {
    store.select(Selectors.getEndpointFor('/api/authenticate')).subscribe((url) => {
      this.resourceUrl = url;
    });
  }

  getToken(): string {
    return this.stateStorageService.getAuthenticationToken() ?? '';
  }

  login(credentials: Login): Observable<void> {
    return this.http
      .post<JwtToken>(this.resourceUrl, credentials)
      .pipe(map(response => this.authenticateSuccess(response, credentials.rememberMe)));
  }

  loginWithToken(token: string): Observable<void> {
    return new Observable(observer => {
      this.authenticateSuccess({ id_token: token }, true);
      observer.next();
    });
  }

  logout(): Observable<void> {
    return new Observable(observer => {
      this.stateStorageService.clearThemePreferences()
      this.stateStorageService.clearAuthenticationToken();
      this.stateStorageService.clearSettings();
      observer.complete();
    });
  }

  private authenticateSuccess(response: JwtToken, rememberMe: boolean): void {
    this.stateStorageService.storeAuthenticationToken(response.id_token, rememberMe);
    this.initSettings().then(() => {});
  }

  private async initSettings() {
    try {
     await this.systemConfigurationService.getSettings();
    } catch(error) {
      // Nothing to do here ... not logged in
    }
  }
}
