import { Injectable } from '@angular/core';
import { HttpClient, HttpEventType, HttpParams, HttpResponse } from '@angular/common/http';
import { Store } from '@ngxs/store';
import { Selectors } from '../state/selectors';
import {
  ICreateLakeFsRepository,
  IDirectLakeFsServiceParams,
  ILakeFsBranchCreation,
  ILakeFsCommit,
  ILakeFsCommitCreation,
  ILakeFsDiff,
  ILakeFsImportJob,
  ILakeFsObjectStats,
  ILakeFsRef,
  ILakeFsRepository,
  ILakeFsTagCreation,
} from '../model/lakefs.model';
import { Observable } from 'rxjs';
import { User } from '../model/user.model';
import { tap } from 'rxjs/operators';

@Injectable({ providedIn: 'root' })
export class LakeFsService {
  public resourceUrl: string;
  s3Auth: string;
  s3Endpoint: string;

  constructor(
    protected http: HttpClient,
    protected store: Store,
  ) {
    store.select(Selectors.getEndpointFor('/api/lakefs')).subscribe(url => {
      this.resourceUrl = url;
    });
  }

  listRepositories(): Observable<ILakeFsRepository[]> {
    return this.http.get<ILakeFsRepository[]>(`${this.resourceUrl}/repos`);
  }

  listBranches(repoId: string): Observable<ILakeFsRef[]> {
    return this.http.get<ILakeFsRef[]>(`${this.resourceUrl}/branches/${encodeURIComponent(repoId)}`);
  }

  createBranch(repoId: string, branch: ILakeFsBranchCreation): Observable<any> {
    return this.http.post<any>(`${this.resourceUrl}/branches/${repoId}`, branch);
  }

  deleteBranch(repoId: string, branch: string): Observable<any> {
    return this.http.delete<any>(`${this.resourceUrl}/branches/${repoId}/${branch}`);
  }

  listTags(repoId: string): Observable<ILakeFsRef[]> {
    return this.http.get<ILakeFsRef[]>(`${this.resourceUrl}/tags/${encodeURIComponent(repoId)}`);
  }

  createTag(repoId: string, tag: ILakeFsTagCreation): Observable<any> {
    return this.http.post<any>(`${this.resourceUrl}/tags/${repoId}`, tag);
  }

  deleteTag(repoId: string, tag: string): Observable<any> {
    return this.http.delete<any>(`${this.resourceUrl}/tags/${repoId}/${tag}`);
  }

  listObjects(repoId: string, ref: string): Observable<ILakeFsObjectStats[]> {
    return this.http.get<ILakeFsObjectStats[]>(`${this.resourceUrl}/objects/${encodeURIComponent(repoId)}/${encodeURIComponent(ref)}`);
  }

  deleteObject(repoId: string, branch: string, path: string): Observable<any> {
    return this.http.delete(
      `${this.resourceUrl}/objects/${encodeURIComponent(repoId)}/${encodeURIComponent(branch)}?path=${encodeURIComponent(path)}`,
    );
  }

  createRepository(repo: ICreateLakeFsRepository): Observable<ILakeFsRepository> {
    return this.http.post<ILakeFsRepository>(`${this.resourceUrl}/repos`, repo);
  }

  createImportJob(job: ILakeFsImportJob): Observable<any> {
    return this.http.post(`${this.resourceUrl}/import`, job);
  }

  deleteRepository(repoId: string): Observable<any> {
    return this.http.delete(`${this.resourceUrl}/repos/${encodeURIComponent(repoId)}`);
  }

  commit(repoId: string, branchId: string, commit: ILakeFsCommitCreation): Observable<any> {
    return this.http.post(`${this.resourceUrl}/commit/${encodeURIComponent(repoId)}/${encodeURIComponent(branchId)}`, commit);
  }

