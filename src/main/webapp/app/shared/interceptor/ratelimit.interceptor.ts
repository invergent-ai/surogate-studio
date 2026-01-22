import {Inject, Injectable, PLATFORM_ID} from '@angular/core';
import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Store } from '@ngxs/store';
import { Selectors } from '../state/selectors';
import {isPlatformBrowser} from "@angular/common";

// declare var FingerprintJS: any;

@Injectable()
export class RatelimitInterceptor implements HttpInterceptor {
  SECURE_END = '55c8c7bcda518d2d12872dedb9c809d7c781e4022ec43e12e592f94ec527248adeb04265e251ebd626bbb722cdc0e0b92532c700eb61cfec0de846b14df70dee953523d72548f0187795e945b69938e86bc46ba0bfe27a567e4bda93b6032512f931221555a2b04ddc56561b0c9c84739a7c214bb67413d3828bd31a5b7a4cffa096ce83ddab8196f042829d51be72224225ee41915bafb48434f8f089f6178a';
  isBrowser: boolean;

  constructor(private store: Store, @Inject(PLATFORM_ID) platformId: Object) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const serverApiUrl = this.store.selectSnapshot(Selectors.getEndpointFor(''));
    if (!request.url || (request.url.startsWith('http') && !(serverApiUrl && request.url.startsWith(serverApiUrl)))) {
      return next.handle(request);
    }

    request = request.clone({
      setHeaders: {
        // @ts-ignore
        'X-Api-Key': (this.isBrowser ? FingerprintJS.getSync().visitorId : 'prerender') + this.SECURE_END
      },
    });

    return next.handle(request);
  }
}
