import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {environment} from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class RoleApiService {

  constructor(private client: HttpClient) { }

  private static getUrl(more?: string) {
    return `${environment.apiLocation}roles${more != null ? `/${more}` : ''}`;
  }

  getRoles(): Promise<string[]> {
    return this.client
      .get<string[]>(RoleApiService.getUrl(''))
      .toPromise();
  }
}


