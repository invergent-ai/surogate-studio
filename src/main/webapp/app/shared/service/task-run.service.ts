import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Store } from '@ngxs/store';
import { Selectors } from '../state/selectors';
import { Observable } from 'rxjs';
import { ITaskRun } from '../model/tasks.model';
import { createRequestOption } from '../util/request-util';
import { TaskRunType } from '../model/enum/task-run-type.model';
import {map} from "rxjs/operators";
import {IJob} from "../model/job.model";

@Injectable({ providedIn: 'root' })
export class TaskRunService {
  protected resourceUrl: string;

  constructor(
    protected http: HttpClient,
    protected store: Store
  ) {
    store.select(Selectors.getEndpointFor('/api/tasks')).subscribe((url) => {
      this.resourceUrl = url;
    });
  }

  save(task: ITaskRun): Observable<ITaskRun> {
    return this.http.post<ITaskRun>(this.resourceUrl, task);
  }

  cancel(id: string): Observable<any> {
    return this.http.delete(`${this.resourceUrl}/cancel/${id}`);
  }

  delete(id: string): Observable<any> {
    return this.http.delete(`${this.resourceUrl}/delete/${id}`);
  }

  submit(task: ITaskRun): Observable<ITaskRun> {
    return this.http.post<ITaskRun>(`${this.resourceUrl}/submit`, task);
  }

  query(req?: any): Observable<IJob[]> {
    const options = createRequestOption(req);
    return this.http.get<ITaskRun[]>(this.resourceUrl, { params: options })
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

  find(id: string): Observable<ITaskRun>{
    return this.http.get<ITaskRun>(`${this.resourceUrl}/${id}`, {observe : 'response'}).pipe((map(res=> res.body)));
  }

  findMyTasks(type: TaskRunType): Observable<ITaskRun[]> {
    let params = new HttpParams()
      .set("type", type);
    return this.http.get<ITaskRun[]>(`${this.resourceUrl}/my`, { params });
  }
}
