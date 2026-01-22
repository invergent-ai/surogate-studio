import {Injectable} from '@angular/core';
import {HttpClient, HttpResponse} from '@angular/common/http';
import {Observable} from 'rxjs';
import {Store} from '@ngxs/store';
import {Selectors} from '../state/selectors';
import {StringWrapper} from "../model/string-wrapper.model";

export type EntityResponseType = HttpResponse<StringWrapper>;

@Injectable({ providedIn: 'root' })
export class InfoService {
  protected resourceUrl: string;

  constructor(
    protected http: HttpClient,
    protected store: Store,
  ) {
    store.select(Selectors.getEndpointFor('api/info')).subscribe((url) => {
      this.resourceUrl = url;
    });
  }

  url(): Observable<EntityResponseType> {
    return this.http.get<StringWrapper>(`${this.resourceUrl}/url`, { observe: 'response' });
  }
}
