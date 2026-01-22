import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Store } from '@ngxs/store';
import { Selectors } from '../state/selectors';


@Injectable({ providedIn: 'root' })
export class PasswordService {
  private resourceUrl: string;

  constructor(
    private http: HttpClient,
    protected store: Store,
  ) {
    store.select(Selectors.getEndpointFor('api/account/change-password')).subscribe((url) => {
      this.resourceUrl = url;
    });
  }

  save(newPassword: string, currentPassword: string): Observable<{}> {
    return this.http.post(this.resourceUrl, { currentPassword, newPassword });
  }
}
