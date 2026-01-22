import {Observable} from 'rxjs';
import {HttpClient} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {TrackerService} from '../tracker.service';
import {Store} from '@ngxs/store';
import {Selectors} from '../../state/selectors';

@Injectable({
  providedIn: 'root'
})
export class TestChatService {
  protected resourceUrl: string;

  constructor(
    private trackerService: TrackerService,
    protected store: Store,
    private http: HttpClient
  ) {
    store.select(Selectors.getEndpointFor('api/chat')).subscribe((url) => {
      this.resourceUrl = url;
    });
  }

  startChat(jobId: string): Observable<any> {
    return this.http.get<any>(`${this.resourceUrl}/job/${jobId}`, {});
  }

  stopChat(jobId: string): Observable<any> {
    return this.http.delete<any>(`${this.resourceUrl}/job/${jobId}`, {});
  }

  connectToJobChat(jobId: string): Observable<any> {
    const topic = `/topic/message/${jobId}`;
    return this.trackerService.stomp.watch(topic);
  }

  sendChatMessage(jobId: string, message: string): void {
    this.trackerService.stomp.publish({
      destination: '/job/message',
      body: JSON.stringify({ jobId, message })
    });
  }
}
