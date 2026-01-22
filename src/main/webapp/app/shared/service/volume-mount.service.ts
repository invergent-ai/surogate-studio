import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';


import { IVolumeMount, NewVolumeMount } from '../model/volume-mount.model';
import { createRequestOption } from '../util/request-util';
import { Store } from '@ngxs/store';
import { Selectors } from '../state/selectors';

export type PartialUpdateVolumeMount = Partial<IVolumeMount> & Pick<IVolumeMount, 'id'>;

export type EntityResponseType = HttpResponse<IVolumeMount>;
export type EntityArrayResponseType = HttpResponse<IVolumeMount[]>;

@Injectable({ providedIn: 'root' })
export class VolumeMountService {
  protected resourceUrl: string;

  constructor(
    protected http: HttpClient,
    protected store: Store,
  ) {
    store.select(Selectors.getEndpointFor('/api/volume-mounts')).subscribe((url) => {
      this.resourceUrl = url;
    });
  }

  create(volumeMount: IVolumeMount | NewVolumeMount): Observable<EntityResponseType> {
    return this.http.post<IVolumeMount>(this.resourceUrl, volumeMount, { observe: 'response' });
  }

  update(volumeMount: IVolumeMount): Observable<EntityResponseType> {
    return this.http.put<IVolumeMount>(`${this.resourceUrl}/${this.getVolumeMountIdentifier(volumeMount)}`, volumeMount, {
      observe: 'response',
    });
  }

  partialUpdate(volumeMount: PartialUpdateVolumeMount): Observable<EntityResponseType> {
    return this.http.patch<IVolumeMount>(`${this.resourceUrl}/${this.getVolumeMountIdentifier(volumeMount)}`, volumeMount, {
      observe: 'response',
    });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IVolumeMount>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IVolumeMount[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  getVolumeMountIdentifier(volumeMount: Pick<IVolumeMount, 'id'>): string {
    return volumeMount.id;
  }
}
