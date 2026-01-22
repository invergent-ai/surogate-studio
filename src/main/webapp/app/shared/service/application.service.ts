import {Injectable} from '@angular/core';
import {HttpClient, HttpResponse} from '@angular/common/http';
import {Observable} from 'rxjs';

import {IApplication} from '../model/application.model';
import {createRequestOption} from '../util/request-util';
import {Store} from '@ngxs/store';
import {Selectors} from '../state/selectors';

export type EntityResponseType = HttpResponse<IApplication>;
export type EntityArrayResponseType = HttpResponse<IApplication[]>;

@Injectable({ providedIn: 'root' })
export class ApplicationService {
  protected resourceUrl: string;

  constructor(
    protected http: HttpClient,
    protected store: Store,
  ) {
    store.select(Selectors.getEndpointFor('/api/applications')).subscribe(url => {
      this.resourceUrl = url;
    });
  }

  save(application: IApplication): Observable<IApplication> {
    return this.http.post<IApplication>(this.resourceUrl, application);
  }

  deploy(application: IApplication): Observable<IApplication> {
    return this.http.post<IApplication>(`${this.resourceUrl}/deploy`, application);
  }

  redeploy(application: IApplication): Observable<IApplication> {
    return this.http.post<IApplication>(`${this.resourceUrl}/redeploy`, application);
  }

  delete(id: string, keepVolumes: boolean): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}${keepVolumes ? '/keep' : ''}/${id}`, { observe: 'response' });
  }

  update(application: IApplication): Observable<EntityResponseType> {
    return this.http.put<IApplication>(`${this.resourceUrl}/${this.getApplicationIdentifier(application)}`, application, {
      observe: 'response',
    });
  }

  find(id: string): Observable<EntityResponseType> {
    return this.http.get<IApplication>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IApplication[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  search(query: string): Observable<IApplication[]> {
    return this.http.get<IApplication[]>(`${this.resourceUrl}/search`, {
      params: { query },
    });
  }

  getBasicInfo(): Observable<IApplication[]> {
    return this.http.get<IApplication[]>(`${this.resourceUrl}/basic`);
  }

  getApplicationIdentifier(application: Pick<IApplication, 'id'>): string {
    return application.id;
  }
}
