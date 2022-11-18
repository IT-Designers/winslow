import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../environments/environment';


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

  getUsers(): Promise<UserInfo[]> {
    return this.client
      .get<UserInfo[]>(UserApiService.getUrl(''))
      .toPromise();
  }

  getUserNameAvailable(userName): Promise<object> {
    return this.client
      .get(UserApiService.getUrl(userName + '/available'))
      .toPromise();
  }
}

export class UserInfo {
  name: string;
  displayName: string;
  email: string;
}
