import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { Store } from '@ngxs/store';
import { Selectors } from '../../state/selectors';
import { DockerPortInfoModelPortInfo } from '../../model/docker/docker-port-info.model';

@Injectable({
  providedIn: 'root',
})
export class GhcrService {
  private resourceUrl: string;

  constructor(
    private http: HttpClient,
    private store: Store,
  ) {
    store.select(Selectors.getEndpointFor('/api/ghcr')).subscribe(url => {
      this.resourceUrl = url;
    });
  }

  getTags(owner: string, imageName: string): Observable<{ results: { name: string }[] }> {
    return this.http.get<any>(`${this.resourceUrl}/tags`, { params: { owner, image: imageName } }).pipe(
      map(response => ({
        results: (response.tags || []).map((tag: string) => ({ name: tag })),
      })),
      catchError(error => {
        console.error('Failed to fetch GHCR tags:', error);
        return throwError(() => new Error('Failed to fetch tags'));
      }),
    );
  }

  getImageConfig(
    owner: string,
    imageName: string,
    tag: string,
  ): Observable<{
    ports: DockerPortInfoModelPortInfo[];
    volumes: string[];
  }> {
    return this.http.get<any>(`${this.resourceUrl}/config`, { params: { owner, image: imageName, tag } }).pipe(
      map(config => ({
        ports: (config.ports || []).map((p: any) => ({
          containerPort: p.containerPort,
          protocol: p.protocol,
        })),
        volumes: config.volumes || [],
      })),
      catchError(error => {
        console.error('Failed to fetch GHCR image config:', error);
        return throwError(() => new Error('Failed to fetch image config'));
      }),
    );
  }
}
