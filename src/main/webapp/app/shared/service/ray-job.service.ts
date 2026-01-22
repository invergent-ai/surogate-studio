import {Injectable} from '@angular/core';
import {HttpClient, HttpResponse} from '@angular/common/http';
import {Observable} from 'rxjs';
import {createRequestOption} from '../util/request-util';
import {Store} from '@ngxs/store';
import {Selectors} from '../state/selectors';
import {IRayJob} from "../model/ray-job.model";
import {IJob} from "../model/job.model";
import {map} from "rxjs/operators";

export type EntityResponseType = HttpResponse<IRayJob>;
export type EntityArrayResponseType = HttpResponse<IRayJob[]>;

@Injectable({providedIn: 'root'})
export class RayJobService {
  protected resourceUrl: string;

  constructor(
    protected http: HttpClient,
    protected store: Store,
  ) {
    store.select(Selectors.getEndpointFor('/api/jobs')).subscribe((url) => {
      this.resourceUrl = url;
    });
  }

  save(rayJob: IRayJob): Observable<IRayJob> {
    return this.http.post<IRayJob>(this.resourceUrl, rayJob);
  }

  deploy(rayJob: IRayJob): Observable<IRayJob> {
    return this.http.post<IRayJob>(`${this.resourceUrl}/deploy`, rayJob);
  }

  redeploy(rayJob: IRayJob): Observable<IRayJob> {
    return this.http.post<IRayJob>(`${this.resourceUrl}/redeploy`, rayJob);
  }

  cancel(id: string): Observable<any> {
    return this.http.delete(`${this.resourceUrl}/cancel/${id}`);
  }

  delete(id: string): Observable<any> {
    return this.http.delete(`${this.resourceUrl}/delete/${id}`);
  }

  find(id: string): Observable<IRayJob> {
    return this.http.get<IRayJob>(`${this.resourceUrl}/${id}`, {observe: 'response'})
      .pipe(map(res => res.body));
  }

  query(req?: any): Observable<IJob[]> {
    const options = createRequestOption(req);
    return this.http.get<IRayJob[]>(this.resourceUrl, {params: options})
      .pipe(map((jobs) => jobs.map(job => ({
        id: job.id,
        jobId: job.jobId,
        name: job.name,
        type: job.type,
        provisioningStatus: job.provisioningStatus,
        completedStatus: job.completedStatus,
        podName: job.podName,
        container: job.container,
        stage: job.stage,
        createdDate: job.createdDate,
        startTime: job.startTime,
        endTime: job.endTime
      } as IJob))));
  }

  search(query: string): Observable<IRayJob[]> {
    return this.http.get<IRayJob[]>(`${this.resourceUrl}/search`, {
      params: { query },
    });
  }
}
