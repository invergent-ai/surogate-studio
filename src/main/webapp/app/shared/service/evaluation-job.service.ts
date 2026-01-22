import {inject, Injectable} from '@angular/core';
import {IEvaluationJob} from "../model/evaluation-job.model";
import {Observable} from "rxjs";
import {HttpClient} from "@angular/common/http";
import {Selectors} from "../state/selectors";
import {Store} from "@ngxs/store";

@Injectable({
  providedIn: 'root'
})
export class EvaluationJobService {
  http = inject(HttpClient);
  resourceUrl = '';
  constructor(private store: Store) {
    this.store.select(Selectors.getEndpointFor('api/evaluation-jobs')).subscribe(url => {
      this.resourceUrl = url;
    });
  }

  create(job: any): Observable<IEvaluationJob> {
    return this.http.post<IEvaluationJob>(this.resourceUrl, job);
  }

  update(job: IEvaluationJob): Observable<IEvaluationJob> {
    return this.http.put<IEvaluationJob>(`${this.resourceUrl}/${job.id}`, job);
  }

  find(id: number): Observable<IEvaluationJob> {
    return this.http.get<IEvaluationJob>(`${this.resourceUrl}/${id}`);
  }

  query(): Observable<IEvaluationJob[]> {
    return this.http.get<IEvaluationJob[]>(this.resourceUrl);
  }

  delete(id: number): Observable<{}> {
    return this.http.delete(`${this.resourceUrl}/${id}`);
  }
}
