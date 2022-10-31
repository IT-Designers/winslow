import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {environment} from '../../environments/environment';
import {Group} from './project-api.service';


@Injectable({
  providedIn: 'root'
})
export class GroupApiService {

  constructor(private client: HttpClient) { }

  private static getUrl(more?: string) {
    return `${environment.apiLocation}groups${more != null ? `/${more}` : ''}`;
  }

  getGroups(): Promise<Group[]> {
    return this.client
      .get<Group[]>(GroupApiService.getUrl(''))
      .toPromise();
  }
  getGroup(groupName): Promise<object> {
    return this.client
      .get<object>(GroupApiService.getUrl(groupName))
      .toPromise();
  }
  createGroup(group): Promise<object> {
    return this.client
      .post<object>(GroupApiService.getUrl(''), group)
      .toPromise();
  }
  deleteGroup(name): Promise<void> {
    return this.client
      .delete<void>(GroupApiService.getUrl(name))
      .toPromise();
  }
  getMemberships(name): Promise<object> {
    return this.client
      .get<object[]>(GroupApiService.getUrl(name + '/members'))
      .toPromise();
  }
  addOrUpdateMembership(groupName, user): Promise<object> {
    return this.client
      .post(GroupApiService.getUrl(groupName + '/members'), user)
      .toPromise();
  }
  deleteGroupMembership(groupName, userName): Promise<object> {
    return this.client
      .delete(GroupApiService.getUrl(groupName + '/members/' + userName))
      .toPromise();
  }
  getGroupNameAvailable(groupName): Promise<object> {
    return this.client
      .get(GroupApiService.getUrl(groupName + '/available'))
      .toPromise();
  }
}
