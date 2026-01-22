import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { IEnvironmentVariable, NewEnvironmentVariable } from '../model/environment-variable.model';
import { createRequestOption } from '../util/request-util';
import { Store } from '@ngxs/store';
import { Selectors } from '../state/selectors';

export type PartialUpdateEnvironmentVariable = Partial<IEnvironmentVariable> & Pick<IEnvironmentVariable, 'id'>;

export type EntityResponseType = HttpResponse<IEnvironmentVariable>;
export type EntityArrayResponseType = HttpResponse<IEnvironmentVariable[]>;

@Injectable({ providedIn: 'root' })
export class EnvironmentVariableService {
  private resourceUrl: string;

  constructor(
    protected http: HttpClient,
    protected store: Store,
  ) {
    store.select(Selectors.getEndpointFor('/api/environment-variables')).subscribe((url) => {
      this.resourceUrl = url;
    });
  }

  create(environmentVariable: IEnvironmentVariable | NewEnvironmentVariable): Observable<EntityResponseType> {
    return this.http.post<IEnvironmentVariable>(this.resourceUrl, environmentVariable, { observe: 'response' });
  }

  update(environmentVariable: IEnvironmentVariable): Observable<EntityResponseType> {
    return this.http.put<IEnvironmentVariable>(
      `${this.resourceUrl}/${this.getEnvironmentVariableIdentifier(environmentVariable)}`,
      environmentVariable,
      { observe: 'response' },
    );
  }

  partialUpdate(environmentVariable: PartialUpdateEnvironmentVariable): Observable<EntityResponseType> {
    return this.http.patch<IEnvironmentVariable>(
      `${this.resourceUrl}/${this.getEnvironmentVariableIdentifier(environmentVariable)}`,
      environmentVariable,
      { observe: 'response' },
    );
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IEnvironmentVariable>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IEnvironmentVariable[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  getEnvironmentVariableIdentifier(environmentVariable: Pick<IEnvironmentVariable, 'id'>): string {
    return environmentVariable.id;
  }
}
