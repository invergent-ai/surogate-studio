import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { S3Credentials, S3ValidationResponse } from '../../model/s3/s3.model';
import { Store } from '@ngxs/store';
import { Selectors } from '../../state/selectors';

@Injectable({providedIn: 'root'})
export class S3Service {
  private resourceUrl: string;

  constructor(
    private http: HttpClient,
    private store: Store
  ) {
    store.select(Selectors.getEndpointFor('api/s3')).subscribe((url) => {
      this.resourceUrl = url;
    });
  }

  validateCredentials(credentials: S3Credentials): Observable<S3ValidationResponse> {
    return this.http.post<S3ValidationResponse>(`${this.resourceUrl}/validate`, credentials);
  }
}
