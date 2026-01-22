import { Injectable } from '@angular/core';
import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Observable } from 'rxjs';
import { StateStorageService } from '../service/state-storage.service';
import { Store } from '@ngxs/store';
import { Selectors } from '../state/selectors';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  serverApiUrl = this.store.selectSnapshot(Selectors.getEndpointFor(''));

  constructor(
    private stateStorageService: StateStorageService,
    private store: Store,
  ) {}

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    if (!request.url || (request.url.startsWith('http') && !(this.serverApiUrl && request.url.startsWith(this.serverApiUrl)))) {
      return next.handle(request);
    }

    const token: string | null = this.stateStorageService.getAuthenticationToken();
    if (token) {
      request = request.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`,
        },
        withCredentials: true
      });
    }
    return next.handle(request);
  }
}
