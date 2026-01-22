import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Store } from '@ngxs/store';
import { Selectors } from '../state/selectors';

@Injectable({ providedIn: 'root' })
export class ActivateService {
  private resourceUrl: string;

  constructor(
    private http: HttpClient,
    private store: Store,
  ) {
    store.select(Selectors.getEndpointFor('api/activate')).subscribe((url) => {
      this.resourceUrl = url;
    });
  }

  get(key: string): Observable<{}> {
    return this.http.get(this.resourceUrl, {
      params: new HttpParams().set('key', key),
    });
  }
}
