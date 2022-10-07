import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {environment} from '../../environments/environment';
import {SubscriptionHandler} from './subscription-handler';
import {Subscription} from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class GroupApiService {

  constructor(private client: HttpClient) { }

  private static getUrl(more?: string) {
    return `${environment.apiLocation}groups${more != null ? `/${more}` : ''}`;
  }

  getGroups(): Promise<object[]> {
    return this.client
      .get<object[]>(GroupApiService.getUrl(''))
      .toPromise();
  }
}
