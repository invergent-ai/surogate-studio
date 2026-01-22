import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { Store } from '@ngxs/store';
import { Selectors } from '../state/selectors';
import { IEvaluationResult } from '../model/evaluation-result.model';

@Injectable({ providedIn: 'root' })
export class EvaluationResultService {
  private resourceUrl: string;
  private readonly EVAL_REPO = 'eval-results';
  private readonly EVAL_BRANCH = 'main';

  constructor(
    protected http: HttpClient,
    protected store: Store,
  ) {
    store.select(Selectors.getEndpointFor('api/lakefs')).subscribe(url => {
      this.resourceUrl = url;
    });
  }

  getResults(): Observable<string[]> {
    return this.http.get<string[]>(`${this.resourceUrl}/eval-results/${this.EVAL_REPO}/${this.EVAL_BRANCH}`);
  }

  getResultByJobId(jobId: string): Observable<IEvaluationResult | null> {
    const filename = `eval_${jobId}.json`;
    return this.getResult(filename).pipe(catchError(() => of(null)));
  }

  getReportByJobId(jobId: string): Observable<string | null> {
    const filename = `report_${jobId}.md`;
    return this.getReport(filename).pipe(catchError(() => of(null)));
  }

  getPdfReportByJobId(jobId: string): Observable<Blob | null> {
    const filename = `report_${jobId}.pdf`;
    return this.getPdfReport(filename).pipe(catchError(() => of(null)));
  }

  getResult(filename: string): Observable<IEvaluationResult> {
    return this.http
      .get(`${this.resourceUrl}/eval-results/${this.EVAL_REPO}/${this.EVAL_BRANCH}/${filename}`, {
        responseType: 'text',
      })
      .pipe(map(text => JSON.parse(text) as IEvaluationResult));
  }

  getReport(filename: string): Observable<string> {
    return this.http.get(`${this.resourceUrl}/eval-results/${this.EVAL_REPO}/${this.EVAL_BRANCH}/${filename}`, {
      responseType: 'text',
    });
  }

  getPdfReport(filename: string): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/eval-results/${this.EVAL_REPO}/${this.EVAL_BRANCH}/${filename}`, {
      responseType: 'blob',
    });
  }

  downloadPdf(jobId: string): void {
    this.getPdfReportByJobId(jobId).subscribe(blob => {
      if (blob) {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `report_${jobId}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
      }
    });
  }
}
