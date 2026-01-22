import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Registration } from '../model/register.model';
import { Store } from '@ngxs/store';
import { Selectors } from '../state/selectors';

@Injectable({ providedIn: 'root' })
export class RegisterService {
  protected resourceUrl: string;

  constructor(
    private http: HttpClient,
    protected store: Store,
  ) {
    store.select(Selectors.getEndpointFor('/api/register')).subscribe((url) => {
      this.resourceUrl = url;
    });
  }

  save(registration: Registration): Observable<{}> {
    return this.http.post(this.resourceUrl, registration);
  }
}
