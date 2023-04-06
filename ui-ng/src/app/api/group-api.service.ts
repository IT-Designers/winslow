import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../environments/environment';


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
    return this.client
      .get<GroupInfo[]>(GroupApiService.getUrl(''))
      .toPromise();
  }

  getGroup(groupName): Promise<GroupInfo> {
    return this.client
      .get<GroupInfo>(GroupApiService.getUrl(groupName))
      .toPromise();
  }

  createGroup(group): Promise<GroupInfo> {
    return this.client
      .post<GroupInfo>(GroupApiService.getUrl(''), group)
      .toPromise();
  }

  deleteGroup(name): Promise<void> {
    return this.client
      .delete<void>(GroupApiService.getUrl(name))
      .toPromise();
  }

  getMemberships(name): Promise<MemberInfo[]> {
    return this.client
      .get<MemberInfo[]>(GroupApiService.getUrl(name + '/members'))
      .toPromise();
  }

  addOrUpdateMembership(groupName, user): Promise<MemberInfo> {
    return this.client
      .post<MemberInfo>(GroupApiService.getUrl(groupName + '/members'), user)
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

export class MemberInfo {
  name: string;
  role: string;
}

export class GroupInfo {
  name: string;
  members: MemberInfo[];
}
