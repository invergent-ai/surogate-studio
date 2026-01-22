import {Observable} from 'rxjs';
import { HttpClient, HttpParams } from '@angular/common/http';
import {Injectable} from '@angular/core';
import {Store} from '@ngxs/store';
import {Selectors} from '../../state/selectors';

@Injectable({
  providedIn: 'root'
})
export class ControlService {
  protected resourceUrl: string;

  constructor(
    protected store: Store,
    private http: HttpClient
  ) {
    store.select(Selectors.getEndpointFor('/api/control')).subscribe((url) => {
      this.resourceUrl = url;
    });
  }

  startApplication(applicationId: string, component: string = null): Observable<boolean> {
    let params = new HttpParams();
    if (component) {
      params = params.set('component', component);
    }
    return this.http.post<boolean>(`${this.resourceUrl}/start/${applicationId}`, null, {params});
  }

  stopApplication(applicationId: string, component: string = null): Observable<boolean> {
    let params = new HttpParams();
    if (component) {
      params = params.set('component', component);
    }
    return this.http.post<boolean>(`${this.resourceUrl}/stop/${applicationId}`, null, {params});
  }

  restartApplication(applicationId: string, component: string = null): Observable<boolean> {
    let params = new HttpParams();
    if (component) {
      params = params.set('component', component);
    }
    return this.http.post<boolean>(`${this.resourceUrl}/restart/${applicationId}`, null, {params});
  }

  scaleApplication(applicationId: string, replicas: number): Observable<boolean> {
    return this.http.post<boolean>(`${this.resourceUrl}/scale/${applicationId}/${replicas}`, null);
  }

  startDatabase(databaseId: string): Observable<boolean> {
    return this.http.post<boolean>(`${this.resourceUrl}/startdb/${databaseId}`, null);
  }

  stopDatabase(databaseId: string): Observable<boolean> {
    return this.http.post<boolean>(`${this.resourceUrl}/stopdb/${databaseId}`, null);
  }
}
