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

  getUsers(): Promise<UserInfo[]> {
    return this.client
      .get<UserInfo[]>(UserApiService.getUrl(''))
      .toPromise();
  }

  createUser(newUser: UserInfo): Promise<UserInfo> {
    return this.client
      .post<UserInfo>(UserApiService.getUrl(''), newUser)
      .toPromise();
  }

  updateUser(updatedUser: UserInfo): Promise<UserInfo> {
    return this.client
      .put<UserInfo>(UserApiService.getUrl(''), updatedUser)
      .toPromise();
  }

  getUser(userName): Promise<UserInfo> {
    return this.client
      .get<UserInfo>(UserApiService.getUrl(userName))
      .toPromise();
  }

  deleteUser(userName): Promise<void> {
    return this.client
      .delete<void>(UserApiService.getUrl(userName))
      .toPromise();
  }

  getUserNameAvailable(userName): Promise<object> {
    return this.client
      .get(UserApiService.getUrl(userName + '/available'))
      .toPromise();
  }

  setPassword(userName: string, newPassword: string): Promise<void> {
    return this.client
      .put<void>(UserApiService.getUrl(userName + '/password'), newPassword)
      .toPromise();
  }

  removePassword(userName: string): Promise<void> {
    return  this.client
      .delete<void>(UserApiService.getUrl(userName + '/password'))
      .toPromise();
  }

  hasSuperPrivileges(userName: string): Promise<boolean> {
    return this.client
      .get<boolean>(UserApiService.getUrl(userName + '/super-privileges'))
      .toPromise();
  }

  getSelfUserName(): Promise<string> {
    return this.client
      .get<string>(UserApiService.getUrl('self/name'))
      .toPromise();
  }
}

export class UserInfo {
  name: string;
  displayName: string;
  email: string;
  password: string;
}
