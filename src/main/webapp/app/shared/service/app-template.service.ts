import {Injectable} from '@angular/core';
import {HttpClient, HttpParams, HttpResponse} from '@angular/common/http';
import {Observable} from 'rxjs';
import {createRequestOption} from '../util/request-util';
import {IAppTemplate, NewAppTemplate} from "../model/app-template.model";
import { Store } from '@ngxs/store';
import { Selectors } from '../state/selectors';

export type EntityResponseType = HttpResponse<IAppTemplate>;
export type EntityArrayResponseType = HttpResponse<IAppTemplate[]>;

@Injectable({ providedIn: 'root' })
export class AppTemplateService {
  protected resourceUrl: string;

  constructor(
    protected http: HttpClient,
    protected store: Store,
  ) {
    store.select(Selectors.getEndpointFor('api/app-template')).subscribe((url) => {
      this.resourceUrl = url;
    });
  }

  create(appTemplate: IAppTemplate | NewAppTemplate): Observable<EntityResponseType> {
    return this.http.post<IAppTemplate>(this.resourceUrl, appTemplate, { observe: 'response' });
  }

  update(appTemplate: IAppTemplate): Observable<EntityResponseType> {
    return this.http.put<IAppTemplate>(`${this.resourceUrl}/${this.getAppTemplateIdentifier(appTemplate)}`, appTemplate, { observe: 'response' });
  }

  find(id: string): Observable<EntityResponseType> {
    return this.http.get<IAppTemplate>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IAppTemplate[]>(`${this.resourceUrl}/all`, { params: options, observe: 'response' });
  }

  // New method for filtered queries
  queryWithFilters(filters?: {
    search?: string;
    category?: string;
    providerId?: string;
    sortBy?: string;
    sortOrder?: string;
  }): Observable<EntityArrayResponseType> {
    let params = new HttpParams();

    if (filters?.search) {
      params = params.set('search', filters.search);
    }
    if (filters?.category) {
      params = params.set('category', filters.category);
    }
    if (filters?.providerId) {
      params = params.set('providerId', filters.providerId);
    }
    if (filters?.sortBy) {
      params = params.set('sortBy', filters.sortBy);
    }
    if (filters?.sortOrder) {
      params = params.set('sortOrder', filters.sortOrder);
    }

    return this.http.get<IAppTemplate[]>(this.resourceUrl, { params, observe: 'response' });
  }

  // New method to get all categories
  getCategories(): Observable<string[]> {
    return this.http.get<string[]>(`${this.resourceUrl}/categories`);
  }

  delete(id: string): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  getAppTemplateIdentifier(appTemplate: Pick<IAppTemplate, 'id'>): string {
    return appTemplate.id;
  }

}
