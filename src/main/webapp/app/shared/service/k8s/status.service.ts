import {Observable} from 'rxjs';
import {map} from 'rxjs/operators';
import {HttpClient} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {AppStatusWithResources} from '../../model/k8s/app-status.model';
import {DbStatus} from '../../model/k8s/db-status.model';
import {Selectors} from '../../state/selectors';
import {Store} from '@ngxs/store';
import {IApplication} from '../../model/application.model';
import {IDatabase} from '../../model/database.model';
import {SseEvent} from '../../model/k8s/event.model';
import {ModelStatusWithResources} from '../../model/k8s/model-status.model';
import {SseClient} from '../sse/sse-client.service';
import {IJobRunStatus} from "../../model/job.model";

@Injectable({
  providedIn: 'root'
})
export class StatusService {
  protected resourceUrl: string;

  constructor(
    protected store: Store,
    private http: HttpClient,
    private sseClient: SseClient
  ) {
    store.select(Selectors.getEndpointFor('api/status')).subscribe((url) => {
      this.resourceUrl = url;
    });
  }

  connectToAppStatus(applicationId: string): Observable<SseEvent<AppStatusWithResources>> {
    return this.sseClient
      .stream(`${this.resourceUrl}/app/${applicationId}`,
        { keepAlive: true, reconnectionDelay: 1000, responseType: 'event' }, {}, 'GET')
      .pipe(
        map((event: Event) => this.handleSseEvent<AppStatusWithResources>('status', event)),
      );
  }

  stopAppStatus(application: IApplication): Observable<any> {
    return this.http.delete<any>(`${this.resourceUrl}/app/${application.id}`, {});
  }

  startDbStatus(databaseId: string): Observable<any> {
    return this.http.post<any>(`${this.resourceUrl}/db/${databaseId}`, {});
  }

  connectToDbStatus(databaseId: string): Observable<DbStatus> {
    return this.sseClient
      .stream(`${this.resourceUrl}/db/${databaseId}`,
        { keepAlive: true, reconnectionDelay: 1000, responseType: 'event' }, {}, 'GET')
      .pipe(
        map((event: Event) => this.handleDbSseEvent(event)),
      );
  }

  stopDbStatus(database: IDatabase): Observable<any> {
    return this.http.delete<any>(`${this.resourceUrl}/db/${database.id}`, {});
  }

  connectToModelStatus(applicationId: string): Observable<SseEvent<ModelStatusWithResources>> {
    return this.sseClient
      .stream(`${this.resourceUrl}/model/${applicationId}`,
        { keepAlive: true, reconnectionDelay: 1000, responseType: 'event' }, {}, 'GET')
      .pipe(
        map((event: Event) => this.handleSseEvent<ModelStatusWithResources>('modelstatus', event)),
      );
  }

  stopModelStatus(application: IApplication): Observable<any> {
    return this.http.delete<any>(`${this.resourceUrl}/model/${application.id}`);
  }

  connectToTaskRunStatus(taskIds: string[]): Observable<SseEvent<IJobRunStatus[]>> {
    return this.sseClient
      .stream(`${this.resourceUrl}/task-run?ids=${taskIds.join(',')}`,
        { keepAlive: true, reconnectionDelay: 1000, responseType: 'event' }, {}, 'GET')
      .pipe(
        map((event: Event) => this.handleSseEvent<IJobRunStatus[]>('taskstatus', event)),
      );
  }

  connectToRayJobStatus(jobIds: string[]): Observable<SseEvent<IJobRunStatus[]>> {
    return this.sseClient
      .stream(`${this.resourceUrl}/ray-job?ids=${jobIds.join(',')}`,
        { keepAlive: true, reconnectionDelay: 1000, responseType: 'event' }, {}, 'GET')
      .pipe(
        map((event: Event) => this.handleSseEvent<IJobRunStatus[]>('rayjobstatus', event)),
      );
  }

  stopJobRunStatus(taskIds: string[]): Observable<any> {
    return this.http.delete<any>(`${this.resourceUrl}/task-run?ids=${taskIds.join(',')}`);
  }

  stopRayJobStatus(jobIds: string[]): Observable<any> {
    return this.http.delete<any>(`${this.resourceUrl}/ray-job?ids=${jobIds.join(',')}`);
  }

  private handleDbSseEvent(event: Event): DbStatus {
    if (event instanceof MessageEvent) {
      if (event.type === 'message' || event.type === 'status') {
        try {
          const data = JSON.parse(event.data) as DbStatus;
          return data;
        } catch (e) {
          return { stage: null, details: [], message: 'Parse error' } as DbStatus;
        }
      }
      return { stage: null, details: [], message: event.type, type: event.type } as DbStatus;
    }
    if (event instanceof ErrorEvent) {
      return { stage: null, details: [], message: 'Reconnecting...' } as DbStatus;
    }
    return { stage: null, details: [], message: 'Unknown event' } as DbStatus;
  }

  private handleSseEvent<T>(type: string, event: Event): SseEvent<T> {
    if (event instanceof MessageEvent) {
      if (event.type === type) {
        try {
          return {type: event.type, data: JSON.parse(event.data) as T} as SseEvent<T>;
        } catch {
          throw new Error('Failed to parse Status message');
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
