import { Injectable } from '@angular/core';
import { Event, NavigationEnd, Router } from '@angular/router';
import { Observer, Subscription } from 'rxjs';
import { filter, map } from 'rxjs/operators';

import SockJS from 'sockjs-client';
import { RxStomp } from '@stomp/rx-stomp';
import { AccountService } from './account.service';
import { AuthServerProvider } from './auth-jwt.service';
import { Account } from '../model/account.model';
import { TrackerActivity } from '../model/tracker-activity.model';
import { Store } from '@ngxs/store';
import { Selectors } from '../state/selectors';

const DESTINATION_TRACKER = '/topic/tracker';
const DESTINATION_ACTIVITY = '/app/activity';

@Injectable({ providedIn: 'root' })
export class TrackerService {
  private rxStomp?: RxStomp;
  private routerSubscription: Subscription | null = null;
  private websocketTrackerUrl: string;

  constructor(
    private router: Router,
    private accountService: AccountService,
    private authServerProvider: AuthServerProvider,
    protected store: Store,
  ) {
    store.select(Selectors.getEndpointFor('websocket/tracker')).subscribe((url) => {
      this.websocketTrackerUrl = url;
    });
  }

  setup(): void {
    this.rxStomp = new RxStomp();
    this.rxStomp.configure({
      reconnectDelay: 1000
      // eslint-disable-next-line no-console
      // debug: (msg: string): void => console.log(new Date(), msg),
    });

    this.accountService.getAuthenticationState().subscribe({
      next: (account: Account | null) => {
        if (account) {
          this.connect();
        } else {
          this.disconnect();
        }
      },
    });

    this.rxStomp.connected$.subscribe(() => {
      this.sendActivity();

      this.routerSubscription = this.router.events
        .pipe(filter((event: Event) => event instanceof NavigationEnd))
        .subscribe(() => this.sendActivity());
    });
  }

  get stomp(): RxStomp {
    if (!this.rxStomp) {
      throw new Error('Stomp connection not initialized');
    }
    return this.rxStomp;
  }

  public subscribe(observer: Partial<Observer<TrackerActivity>>): Subscription {
    return (
      this.stomp
        .watch(DESTINATION_TRACKER)
        // eslint-disable-next-line @typescript-eslint/no-unsafe-return
        .pipe(map(imessage => JSON.parse(imessage.body)))
        .subscribe(observer)
    );
  }

  private connect(): void {
    this.updateCredentials();
    return this.stomp.activate();
  }

  private disconnect(): Promise<void> {
    if (this.routerSubscription) {
      this.routerSubscription.unsubscribe();
      this.routerSubscription = null;
    }
    return this.stomp.deactivate();
  }

  private buildUrl(): string {
    // building absolute path so that websocket doesn't fail when deploying with a context path
    const authToken = this.authServerProvider.getToken();
    if (authToken) {
      return `${this.websocketTrackerUrl}?access_token=${authToken}`;
    }
    return this.websocketTrackerUrl;
  }

  private updateCredentials(): void {
    this.stomp.configure({
      webSocketFactory: () => SockJS(this.buildUrl()),
    });
  }

  private sendActivity(): void {
    this.stomp.publish({
      destination: DESTINATION_ACTIVITY,
      body: JSON.stringify({ page: this.router.routerState.snapshot.url }),
    });
  }
}
