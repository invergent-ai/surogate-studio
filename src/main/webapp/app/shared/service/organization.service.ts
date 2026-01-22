import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { IOrganization, NewOrganization } from '../model/organization.model';
import { createRequestOption } from '../util/request-util';
import { Store } from '@ngxs/store';
import { Selectors } from '../state/selectors';

export type PartialUpdateOrganization = Partial<IOrganization> & Pick<IOrganization, 'id'>;

export type EntityResponseType = HttpResponse<IOrganization>;
export type EntityArrayResponseType = HttpResponse<IOrganization[]>;

@Injectable({ providedIn: 'root' })
export class OrganizationService {
  private resourceUrl: string;

  constructor(
    protected http: HttpClient,
    protected store: Store,
  ) {
    store.select(Selectors.getEndpointFor('/api/organizations')).subscribe((url) => {
      this.resourceUrl = url;
    });
  }

  create(organization: IOrganization | NewOrganization): Observable<EntityResponseType> {
    return this.http.post<IOrganization>(this.resourceUrl, organization, { observe: 'response' });
  }

  update(organization: IOrganization): Observable<EntityResponseType> {
    return this.http.put<IOrganization>(`${this.resourceUrl}/${this.getOrganizationIdentifier(organization)}`, organization, {
      observe: 'response',
    });
  }

  partialUpdate(organization: PartialUpdateOrganization): Observable<EntityResponseType> {
    return this.http.patch<IOrganization>(`${this.resourceUrl}/${this.getOrganizationIdentifier(organization)}`, organization, {
      observe: 'response',
    });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IOrganization>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IOrganization[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  getOrganizationIdentifier(organization: Pick<IOrganization, 'id'>): string {
    return organization.id;
  }
}
