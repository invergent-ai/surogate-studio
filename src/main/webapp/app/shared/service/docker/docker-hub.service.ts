// docker-hub.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { DockerHubImage, DockerHubSearchResponse, DockerHubTag } from '../../model/docker/docker-hub.model';
import { DockerPortInfoModelPortInfo } from '../../model/docker/docker-port-info.model';
import { Store } from '@ngxs/store';
import { Selectors } from '../../state/selectors';

@Injectable({
  providedIn: 'root'
})
export class DockerHubService {
  protected resourceUrl: string;

  constructor(private http: HttpClient,
              protected store: Store,
  ) {
    store.select(Selectors.getEndpointFor('/api/docker-hub')).subscribe((url) => {
      this.resourceUrl = url;
    });
  }

  searchImages(query: string): Observable<{ results: DockerHubImage[] }> {
    return this.http.get<DockerHubSearchResponse>(`${this.resourceUrl}/search`, {
      params: {q: query}
    }).pipe(
      map(response => ({
        results: (response?.results || []).map(item => ({
          name: item.name,
          description: item.short_description || '',
          stars: item.star_count || 0,
          official: item.source === 'official',
          automated: false,
          logo_url: item.logo_url?.small || null
        }))
      })),
      catchError(error => {
        console.error('Search failed:', error);
        return throwError(() => new Error('Search failed'));
      })
    );
  }

  getImageTags(imageName: string): Observable<{ results: DockerHubTag[] }> {
    // No need to modify the URL - let the browser handle the encoding
    return this.http.get<any>(`${this.resourceUrl}/tags/${imageName}`).pipe(
      map(response => ({
        results: (response.results || []).map((tag: any) => ({
          name: tag.name,
          last_updated: tag.last_updated
        }))
      })),
      catchError(error => {
        console.error('Failed to fetch tags:', error);
        return throwError(() => new Error('Failed to fetch tags'));
      })
    );
  }

  getImagePortsAndVolumes(namespace: string | null, imageName: string, tag: string): Observable<{
    ports: DockerPortInfoModelPortInfo[],
    volumes: string[]
  }> {
    const path = namespace ?
      `${namespace}/${imageName}` :
      imageName;

    return this.http.get<any>(`${this.resourceUrl}/config/${path}/${tag}`)
      .pipe(
        map(config => ({
          ports: this.parseExposedPorts(config.ExposedPorts),
          volumes: this.parseVolumes(config.Volumes)
        }))
      );
  }

  private parseVolumes(volumes: any): string[] {
    if (!volumes) return [];

    return Object.entries(volumes).map(([key]) => {
      // Handle the case where the key is wrapped in "map[]"
      if (key.startsWith('map[') && key.endsWith(']')) {
        return key.substring(4, key.length - 1);
      }
      return key;
    });
  }

  private parseExposedPorts(exposedPorts: any): DockerPortInfoModelPortInfo[] {
    const ports: DockerPortInfoModelPortInfo[] = [];
    if (!exposedPorts) return ports;

    Object.keys(exposedPorts).forEach(key => {
      // Handle nested map objects
      const matches = key.match(/(?:map\[)?(\d+)\/(\w+)(?::\{\})?(?:\])?/);
      if (matches && matches[1] && matches[2]) {
        ports.push({
          containerPort: parseInt(matches[1], 10),
          protocol: matches[2].toUpperCase()
        });
      }
    });

    return ports;
  }

  validateRegistryCredentials(credentials: {
    registryUrl: string | null;
    registryUser: string | null;
    registryPassword: string | null;
    applicationId: string | null;
    imageName: string | null;
  }): Observable<any> {
    if (!credentials.registryUrl || !credentials.registryUser || !credentials.registryPassword) {
      return of(true);
    }

    return this.http.post(`${this.resourceUrl}/validate/registry`, {
      url: credentials.registryUrl,
      username: credentials.registryUser,
      password: credentials.registryPassword,
      applicationId: credentials.applicationId
    });
  }

  searchCustomRegistry(registry: any): Observable<{ results: DockerHubImage[] }> {
    const params: any = {
      url: registry.registryUrl,
      username: registry.registryUser === 'null' ? null : registry.registryUser,
      password: registry.registryPassword === 'null' ? null : registry.registryPassword
    };

    Object.keys(params).forEach(key => {
      if (params[key] === null) delete params[key];
    });

    return this.http.get<DockerHubSearchResponse>(`${this.resourceUrl}/registry/search`, {params}).pipe(
      map(response => ({
        results: response.results.map(item => ({
          name: item.name,
          description: item.short_description || '',
          stars: 0,
          official: false,
          automated: false,
          logo_url: null,
          tags: item.tags
        }))
      })),
      catchError(error => {
        console.error('Registry search failed:', error);
        return throwError(() => new Error('Registry search failed'));
      })
    );
  }

  getRegistryImageTags(registry: any, imageName: string): Observable<{ results: DockerHubTag[] }> {
    return this.http.get<any>(`${this.resourceUrl}/registry/tags`, {
      params: {
        url: registry.registryUrl,
        username: registry.registryUser,
        password: registry.registryPassword,
        image: imageName
      }
    }).pipe(
      map(response => ({
        results: response.results.map((tag: any) => ({
          name: tag.name,
          images: tag.images,
          last_updated: tag.last_updated,
          last_updater_username: tag.last_updater_username,
          full_size: tag.full_size,
          digest: tag.digest
        }))
      })),
      catchError(error => {
        console.error('Failed to fetch registry tags:', error);
        return throwError(() => new Error('Failed to fetch registry tags'));
      })
    );
  }

  getRegistryImagePortsAndVolumes(params: {
    registryUrl: string;
    registryUser: string;
    registryPassword: string;
    namespace: string;
    imageName: string;
    tag: string;
  }): Observable<{
    ports: DockerPortInfoModelPortInfo[];
    volumes: string[];
  }> {
    return this.http.post<{
      ports: DockerPortInfoModelPortInfo[];
      volumes: string[];
    }>(`${this.resourceUrl}/docker-registry/ports-and-volumes`, params);
  }
}
