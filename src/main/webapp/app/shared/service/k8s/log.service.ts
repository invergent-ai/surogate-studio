import { Injectable } from '@angular/core';
import { Store } from '@ngxs/store';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Selectors } from '../../state/selectors';
import { ILog, ILogCriteria } from '../../model/k8s/log.model';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { SseEvent } from '../../model/k8s/event.model';
import { SseClient } from '../sse/sse-client.service';

@Injectable({
  providedIn: 'root'
})
export class LogService {
  protected resourceUrl: string;

  constructor(
    protected store: Store,
    private http: HttpClient,
    private sseClient: SseClient
  ) {
    store.select(Selectors.getEndpointFor('/api/logs')).subscribe((url) => {
      this.resourceUrl = url;
    });
  }

  connectToLogStream(resourceId: string, resourceType: 'application' | 'taskRun' | 'rayJob', criteria: ILogCriteria): Observable<SseEvent<ILog[]>> {
    let params = new HttpParams()
      .set('limit', criteria.limit.toString())
      .set('podName', criteria.podName)
      .set('resourceId', resourceId);

    if (criteria.containerId) {
      params = params.set('containerId', criteria.containerId.toString());
    }
    if (criteria.sinceSeconds) {
      params = params.set('sinceSeconds', criteria.sinceSeconds.toString());
    }
    if (criteria.startDate) {
      params = params.set('startDate', criteria.startDate.toISOString());
    }
    if (criteria.endDate) {
      params = params.set('endDate', criteria.endDate.toISOString());
    }
    if (criteria.searchTerm) {
      params = params.set('searchTerm', criteria.searchTerm);
    }

    return this.sseClient
      .stream(`${this.resourceUrl}/logs/${resourceType}/${resourceId}`,
        { keepAlive: false, reconnectionDelay: 1000, responseType: 'event' }, {params}, 'GET')
      .pipe(
        map((event: Event) => this.handleSseEvent<ILog[]>('logs', event)),
      );
  }

  stopLogs(resourceId: string, resourceType: 'application' | 'taskRun' | 'rayJob', podName: string, containerId: string): Observable<any> {
    let params = new HttpParams()
      .set('podName', podName)
      .set('containerId', containerId);
    return this.http.delete<any>(`${this.resourceUrl}/logs/${resourceType}/${resourceId}`, {params});
  }

  private handleSseEvent<T>(type: string, event: Event): SseEvent<T> {
    if (event instanceof MessageEvent) {
      if (event.type === type) {
        try {
          return {type: event.type, data: JSON.parse(event.data) as T} as SseEvent<T>;
        } catch {
          throw new Error('Failed to parse log message');
        }
      }
      return {type: event.type} as SseEvent<T>;
    }
    if (event instanceof ErrorEvent) {
      throw event.error || new Error(event.message || 'SSE ErrorEvent');
    }
    throw new Error('Unsupported event type');
  }
}
