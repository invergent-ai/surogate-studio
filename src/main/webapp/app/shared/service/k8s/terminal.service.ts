import { Observable } from 'rxjs';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { TrackerService } from '../tracker.service';
import { Store } from '@ngxs/store';
import { Selectors } from '../../state/selectors';

@Injectable({
  providedIn: 'root'
})
export class TerminalService {
  protected resourceUrl: string;

  constructor(
    private trackerService: TrackerService,
    protected store: Store,
    private http: HttpClient
  ) {
    store.select(Selectors.getEndpointFor('api/terminal')).subscribe((url) => {
      this.resourceUrl = url;
    });
  }

  connectToAppTerminal(applicationId: string, podName: string, containerId?: string): Observable<any> {
    const topic = `/topic/terminal/${applicationId}/${podName}/${containerId}`;
    return this.trackerService.stomp.watch(topic);
  }

  startAppTerminal(applicationId: string, podName: string, containerId?: string): Observable<any> {
    let params = new HttpParams()
      .set("podName", podName)
      .set('containerId', containerId.toString());
    return this.http.get<any>(`${this.resourceUrl}/app/${applicationId}`, {params});
  }

  stopAppTerminal(applicationId: string, podName: string, containerId?: string): Observable<any> {
    let params = new HttpParams()
      .set("podName", podName)
      .set('containerId', containerId.toString());
    return this.http.delete<any>(`${this.resourceUrl}/app/${applicationId}`, {params});
  }

  sendAppTerminalCommand(applicationId: string, podName: string, containerId: string, message: string): void {
    this.trackerService.stomp.publish({
      destination: '/app/terminal',
      body: JSON.stringify({ applicationId, podName, containerId, vmId: null, message }),
    });
  }

  connectToVmTerminal(vmId: string): Observable<any> {
    const topic = `/topic/terminal/${vmId}`;
    return this.trackerService.stomp.watch(topic);
  }

  startVmTerminal(vmId: string): Observable<any> {
    return this.http.get<any>(`${this.resourceUrl}/vm/${vmId}`);
  }

  stopVmTerminal(vmId: string): Observable<any> {
    return this.http.delete<any>(`${this.resourceUrl}/vm/${vmId}`);
  }

  sendVmTerminalCommand(vmId: string, message: string): void {
    this.trackerService.stomp.publish({
      destination: '/app/terminal',
      body: JSON.stringify({ applicationId: null, containerId: null, vmId: vmId, message }),
    });
  }
}
