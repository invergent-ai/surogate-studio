import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { IPort, NewPort } from '../model/port.model';
import { createRequestOption } from '../util/request-util';
import { Store } from '@ngxs/store';
import { Selectors } from '../state/selectors';


export type PartialUpdatePort = Partial<IPort> & Pick<IPort, 'id'>;

export type EntityResponseType = HttpResponse<IPort>;
export type EntityArrayResponseType = HttpResponse<IPort[]>;

@Injectable({ providedIn: 'root' })
export class PortService {
  private resourceUrl: string;

  constructor(
    protected http: HttpClient,
    protected store: Store,
  ) {
    store.select(Selectors.getEndpointFor('api/ports')).subscribe((url) => {
      this.resourceUrl = url;
    });
  }

  create(port: IPort | NewPort): Observable<EntityResponseType> {
    return this.http.post<IPort>(this.resourceUrl, port, { observe: 'response' });
  }

  update(port: IPort): Observable<EntityResponseType> {
    return this.http.put<IPort>(`${this.resourceUrl}/${this.getPortIdentifier(port)}`, port, { observe: 'response' });
  }

  partialUpdate(port: PartialUpdatePort): Observable<EntityResponseType> {
    return this.http.patch<IPort>(`${this.resourceUrl}/${this.getPortIdentifier(port)}`, port, { observe: 'response' });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IPort>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IPort[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  getPortIdentifier(port: Pick<IPort, 'id'>): string {
    return port.id;
  }
}
