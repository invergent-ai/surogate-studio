import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { map } from 'rxjs/operators';

import dayjs from 'dayjs/esm';
import { INodeReservation } from '../model/node-reservation.model';
import { Store } from '@ngxs/store';
import { Selectors } from '../state/selectors';


type RestOf<T extends INodeReservation> = Omit<T, 'created' | 'updated'> & {
  created?: string | null;
  updated?: string | null;
};

export type RestNodeReservation = RestOf<INodeReservation>;

export type EntityResponseType = HttpResponse<INodeReservation>;
export type EntityArrayResponseType = HttpResponse<INodeReservation[]>;

@Injectable({ providedIn: 'root' })
export class NodeReservationService {
  private resourceUrl: string;

  constructor(
    protected http: HttpClient,
    protected store: Store,
  ) {
    store.select(Selectors.getEndpointFor('/api/node-reservation')).subscribe((url) => {
      this.resourceUrl = url;
    });
  }

  getOrCreate(): Observable<EntityResponseType> {
    return this.http
      .get<RestNodeReservation>(`${this.resourceUrl}`, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  query(): Observable<INodeReservation[]> {
    return this.http
      .get<RestNodeReservation[]>(`${this.resourceUrl}/query`, { observe: 'response' })
      .pipe(
        map(res => this.convertArrayResponseFromServer(res)),
        map(res => res.body)
      );
  }

  protected convertDateFromServer(restNodeReservation: RestNodeReservation): INodeReservation {
    return {
      ...restNodeReservation,
      created: restNodeReservation.created ? dayjs(restNodeReservation.created) : undefined,
      updated: restNodeReservation.updated ? dayjs(restNodeReservation.updated) : undefined,
      expireTime: restNodeReservation.expireTime ? dayjs(restNodeReservation.expireTime) : undefined,
    };
  }

  protected convertResponseFromServer(res: HttpResponse<RestNodeReservation>): HttpResponse<INodeReservation> {
    return res.clone({
      body: res.body ? this.convertDateFromServer(res.body) : null,
    });
  }

  protected convertArrayResponseFromServer(res: HttpResponse<RestNodeReservation[]>): HttpResponse<INodeReservation[]> {
    return res.clone({
      body: res.body ? res.body.map(this.convertDateFromServer) : null,
    });
  }
}
