import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ICluster, NewCluster } from '../model/cluster.model';
import { createRequestOption } from '../util/request-util';
import { Store } from '@ngxs/store';
import { Selectors } from '../state/selectors';

export type PartialUpdateCluster = Partial<ICluster> & Pick<ICluster, 'id'>;

export type EntityResponseType = HttpResponse<ICluster>;
export type EntityArrayResponseType = HttpResponse<ICluster[]>;

@Injectable({ providedIn: 'root' })
export class ClusterService {
  private resourceUrl: string;

  constructor(
    protected http: HttpClient,
    protected store: Store,
  ) {
    store.select(Selectors.getEndpointFor('/api/clusters')).subscribe((url) => {
      this.resourceUrl = url;
    });
  }

  create(cluster: ICluster | NewCluster): Observable<EntityResponseType> {
    return this.http.post<ICluster>(this.resourceUrl, cluster, { observe: 'response' });
  }

  update(cluster: ICluster): Observable<EntityResponseType> {
    return this.http.put<ICluster>(`${this.resourceUrl}/${this.getClusterIdentifier(cluster)}`, cluster, { observe: 'response' });
  }

  partialUpdate(cluster: PartialUpdateCluster): Observable<EntityResponseType> {
    return this.http.patch<ICluster>(`${this.resourceUrl}/${this.getClusterIdentifier(cluster)}`, cluster, { observe: 'response' });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<ICluster>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<ICluster[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: string): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  getClusterIdentifier(cluster: Pick<ICluster, 'id'>): string {
    return cluster.id;
  }
}
