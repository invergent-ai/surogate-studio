import {Injectable} from '@angular/core';
import {HttpClient, HttpResponse} from '@angular/common/http';
import {Observable} from 'rxjs';
import {createRequestOption} from '../util/request-util';
import {Store} from '@ngxs/store';
import {Selectors} from '../state/selectors';
import {IDatabase} from "../model/database.model";

export type EntityResponseType = HttpResponse<IDatabase>;
export type EntityArrayResponseType = HttpResponse<IDatabase[]>;

@Injectable({providedIn: 'root'})
export class DatabaseService {
  protected resourceUrl: string;

  constructor(
    protected http: HttpClient,
    protected store: Store,
  ) {
    store.select(Selectors.getEndpointFor('/api/databases')).subscribe((url) => {
      this.resourceUrl = url;
    });
  }

  save(database: IDatabase): Observable<HttpResponse<IDatabase>> {
    return this.http.post<any>(this.resourceUrl, database, { observe: 'response' });
  }

  deploy(database: IDatabase): Observable<HttpResponse<IDatabase>> {
    return this.http.post<any>(`${this.resourceUrl}/deploy`, database, { observe: 'response' });
  }

  redeploy(database: IDatabase): Observable<HttpResponse<IDatabase>> {
    return this.http.post<any>(`${this.resourceUrl}/redeploy`, database, { observe: 'response' });
  }

  password(database: IDatabase): Observable<any> {
    return this.http.post<any>(`${this.resourceUrl}/password`, database, { observe: 'response' });
  }

  delete(id: string, keepVolumes: boolean): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}${keepVolumes ? '/keep' : ''}/${id}`, {observe: 'response'});
  }

  update(database: IDatabase): Observable<EntityResponseType> {
    return this.http.put<IDatabase>(`${this.resourceUrl}/${this.getDatabaseIdentifier(database)}`, database, {
      observe: 'response',
    });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IDatabase>(`${this.resourceUrl}/${id}`, {observe: 'response'});
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IDatabase[]>(this.resourceUrl, {params: options, observe: 'response'});
  }

  search(query: string): Observable<IDatabase[]> {
    return this.http.get<IDatabase[]>(`${this.resourceUrl}/search`, {
      params: { query },
    });
  }

  getDatabaseIdentifier(database: Pick<IDatabase, 'id'>): string {
    return database.id;
  }
}