  diff(repoId: string, leftRefId: string, rightRefId: string): Observable<ILakeFsDiff[]> {
    return this.http.get<ILakeFsDiff[]>(
      `${this.resourceUrl}/diff/${encodeURIComponent(repoId)}/${encodeURIComponent(leftRefId)}/${encodeURIComponent(rightRefId)}`,
    );
  }

  getCommit(repoId: string, commitId: string): Observable<ILakeFsCommit> {
    return this.http.get<ILakeFsCommit>(`${this.resourceUrl}/commit/${encodeURIComponent(repoId)}/${encodeURIComponent(commitId)}`);
  }

  getCommits(repoId: string, refId: string): Observable<ILakeFsCommit[]> {
    return this.http.get<ILakeFsCommit[]>(`${this.resourceUrl}/commits/${encodeURIComponent(repoId)}/${encodeURIComponent(refId)}`);
  }

  getStat(repoId: string, refId: string, path: string): Observable<ILakeFsObjectStats> {
    return this.http.get<ILakeFsObjectStats>(
      `${this.resourceUrl}/stat/${encodeURIComponent(repoId)}/${encodeURIComponent(refId)}?path=${encodeURIComponent(path)}`,
    );
  }

  getDirectServiceParams(): Observable<IDirectLakeFsServiceParams> {
    return this.http.get<IDirectLakeFsServiceParams>(`${this.resourceUrl}/config`);
  }

  addUserToRepoGroup(repoId: string, username: string) {
    return this.http.post(`${this.resourceUrl}/repos/${encodeURIComponent(repoId)}/users`, { username });
  }

  getGroupMembers(groupId: string): Observable<User[]> {
    return this.http.get<User[]>(`${this.resourceUrl}/group/${encodeURIComponent(groupId)}/members`);
  }

  deleteGroupMember(groupId: string, username: string): Observable<any> {
    return this.http.delete(`${this.resourceUrl}/group/${encodeURIComponent(groupId)}/members/${encodeURIComponent(username)}`);
  }

  fetchObjectAsText(repoId: string, ref: string, path: string): Observable<string> {
    return this.http.get(`${this.resourceUrl}/objects/${encodeURIComponent(repoId)}/${encodeURIComponent(ref)}/content`, {
      params: new HttpParams().set('path', path),
      responseType: 'text',
    });
  }
  download(repoId: string, ref: string, path: string): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.resourceUrl}/objects/${encodeURIComponent(repoId)}/${encodeURIComponent(ref)}/download`, {
      params: new HttpParams().set('path', path),
      responseType: 'blob',
      observe: 'response',
    });
  }
  objectUploadUrl(repoId: string, branchId: string, path: string, file: File): string {
    const fpath = this.destinationPath(path, file);
    return `${this.resourceUrl}/objects/${encodeURIComponent(repoId)}/${encodeURIComponent(branchId)}/upload?path=${encodeURIComponent(fpath)}`;
  }

  destinationPath = (path: string, file: File) => {
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

  loadS3Config(): Observable<any> {
    return this.http.get<any>(`${this.resourceUrl}/config`).pipe(
      tap(config => {
        this.s3Auth = config.s3Auth;
        this.s3Endpoint = config.s3Endpoint;
      }),
    );
  }

  fetchObjectAsTextWithProgress(repoId: string, ref: string, path: string): Observable<{ progress: number; data: string | null }> {
    return new Observable(observer => {
      this.http
        .get(`${this.resourceUrl}/objects/${encodeURIComponent(repoId)}/${encodeURIComponent(ref)}/content`, {
          params: new HttpParams().set('path', path),
          responseType: 'text',
          reportProgress: true,
          observe: 'events',
        })
        .subscribe({
          next: (event: any) => {
            switch (event.type) {
              case HttpEventType.DownloadProgress:
                const progress = event.total ? Math.round((event.loaded * 100) / event.total) : -1;
                observer.next({ progress, data: null });
                break;
              case HttpEventType.Response:
                observer.next({ progress: 100, data: event.body });
                observer.complete();
                break;
            }
          },
          error: err => observer.error(err),
        });
    });
  }
}
