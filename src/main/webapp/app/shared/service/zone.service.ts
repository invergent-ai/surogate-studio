import {Injectable} from '@angular/core';
import {HttpClient, HttpResponse} from '@angular/common/http';
import {Observable} from 'rxjs';

import {IZone, NewZone} from '../model/zone.model';
import {createRequestOption} from '../util/request-util';
import { Store } from '@ngxs/store';
import { Selectors } from '../state/selectors';

export type PartialUpdateZone = Partial<IZone> & Pick<IZone, 'id'>;

export type EntityResponseType = HttpResponse<IZone>;
export type EntityArrayResponseType = HttpResponse<IZone[]>;

@Injectable({ providedIn: 'root' })
export class ZoneService {
  protected resourceUrl: string;

  constructor(
    protected http: HttpClient,
    protected store: Store,
  ) {
    store.select(Selectors.getEndpointFor('/api/zones')).subscribe((url) => {
      this.resourceUrl = url;
    });
  }

  create(zone: IZone | NewZone): Observable<EntityResponseType> {
    return this.http.post<IZone>(this.resourceUrl, zone, { observe: 'response' });
  }

  update(zone: IZone): Observable<EntityResponseType> {
    return this.http.put<IZone>(`${this.resourceUrl}/${this.getZoneIdentifier(zone)}`, zone, { observe: 'response' });
  }

  partialUpdate(zone: PartialUpdateZone): Observable<EntityResponseType> {
    return this.http.patch<IZone>(`${this.resourceUrl}/${this.getZoneIdentifier(zone)}`, zone, { observe: 'response' });
  }

  find(id: string): Observable<EntityResponseType> {
    return this.http.get<IZone>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IZone[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: string): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  getZoneIdentifier(zone: Pick<IZone, 'id'>): string {
    return zone.id;
  }
}
