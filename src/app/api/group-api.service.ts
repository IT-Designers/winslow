import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../environments/environment';
import {lastValueFrom} from "rxjs";

import {Link} from "./winslow-api";


@Injectable({
  providedIn: 'root'
})
export class GroupApiService {

  constructor(private client: HttpClient) {
  }

  private static getUrl(more?: string) {
    return `${environment.apiLocation}groups${more != null ? `/${more}` : ''}`;
  }

  getGroups(): Promise<GroupInfo[]> {
    return lastValueFrom(
      this.client.get<GroupInfo[]>(GroupApiService.getUrl(''))
    );
  }

  getGroup(groupName: string): Promise<GroupInfo> {
    return lastValueFrom(
      this.client.get<GroupInfo>(GroupApiService.getUrl(groupName))
    );
  }

  createGroup(group: GroupInfo): Promise<GroupInfo> {
    return lastValueFrom(
      this.client.post<GroupInfo>(GroupApiService.getUrl(''), group)
    );
  }

  deleteGroup(name: string): Promise<void> {
    return lastValueFrom(
      this.client.delete<void>(GroupApiService.getUrl(name))
    );
  }

  getMemberships(name: string): Promise<Link[]> {
    return lastValueFrom(
      this.client.get<Link[]>(GroupApiService.getUrl(name + '/members'))
    );
  }

  addOrUpdateMembership(groupName: string, link: Link): Promise<Link[]> {
    return lastValueFrom(
      this.client.post<Link[]>(GroupApiService.getUrl(groupName + '/members'), link)
    );
  }

  deleteGroupMembership(groupName: string, userName: string): Promise<object> {
    return lastValueFrom(
      this.client.delete(GroupApiService.getUrl(groupName + '/members/' + userName))
    );
  }

  getGroupNameAvailable(groupName: string): Promise<object> {
    return lastValueFrom(
      this.client.get(GroupApiService.getUrl(groupName + '/available'))
    );
  }
}

export interface GroupInfo {
  name: string;
  members: Link[];
}
