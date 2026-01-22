import {Injectable} from '@angular/core';
import {HttpClient, HttpResponse} from '@angular/common/http';
import { lastValueFrom, Observable } from 'rxjs';

import {ISystemConfiguration} from '../model/system-configuration.model';
import {createRequestOption} from '../util/request-util';
import { Store } from '@ngxs/store';
import { Selectors } from '../state/selectors';
import { StateStorageService } from './state-storage.service';
import { map } from 'rxjs/operators';

export type EntityResponseType = HttpResponse<ISystemConfiguration>;
export type EntityArrayResponseType = HttpResponse<ISystemConfiguration[]>;

@Injectable({ providedIn: 'root' })
export class SystemConfigurationService {
  protected resourceUrl: string;

  constructor(
    protected http: HttpClient,
    protected store: Store,
    private stateStorageService: StateStorageService,
  ) {
    store.select(Selectors.getEndpointFor('/api/system-configurations')).subscribe((url) => {
      this.resourceUrl = url;
    });
  }

  update(systemConfiguration: ISystemConfiguration): Observable<EntityResponseType> {
    return this.http.put<ISystemConfiguration>(
      `${this.resourceUrl}/${this.getSystemConfigurationIdentifier(systemConfiguration)}`,
      systemConfiguration,
      { observe: 'response' },
    );
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<ISystemConfiguration[]>(`${this.resourceUrl}`, { params: options, observe: 'response' });
  }

  getSystemConfigurationIdentifier(systemConfiguration: Pick<ISystemConfiguration, 'id'>): string {
    return systemConfiguration.id;
  }

  async getSettings(): Promise<ISystemConfiguration[] | null> {
    let settings = this.stateStorageService.innerGetSettings();
    if (!settings?.length) {
      const settings = await lastValueFrom(this.query(null)
        .pipe(map((res: HttpResponse<ISystemConfiguration[]>) => res.body ?? [])))
      this.stateStorageService.innerStoreSettings(settings);
    }
    return settings;
  }
}
