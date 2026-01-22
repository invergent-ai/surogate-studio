import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';


import { IVolume, NewVolume, ProjectVolumesDTO } from '../model/volume.model';
import { createRequestOption } from '../util/request-util';
import { Store } from '@ngxs/store';
import { Selectors } from '../state/selectors';

export type PartialUpdateVolume = Partial<IVolume> & Pick<IVolume, 'id'>;

export type EntityResponseType = HttpResponse<IVolume>;
export type EntityArrayResponseType = HttpResponse<IVolume[]>;

@Injectable({ providedIn: 'root' })
export class VolumeService {
  protected resourceUrl: string;

  constructor(
    protected http: HttpClient,
    protected store: Store,
  ) {
    store.select(Selectors.getEndpointFor('/api/volumes')).subscribe((url) => {
      this.resourceUrl = url;
    });
  }

  create(volume: IVolume | NewVolume): Observable<EntityResponseType> {
    return this.http.post<IVolume>(this.resourceUrl, volume, { observe: 'response' });
  }

  update(volume: IVolume): Observable<EntityResponseType> {
    return this.http.put<IVolume>(`${this.resourceUrl}/${this.getVolumeIdentifier(volume)}`, volume, { observe: 'response' });
  }

  partialUpdate(volume: PartialUpdateVolume): Observable<EntityResponseType> {
    return this.http.patch<IVolume>(`${this.resourceUrl}/${this.getVolumeIdentifier(volume)}`, volume, { observe: 'response' });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IVolume>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IVolume[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: string): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  getVolumeIdentifier(volume: Pick<IVolume, 'id'>): string {
    return volume.id;
  }
}
