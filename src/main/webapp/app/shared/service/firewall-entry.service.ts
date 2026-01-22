import {Injectable} from '@angular/core';
import {HttpClient, HttpResponse} from '@angular/common/http';
import {Observable} from 'rxjs';
import {createRequestOption} from '../util/request-util';
import {Store} from '@ngxs/store';
import {Selectors} from '../state/selectors';
import {IFirewallEntry, NewFirewallEntry} from "../model/firewall-entry.model";

export type PartialUpdateFirewallEntry = Partial<IFirewallEntry> & Pick<IFirewallEntry, 'id'>;

export type EntityResponseType = HttpResponse<IFirewallEntry>;
export type EntityArrayResponseType = HttpResponse<IFirewallEntry[]>;

@Injectable({ providedIn: 'root' })
export class FirewallEntryService {
  private resourceUrl: string;

  constructor(
    protected http: HttpClient,
    protected store: Store,
  ) {
    store.select(Selectors.getEndpointFor('/api/firewall-entries')).subscribe((url) => {
      this.resourceUrl = url;
    });
  }

  create(firewallEntry: IFirewallEntry | NewFirewallEntry): Observable<EntityResponseType> {
    return this.http.post<IFirewallEntry>(this.resourceUrl, firewallEntry, { observe: 'response' });
  }

  update(firewallEntry: IFirewallEntry): Observable<EntityResponseType> {
    return this.http.put<IFirewallEntry>(
      `${this.resourceUrl}/${this.getFirewallEntryIdentifier(firewallEntry)}`,
      firewallEntry,
      { observe: 'response' },
    );
  }

  partialUpdate(firewallEntry: PartialUpdateFirewallEntry): Observable<EntityResponseType> {
    return this.http.patch<IFirewallEntry>(
      `${this.resourceUrl}/${this.getFirewallEntryIdentifier(firewallEntry)}`,
      firewallEntry,
      { observe: 'response' },
    );
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IFirewallEntry>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IFirewallEntry[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  getFirewallEntryIdentifier(firewallEntry: Pick<IFirewallEntry, 'id'>): string {
    return firewallEntry.id;
  }
}
