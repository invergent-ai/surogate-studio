import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { IProtocol, NewProtocol } from '../model/protocol.model';
import { createRequestOption } from '../util/request-util';
import { Store } from '@ngxs/store';
import { Selectors } from '../state/selectors';


export type PartialUpdateProtocol = Partial<IProtocol> & Pick<IProtocol, 'id'>;

export type EntityResponseType = HttpResponse<IProtocol>;
export type EntityArrayResponseType = HttpResponse<IProtocol[]>;

@Injectable({ providedIn: 'root' })
export class ProtocolService {
  protected resourceUrl: string;

  constructor(
    protected http: HttpClient,
    protected store: Store,
  ) {
    store.select(Selectors.getEndpointFor('/api/protocols')).subscribe((url) => {
      this.resourceUrl = url;
    });
  }

  create(protocol: IProtocol | NewProtocol): Observable<EntityResponseType> {
    return this.http.post<IProtocol>(this.resourceUrl, protocol, { observe: 'response' });
  }

  update(protocol: IProtocol): Observable<EntityResponseType> {
    return this.http.put<IProtocol>(`${this.resourceUrl}/${this.getProtocolIdentifier(protocol)}`, protocol, { observe: 'response' });
  }

  partialUpdate(protocol: PartialUpdateProtocol): Observable<EntityResponseType> {
    return this.http.patch<IProtocol>(`${this.resourceUrl}/${this.getProtocolIdentifier(protocol)}`, protocol, { observe: 'response' });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IProtocol>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IProtocol[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  getProtocolIdentifier(protocol: Pick<IProtocol, 'id'>): string {
    return protocol.id;
  }
}
