import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../environments/environment';
import {lastValueFrom} from "rxjs";


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
    return lastValueFrom(
      this.client.get<UserInfo[]>(UserApiService.getUrl(''))
    );
  }

  createUser(newUser: UserInfo): Promise<UserInfo> {
    return lastValueFrom(
      this.client.post<UserInfo>(UserApiService.getUrl(''), newUser)
    );
  }

  updateUser(updatedUser: UserInfo): Promise<UserInfo> {
    return lastValueFrom(
      this.client.put<UserInfo>(UserApiService.getUrl(''), updatedUser)
    );
  }

  getUser(userName: string): Promise<UserInfo> {
    return lastValueFrom(
      this.client.get<UserInfo>(UserApiService.getUrl(userName))
    );
  }

  deleteUser(userName: string): Promise<void> {
    return lastValueFrom(
      this.client.delete<void>(UserApiService.getUrl(userName))
    );
  }

  /**
   * @param userName The name of the {@link UserInfo} to check for
   * @return Whether the name is available (not used yet).
   */
  getUserNameAvailable(userName: string): Promise<object> {
    return lastValueFrom(
      this.client.get(UserApiService.getUrl(userName + '/available'))
    );
  }

  setPassword(userName: string, newPassword: string): Promise<void> {
    return lastValueFrom(
      this.client.put<void>(UserApiService.getUrl(userName + '/password'), newPassword)
    );
  }

  removePassword(userName: string): Promise<void> {
    return lastValueFrom(
      this.client.delete<void>(UserApiService.getUrl(userName + '/password'))
    );
  }

  hasSuperPrivileges(userName: string): Promise<boolean> {
    return lastValueFrom(
      this.client.get<boolean>(UserApiService.getUrl(userName + '/super-privileges'))
    );
  }

  getSelfUserName(): Promise<string> {
    return lastValueFrom(
      this.client.get<string>(UserApiService.getUrl('self/name'))
    );
  }
}

// todo make type without password
export interface UserInfo {
  name: string;
  displayName: string;
  email: string;
  active: boolean;
  password: string;
}
