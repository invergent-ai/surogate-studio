import {IContainer, NewContainer} from "../model/container.model";
import {HttpClient, HttpResponse} from "@angular/common/http";
import {Injectable} from "@angular/core";
import {Observable} from "rxjs";
import {createRequestOption} from "../util/request-util";
import { Store } from '@ngxs/store';
import { Selectors } from '../state/selectors';

export type PartialUpdateContainer = Partial<IContainer> & Pick<IContainer, 'id'>;

export type EntityResponseType = HttpResponse<IContainer>;
export type EntityArrayResponseType = HttpResponse<IContainer[]>;

@Injectable({ providedIn: 'root' })
export class ContainerService {
  private resourceUrl: string;

  constructor(
    protected http: HttpClient,
    protected store: Store,
  ) {
    store.select(Selectors.getEndpointFor('/api/containers')).subscribe((url) => {
      this.resourceUrl = url;
    });
  }

  create(container: IContainer | NewContainer): Observable<EntityResponseType> {
    return this.http.post<IContainer>(this.resourceUrl, container, { observe: 'response' });
  }

  update(container: IContainer): Observable<EntityResponseType> {
    return this.http.put<IContainer>(`${this.resourceUrl}/${this.getContainerIdentifier(container)}`, container, { observe: 'response' });
  }

  partialUpdate(container: PartialUpdateContainer): Observable<EntityResponseType> {
    return this.http.patch<IContainer>(`${this.resourceUrl}/${this.getContainerIdentifier(container)}`, container, { observe: 'response' });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IContainer>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IContainer[]>(this.resourceUrl, {
      params: options,
      observe: 'response'
    });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  getContainerIdentifier(container: Pick<IContainer, 'id'>): string {
    return container.id;
  }
}
