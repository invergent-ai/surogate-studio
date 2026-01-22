import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { TrackerService } from '../tracker.service';
import { NodeStats } from '../../model/k8s/node-stats.model';
import { Store } from '@ngxs/store';
import { Selectors } from '../../state/selectors';

@Injectable({
  providedIn: 'root'
})
export class NodeStatusService {
  protected resourceUrl: string;

  constructor(
    private trackerService: TrackerService,
    protected store: Store,
    private http: HttpClient
  ) {
    store.select(Selectors.getEndpointFor('api/status/node')).subscribe((url) => {
      this.resourceUrl = url;
    });
  }

  connectToStatus(nodeId: string): Observable<NodeStats> {
    const topic = `/topic/stats/${nodeId}`;
    return this.trackerService.stomp
      .watch(topic)
      .pipe(
        map(message => {
          return JSON.parse(message.body) as NodeStats;
        })
      );
  }

  startStatus(nodeId: string): Observable<any> {
    return this.http.get<any>(`${this.resourceUrl}/status/${nodeId}`, {});
  }

  stopStatus(nodeId: string): Observable<any> {
    return this.http.delete<any>(`${this.resourceUrl}/status/${nodeId}`, {});
  }

  statusSnapshot(clusterCid: string): Observable<NodeStats[]> {
    return this.http.get<NodeStats[]>(`${this.resourceUrl}/snapshot/${clusterCid}`);
  }

  statusSnapshotForNode(nodeId: string): Observable<NodeStats> {
    return this.http.get<NodeStats>(`${this.resourceUrl}/node-snapshot/${nodeId}`);
  }
}
