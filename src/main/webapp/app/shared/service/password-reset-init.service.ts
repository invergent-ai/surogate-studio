import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Store } from '@ngxs/store';
import { Selectors } from '../state/selectors';

@Injectable({ providedIn: 'root' })
export class PasswordResetInitService {
  private resourceUrl: string;

  constructor(
    private http: HttpClient,
    protected store: Store,
  ) {
    store.select(Selectors.getEndpointFor('api/account/reset-password/init')).subscribe((url) => {
      this.resourceUrl = url;
    });
  }

  save(mail: string): Observable<{}> {
    return this.http.post(this.resourceUrl, mail);
  }
}
