import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { Bean, Beans, ConfigProps, Env, PropertySource } from '../model/configuration.model';
import { Selectors } from '../state/selectors';
import { Store } from '@ngxs/store';

@Injectable({ providedIn: 'root' })
export class ConfigurationService {
  private configpropsUrl: string;
  private envUrl: string;

  constructor(
    private http: HttpClient,
    private store: Store,
  ) {
    store.select(Selectors.getEndpointFor('/management/configprops')).subscribe((url) => {
      this.configpropsUrl = url;
    });
    store.select(Selectors.getEndpointFor('/management/env')).subscribe((url) => {
      this.envUrl = url;
    });
  }

  getBeans(): Observable<Bean[]> {
    return this.http.get<ConfigProps>(this.configpropsUrl).pipe(
      map(configProps =>
        Object.values(
          Object.values(configProps.contexts)
            .map(context => context.beans)
            .reduce((allBeans: Beans, contextBeans: Beans) => ({ ...allBeans, ...contextBeans })),
        ),
      ),
    );
  }

  getPropertySources(): Observable<PropertySource[]> {
    return this.http.get<Env>(this.envUrl).pipe(map(env => env.propertySources));
  }
}
