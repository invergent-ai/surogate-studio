import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { map } from 'rxjs/operators';

import dayjs from 'dayjs/esm';
import { INotification, NewNotification } from '../model/notification.model';
import { createRequestOption } from '../util/request-util';
import { isPresent } from '../util/operators';
import { DATE_FORMAT } from '../../config/constant/input.constants';
import { Store } from '@ngxs/store';
import { Selectors } from '../state/selectors';


export type PartialUpdateNotification = Partial<INotification> & Pick<INotification, 'id'>;

type RestOf<T extends INotification | NewNotification> = Omit<T, 'createdTime'> & {
  createdTime?: string | null;
};

export type RestNotification = RestOf<INotification>;

export type NewRestNotification = RestOf<NewNotification>;

export type PartialUpdateRestNotification = RestOf<PartialUpdateNotification>;

export type EntityResponseType = HttpResponse<INotification>;
export type EntityArrayResponseType = HttpResponse<INotification[]>;

@Injectable({providedIn: 'root'})
export class NotificationService {
  private resourceUrl: string;

  constructor(
    protected http: HttpClient,
    protected store: Store,
  ) {
    store.select(Selectors.getEndpointFor('/api/notifications')).subscribe((url) => {
      this.resourceUrl = url;
    });
  }

  create(notification: INotification | NewNotification): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(notification);
    return this.http
      .post<RestNotification>(this.resourceUrl, copy, {observe: 'response'})
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  update(notification: INotification): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(notification);
    return this.http
      .put<RestNotification>(`${this.resourceUrl}/${this.getNotificationIdentifier(notification)}`, copy, {observe: 'response'})
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  partialUpdate(notification: PartialUpdateNotification): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(notification);
    return this.http
      .patch<RestNotification>(`${this.resourceUrl}/${this.getNotificationIdentifier(notification)}`, copy, {observe: 'response'})
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http
      .get<RestNotification>(`${this.resourceUrl}/${id}`, {observe: 'response'})
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http
      .get<RestNotification[]>(this.resourceUrl, {params: options, observe: 'response'})
      .pipe(map(res => this.convertResponseArrayFromServer(res)));
  }

  getUserNotifications(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http
      .get<RestNotification[]>(`${this.resourceUrl}/user`, {params: options, observe: 'response'})
      .pipe(map(res => this.convertResponseArrayFromServer(res)));
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, {observe: 'response'});
  }

  getNotificationIdentifier(notification: Pick<INotification, 'id'>): string {
    return notification.id;
  }


  markAsRead(id: string): Observable<HttpResponse<void>> {
    return this.http.put<void>(
      `${this.resourceUrl}/${id}/mark-read`,
      {},
      {observe: 'response'}
    );
  }

  markAllAsRead(): Observable<HttpResponse<void>> {
    return this.http.put<void>(
      `${this.resourceUrl}/mark-all-read`,
      {},
      {observe: 'response'}
    );
  }

  compareNotification(o1: Pick<INotification, 'id'> | null, o2: Pick<INotification, 'id'> | null): boolean {
    return o1 && o2 ? this.getNotificationIdentifier(o1) === this.getNotificationIdentifier(o2) : o1 === o2;
  }

  addNotificationToCollectionIfMissing<Type extends Pick<INotification, 'id'>>(
    notificationCollection: Type[],
    ...notificationsToCheck: (Type | null | undefined)[]
  ): Type[] {
    const notifications: Type[] = notificationsToCheck.filter(isPresent);
    if (notifications.length > 0) {
      const notificationCollectionIdentifiers = notificationCollection.map(
        notificationItem => this.getNotificationIdentifier(notificationItem)!,
      );
      const notificationsToAdd = notifications.filter(notificationItem => {
        const notificationIdentifier = this.getNotificationIdentifier(notificationItem);
        if (notificationCollectionIdentifiers.includes(notificationIdentifier)) {
          return false;
        }
        notificationCollectionIdentifiers.push(notificationIdentifier);
        return true;
      });
      return [...notificationsToAdd, ...notificationCollection];
    }
    return notificationCollection;
  }

  protected convertDateFromClient<T extends INotification | NewNotification | PartialUpdateNotification>(notification: T): RestOf<T> {
    return {
      ...notification,
      createdTime: notification.createdTime?.format(DATE_FORMAT) ?? null,
    };
  }

  protected convertDateFromServer(restNotification: RestNotification): INotification {
    return {
      ...restNotification,
      createdTime: restNotification.createdTime ? dayjs(restNotification.createdTime) : undefined,
    };
  }

  protected convertResponseFromServer(res: HttpResponse<RestNotification>): HttpResponse<INotification> {
    return res.clone({
      body: res.body ? this.convertDateFromServer(res.body) : null,
    });
  }

  protected convertResponseArrayFromServer(res: HttpResponse<RestNotification[]>): HttpResponse<INotification[]> {
    return res.clone({
      body: res.body ? res.body.map(item => this.convertDateFromServer(item)) : null,
    });
  }
}
