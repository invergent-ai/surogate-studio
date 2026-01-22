import {inject, Injectable} from '@angular/core';
import {lastValueFrom, Observable} from 'rxjs';
import {Account} from '../model/account.model';
import {AccountService} from './account.service';
import {HttpClient, HttpParams, HttpResponse} from "@angular/common/http";
import {User} from "../model/user.model";
import {Store} from "@ngxs/store";
import {Selectors} from "../state/selectors";

@Injectable({providedIn: 'root'})
export class UserService {

  httpClient = inject(HttpClient);

  protected resourceUrl: string;

  constructor(
    private accountService: AccountService,
    protected store: Store,
  ) {
    store.select(Selectors.getEndpointFor('/api/admin')).subscribe((url) => {
      this.resourceUrl = url;
    });
  }

  async getUserDetails(force = false, navigateToPreviousUrl = true): Promise<Account> {
    try {
      return await lastValueFrom(this.accountService.identity(force, navigateToPreviousUrl));
    } catch (e) {
      console.log(e);
    }

    return null;
  }

  getAllUsers(page: number, size: number, sort: string = 'createdDate,desc'): Observable<HttpResponse<User[]>> {
    let params = new HttpParams().set('page', page).set('size', size).set('sort', sort)
    return this.httpClient.get<User[]>(`${this.resourceUrl}/users`, {params: params, observe: 'response'});
  }

  createUser(user: User): Observable<any> {
    return this.httpClient.post(`${this.resourceUrl}/users`, user);
  }

  updateUser(user: User): Observable<any> {
    return this.httpClient.put(`${this.resourceUrl}/users`, user);
  }

  getUser(login: string): Observable<User> {
    return this.httpClient.get<User>(`${this.resourceUrl}/users/${login}`);
  }

  deleteUser(login: string): Observable<void> {
    return this.httpClient.delete<void>(`${this.resourceUrl}/users/${login}`);
  }
}
