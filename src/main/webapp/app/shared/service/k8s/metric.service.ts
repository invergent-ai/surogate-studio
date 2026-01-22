import {Observable} from 'rxjs';
import {map} from 'rxjs/operators';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {IGpuMetric, IMetric, IModelWorkerMetric} from '../../model/k8s/metric.model';
import {Store} from '@ngxs/store';
import {Selectors} from '../../state/selectors';
import {SseEvent} from '../../model/k8s/event.model';
import {SseClient} from '../sse/sse-client.service';

@Injectable({
  providedIn: 'root'
})
export class MetricService {
  protected resourceUrl: string;

  constructor(
    protected store: Store,
    private http: HttpClient,
    private sseClient: SseClient
  ) {
    store.select(Selectors.getEndpointFor('/api/metrics')).subscribe((url) => {
      this.resourceUrl = url;
    });
  }

  connectToMetrics(applicationId: string, podName: string, containerId?: string): Observable<SseEvent<IMetric>> {
    let params = new HttpParams()
      .set('podName', podName)
      .set('containerId', containerId);

    return this.sseClient
      .stream(`${this.resourceUrl}/metrics/${applicationId}`,
        { keepAlive: true, reconnectionDelay: 1000, responseType: 'event' }, {params}, 'GET')
      .pipe(
        map((event: Event) => {
          if (event instanceof MessageEvent) {
            if (event.type === 'metrics') {
              try {
                return {type: event.type, data: JSON.parse(event.data) as IMetric} as SseEvent<IMetric>;
              } catch {
                throw new Error('Failed to parse IMetric message');
              }
            }
            return {type: event.type} as SseEvent<IMetric>;
          }
          if (event instanceof ErrorEvent) {
            throw event.error || new Error(event.message || 'SSE ErrorEvent');
          }
          throw new Error('Unsupported event type');
        })
      );
  }

  stopMetrics(applicationId: string, podName: string, containerId?: string): Observable<any> {
    let params = new HttpParams()
      .set('podName', podName)
      .set('containerId', containerId.toString());
    return this.http.delete<any>(`${this.resourceUrl}/metrics/${applicationId}`, {params});
  }

  connectToGpuMetrics(nodeId: string, gpuId: number): Observable<SseEvent<IGpuMetric>> {
    return this.sseClient
      .stream(`${this.resourceUrl}/gpu-metrics/${nodeId}/${gpuId}`,
        { keepAlive: true, reconnectionDelay: 1000, responseType: 'event' }, {}, 'GET')
      .pipe(
        map((event: Event) => {
          if (event instanceof MessageEvent) {
            if (event.type === 'metrics') {
              try {
                return {type: event.type, data: JSON.parse(event.data) as IGpuMetric} as SseEvent<IGpuMetric>;
              } catch {
                throw new Error('Failed to parse IGpuMetric message');
              }
            }
            return {type: event.type} as SseEvent<IGpuMetric>;
          }
          if (event instanceof ErrorEvent) {
            throw event.error || new Error(event.message || 'SSE ErrorEvent');
          }
          throw new Error('Unsupported event type');
        })
      );
  }

  stopGpuMetrics(nodeId: string, gpuId: number): Observable<any> {
    return this.http.delete<any>(`${this.resourceUrl}/gpu-metrics/${nodeId}/${gpuId}`);
  }

  connectToModelWorkerMetrics(applicationId: string, podName: string, containerName: string, modelName?: string): Observable<SseEvent<IModelWorkerMetric>> {
    let params = new HttpParams()
      .set('podName', podName)
      .set('containerName', containerName);

    if (modelName) {
      params = params.set('modelName', modelName);
    }

    return this.sseClient
      .stream(`${this.resourceUrl}/model-worker-metrics/${applicationId}`,
        { keepAlive: true, reconnectionDelay: 1000, responseType: 'event' }, {params}, 'GET')
      .pipe(
        map((event: Event) => {
          if (event instanceof MessageEvent) {
            if (event.type === 'metrics') {
              try {
                return {type: event.type, data: JSON.parse(event.data) as IModelWorkerMetric} as SseEvent<IModelWorkerMetric>;
              } catch {
                throw new Error('Failed to parse IModelWorkerMetric message');
              }
            }
            return {type: event.type} as SseEvent<IModelWorkerMetric>;
          }
          if (event instanceof ErrorEvent) {
            throw event.error || new Error(event.message || 'SSE ErrorEvent');
          }
          throw new Error('Unsupported event type');
        })
      );
  }

  stopModelWorkerMetrics(applicationId: string, podName: string, containerName: string, modelName?: string): Observable<any> {
    let params = new HttpParams()
      .set('podName', podName)
      .set('containerName', containerName);

    if (modelName) {
      params = params.set('modelName', modelName);
    }

    return this.http.delete<any>(`${this.resourceUrl}/model-worker-metrics/${applicationId}`, {params});
  }

  connectToModelRouterMetrics(applicationId: string, podName: string, containerName: string): Observable<SseEvent<any>> {
    let params = new HttpParams()
      .set('podName', podName)
      .set('containerName', containerName);

    return this.sseClient
      .stream(`${this.resourceUrl}/model-router-metrics/${applicationId}`,
        { keepAlive: true, reconnectionDelay: 1000, responseType: 'event' }, {params}, 'GET')
      .pipe(
        map((event: Event) => {
          if (event instanceof MessageEvent) {
            if (event.type === 'metrics') {
              try {
                return {type: event.type, data: JSON.parse(event.data)} as SseEvent<any>;
              } catch {
                throw new Error('Failed to parse Metrics message');
              }
            }
            return {type: event.type} as SseEvent<any>;
          }
          if (event instanceof ErrorEvent) {
            throw event.error || new Error(event.message || 'SSE ErrorEvent');
          }
          throw new Error('Unsupported event type');
        })
      );
  }

  stopModelRouterMetrics(applicationId: string, podName: string, containerName: string): Observable<any> {
    let params = new HttpParams()
      .set('podName', podName)
      .set('containerName', containerName);

    return this.http.delete<any>(`${this.resourceUrl}/model-router-metrics/${applicationId}`, {params});
  }

  connectToRayJobMetrics(jobIds: string[]): Observable<SseEvent<IMetric>> {
    return this.sseClient
      .stream(`${this.resourceUrl}/ray-job-metrics?ids=${jobIds.join(',')}`,
        { keepAlive: true, reconnectionDelay: 1000, responseType: 'event' }, {}, 'GET')
      .pipe(
        map((event: Event) => {
          if (event instanceof MessageEvent) {
            if (event.type === 'metrics') {
              try {
                return {type: event.type, data: JSON.parse(event.data) as IMetric} as SseEvent<IMetric>;
              } catch {
                throw new Error('Failed to parse IMetric message');
              }
            }
            return {type: event.type} as SseEvent<IMetric>;
          }
          if (event instanceof ErrorEvent) {
            throw event.error || new Error(event.message || 'SSE ErrorEvent');
          }
          throw new Error('Unsupported event type');
        })
      );
  }

  stopRayJobMetrics(jobIds: string[]): Observable<any> {
    return this.http.delete<any>(`${this.resourceUrl}/ray-job-metrics?ids=${jobIds.join(',')}`, {});
  }
}
