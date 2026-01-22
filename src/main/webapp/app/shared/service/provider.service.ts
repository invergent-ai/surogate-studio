// provider.service.ts
import {Injectable} from '@angular/core';
import {HttpClient, HttpResponse} from '@angular/common/http';
import {Observable} from 'rxjs';
import { Store } from '@ngxs/store';
import { Selectors } from '../state/selectors';

export interface ProviderDTO {
  id: string;
  name: string;
  description?: string;
  active?: boolean;
}

export type ProviderArrayResponseType = HttpResponse<ProviderDTO[]>;

@Injectable({ providedIn: 'root' })
export class ProviderService {
  protected resourceUrl: string;

  constructor(
    protected http: HttpClient,
    protected store: Store,
  ) {
    store.select(Selectors.getEndpointFor('api/provider')).subscribe((url) => {
      this.resourceUrl = url;
    });
  }

  getActiveProviders(): Observable<ProviderDTO[]> {
    return this.http.get<ProviderDTO[]>(`${this.resourceUrl}/active`);
  }
}
