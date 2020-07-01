import { Injectable } from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../environments/environment';
import {StateInfo} from './project-api.service';

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
}
