import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { IProject, IProjectResource, NewProject } from '../model/project.model';
import { createRequestOption } from '../util/request-util';
import { Store } from '@ngxs/store';
import { Selectors } from '../state/selectors';

export type PartialUpdateProject = Partial<IProject> & Pick<IProject, 'id'>;

export type EntityResponseType = HttpResponse<IProject>;
export type EntityArrayResponseType = HttpResponse<IProject[]>;

@Injectable({ providedIn: 'root' })
export class ProjectService {
  protected resourceUrl: string;

  constructor(
    protected http: HttpClient,
    protected store: Store,
  ) {
    store.select(Selectors.getEndpointFor('/api/projects')).subscribe((url) => {
      this.resourceUrl = url;
    });
  }

  create(project: IProject | NewProject): Observable<EntityResponseType> {
    return this.http.post<IProject>(this.resourceUrl, project, { observe: 'response' });
  }

  update(project: IProject): Observable<EntityResponseType> {
    return this.http.put<IProject>(`${this.resourceUrl}/${this.getProjectIdentifier(project)}`, project, { observe: 'response' });
  }

  partialUpdate(project: PartialUpdateProject): Observable<EntityResponseType> {
    return this.http.patch<IProject>(`${this.resourceUrl}/${this.getProjectIdentifier(project)}`, project, { observe: 'response' });
  }

  find(id: string): Observable<EntityResponseType> {
    return this.http.get<IProject>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IProject[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: string): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  search(query: string): Observable<IProject[]> {
    return this.http.get<IProject[]>(`${this.resourceUrl}/search`, {
      params: { query },
    });
  }

  getBasicInfo(): Observable<IProject[]> {
    return this.http.get<IProject[]>(`${this.resourceUrl}/basic`);
  }

  getProjectIdentifier(project: Pick<IProject, 'id'>): string {
    return project.id;
  }

  resources(id: string): Observable<IProjectResource[]> {
    return this.http.get<IProjectResource[]>(`${this.resourceUrl}/${id}/resources`);
  }
}
