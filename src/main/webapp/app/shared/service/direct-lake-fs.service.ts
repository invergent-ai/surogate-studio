import {Injectable} from '@angular/core';
import {Observable} from 'rxjs';
import {HttpClient, HttpHeaders, HttpParams, HttpResponse} from '@angular/common/http';
import {map, tap} from 'rxjs/operators';
import {Store} from '@ngxs/store';
import {Selectors} from '../state/selectors';

const authenticationError = 'error authenticating request';

export class LakeFsError extends Error {
  status: number;

  constructor(message: string, status: number) {
    super(message);
    this.status = status;
    this.name = this.constructor.name;
  }
}

@Injectable({ providedIn: 'root' })
export class DirectLakeFsService {
  public lakeFsUri: string;
  private resourceUrl: string;
  public auth: string;
  public s3Auth: string;
  public s3Endpoint: string;

  constructor(
    protected http: HttpClient,
    protected store: Store,
  ) {
    store.select(Selectors.directLakeFsParams).subscribe(params => {
      if (params) {
        this.lakeFsUri = params.endpoint;
        this.auth = params.auth;
        this.s3Auth = params.s3Auth;
        this.s3Endpoint = params.s3Endpoint;
        this.resourceUrl = `${this.lakeFsUri}/api/v1`;
      }
    });
  }

  fetchObjectAsText(repoId: string, ref: string, path: string, presign = false): Observable<string> {
    let params = new HttpParams().set('path', path).set('presign', presign);

    return this.http
      .get(`${this.resourceUrl}/repositories/${encodeURIComponent(repoId)}/refs/${encodeURIComponent(ref)}/objects`, {
        headers: this.authHeaders(),
        params,
        observe: 'response',
        responseType: 'text',
      })
      .pipe(
        tap((response: any) => {
          if (response.status === 401) {
            const errorMessage = this.extractError(response);
            if (errorMessage === authenticationError) {
              throw new LakeFsError('Authentication Error', response.status);
            }
            throw new LakeFsError(errorMessage || 'Unauthorized', response.status);
          } else if (response.status !== 200 && response.status !== 206) {
            throw new Error(this.extractError(response));
          }
        }),
        map(response => response.body),
      );
  }

  download(repoId: string, branchId: string, path: string): Observable<HttpResponse<Blob>> {
    return this.http.get(
      `${this.resourceUrl}/repositories/${encodeURIComponent(repoId)}/refs/${encodeURIComponent(branchId)}/objects?path=${encodeURIComponent(path)}`,
      {
        headers: this.authHeaders(),
        responseType: 'blob',
        observe: 'response',
      },
    );
  }

  objectUploadUrl(repoId: string, branchId: string, path: string, file: File): string {
    const fpath = destinationPath(path, file);
    return `${this.resourceUrl}/repositories/${encodeURIComponent(repoId)}/branches/${encodeURIComponent(branchId)}/objects?path=${encodeURIComponent(fpath)}`;
  }

  private extractError(response: HttpResponse<any>) {
    let body = '';
    if (response.headers.get('Content-Type') === 'application/json') {
      try {
        const jsonBody = JSON.parse(response.body);
        body = jsonBody.message;
      } catch (e) {}
    } else {
      body = response.body;
    }
    return body;
  }

  public authHeaders(): HttpHeaders {
    return new HttpHeaders()
      .set('Accept', 'application/json')
      .set('X-Lakefs-Client', 'lakefs-webui/__buildVersion')
      .set('Authorization', 'Basic ' + this.auth);
  }
}

const destinationPath = (path: string, file: File) => {
  if (path?.trim() === '/') {
    return file.name;
  } else if (path) {
    if (path.startsWith('/')) {
      path = path.substring(1);
    }
    return path.endsWith('/') ? `${path}${file.name}` : `${path}/${file.name}`;
  } else {
    return file.name;
  }
};

