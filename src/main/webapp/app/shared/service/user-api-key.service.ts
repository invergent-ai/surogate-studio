// src/app/shared/service/user-api-key.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Store } from '@ngxs/store';
import { ApiKeyProvider, ApiKeyType } from '../model/enum/api-key.enum';
import { Selectors } from '../state/selectors';

export interface UserApiKey {
  id?: string;
  type: ApiKeyType;
  provider: ApiKeyProvider;
  apiKey?: string;
  maskedApiKey?: string;
}

@Injectable({ providedIn: 'root' })
export class UserApiKeyService {
  protected resourceUrl: string;

  constructor(
    protected http: HttpClient,
    protected store: Store,
  ) {
    store.select(Selectors.getEndpointFor('/api/user-api-keys')).subscribe(url => {
      this.resourceUrl = url;
    });
  }

  getAll(type: ApiKeyType): Observable<UserApiKey[]> {
    const params = new HttpParams().set('type', type);
    return this.http.get<UserApiKey[]>(this.resourceUrl, { params });
  }

  hasKey(provider: ApiKeyProvider, type: ApiKeyType): Observable<boolean> {
    const params = new HttpParams().set('type', type);
    return this.http.get<boolean>(`${this.resourceUrl}/${provider}/exists`, { params });
  }

  save(dto: UserApiKey): Observable<UserApiKey> {
    return this.http.post<UserApiKey>(this.resourceUrl, dto);
  }

  delete(provider: ApiKeyProvider, type: ApiKeyType): Observable<void> {
    const params = new HttpParams().set('type', type);
    return this.http.delete<void>(`${this.resourceUrl}/${provider}`, { params });
  }
}
