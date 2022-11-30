import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../environments/environment';
import {map} from 'rxjs/operators';
import {ResourceLimitation} from './winslow-api';
import {ResourceLimitationExt} from './project-api.service';

@Injectable({
  providedIn: 'root'
})
export class SettingsApiService {

  constructor(private client: HttpClient) {
  }

  private static getUrl(more?: string) {
    return `${environment.apiLocation}settings${more != null ? `/${more}` : ''}`;
  }

  getGlobalEnvironmentVariables(): Promise<Map<string, string>> {
    return this.client.get<Map<string, string>>(SettingsApiService.getUrl('global-env'))
      .pipe(map(response => new Map(Object.entries(response))))
      .toPromise();
  }

  setGlobalEnvironmentVariables(env: any): Promise<void> {
    return this.client.post<any>(SettingsApiService.getUrl('global-env'), env)
      .pipe(map(v => {
        return;
      }))
      .toPromise();
  }

  getUserResourceLimitation(): Promise<ResourceLimitationExt> {
    return this.client.get<ResourceLimitationExt>(SettingsApiService.getUrl('user-res-limit'))
      .pipe(map(response => new ResourceLimitationExt(response)))
      .toPromise();
  }

  setUserResourceLimitation(limit: ResourceLimitationExt): Promise<ResourceLimitationExt> {
    return this.client.post<ResourceLimitationExt>(SettingsApiService.getUrl('user-res-limit'), limit)
      .pipe(map(response => new ResourceLimitationExt(response)))
      .toPromise();
  }
}


