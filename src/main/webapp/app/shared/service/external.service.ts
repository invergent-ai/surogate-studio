import {Injectable} from '@angular/core';
import {HttpClient, HttpResponse} from '@angular/common/http';
import {Observable} from 'rxjs';

import {IApplication} from '../model/application.model';
import {Store} from '@ngxs/store';
import {Selectors} from '../state/selectors';
import {IExternalDeploy} from "../model/external-deploy.model";

@Injectable({providedIn: 'root'})
export class ExternalService {
  protected resourceUrl: string;

  constructor(
    protected http: HttpClient,
    protected store: Store,
  ) {
    store.select(Selectors.getEndpointFor('/api/external')).subscribe((url) => {
      this.resourceUrl = url;
    });
  }

  deploy(externalDeploy: IExternalDeploy): Observable<HttpResponse<IApplication>> {
    return this.http.post<any>(`${this.resourceUrl}/deploy`, externalDeploy, { observe: 'response' });
  }
}
