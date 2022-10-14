import { Injectable } from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../environments/environment';

export interface User {
  name: string;
  role: string;
}

@Injectable({
  providedIn: 'root'
})
export class UserApiService {

  constructor(private client: HttpClient) {
  }

  private static getUrl(more?: string) {
    return `${environment.apiLocation}users${more != null ? `/${more}` : ''}`;
  }

  getSelfUserName(): Promise<string> {
    return this.client
      .get<string>(UserApiService.getUrl('self/name'))
      .toPromise();
  }
  getUsers(): Promise<User[]> {
    return this.client
      .get<User[]>(UserApiService.getUrl(''))
      .toPromise();
  }
}
