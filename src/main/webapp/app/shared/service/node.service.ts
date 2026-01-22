import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { map } from 'rxjs/operators';

import dayjs from 'dayjs/esm';

import { DATE_FORMAT } from 'app/config/constant/input.constants';
import { INode, NewNode } from '../model/node.model';
import { createRequestOption } from '../util/request-util';
import { Store } from '@ngxs/store';
import { Selectors } from '../state/selectors';

export type PartialUpdateNode = Partial<INode> & Pick<INode, 'id'>;

type RestOf<T extends INode | NewNode> = Omit<T, 'lastStartTime'> & {
  lastStartTime?: string | null;
};

export type RestNode = RestOf<INode>;
export type EntityResponseType = HttpResponse<INode>;
export type EntityArrayResponseType = HttpResponse<INode[]>;

@Injectable({ providedIn: 'root' })
export class NodeService {
  private resourceUrl: string;

  constructor(
    protected http: HttpClient,
    protected store: Store,
  ) {
    store.select(Selectors.getEndpointFor('/api/nodes')).subscribe((url) => {
      this.resourceUrl = url;
    });
  }

  create(node: INode | NewNode): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(node);
    return this.http.post<RestNode>(this.resourceUrl, copy, { observe: 'response' }).pipe(map(res => this.convertResponseFromServer(res)));
  }

  update(node: INode): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(node);
    return this.http
      .put<RestNode>(`${this.resourceUrl}/${this.getNodeIdentifier(node)}`, copy, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  partialUpdate(node: PartialUpdateNode): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(node);
    return this.http
      .patch<RestNode>(`${this.resourceUrl}/${this.getNodeIdentifier(node)}`, copy, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http
      .get<RestNode>(`${this.resourceUrl}/${id}`, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http
      .get<RestNode[]>(this.resourceUrl, { params: options, observe: 'response' })
      .pipe(map(res => this.convertResponseArrayFromServer(res)));
  }

  delete(id: string): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  suggestedPrice(node: INode): Observable<number> {
    return this.http.post<number>(`${this.resourceUrl}/suggested`, node, { observe: 'response' })
      .pipe(map(res => res.body));
  }

  getNodeIdentifier(node: Pick<INode, 'id'>): string {
    return node.id;
  }

  protected convertDateFromClient<T extends INode | NewNode | PartialUpdateNode>(node: T): RestOf<T> {
    return {
      ...node,
      lastStartTime: node.lastStartTime?.format(DATE_FORMAT) ?? null,
    };
  }

  protected convertDateFromServer(restNode: RestNode): INode {
    return {
      ...restNode,
      lastStartTime: restNode.lastStartTime ? dayjs(restNode.lastStartTime) : undefined,
    };
  }

  protected convertResponseFromServer(res: HttpResponse<RestNode>): HttpResponse<INode> {
    return res.clone({
      body: res.body ? this.convertDateFromServer(res.body) : null,
    });
  }

  protected convertResponseArrayFromServer(res: HttpResponse<RestNode[]>): HttpResponse<INode[]> {
    return res.clone({
      body: res.body ? res.body.map(item => this.convertDateFromServer(item)) : null,
    });
  }
}
