import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../environments/environment';
import {map} from 'rxjs/operators';
import {IResourceLimitation} from './winslow-api';
import {IResourceLimitationExt} from './project-api.service';

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

  getUserResourceLimitation(): Promise<IResourceLimitationExt> {
    return this.client.get<IResourceLimitationExt>(SettingsApiService.getUrl('user-res-limit'))
      .pipe(map(response => new IResourceLimitationExt(response)))
      .toPromise();
  }

  setUserResourceLimitation(limit: IResourceLimitationExt): Promise<IResourceLimitationExt> {
    return this.client.post<IResourceLimitationExt>(SettingsApiService.getUrl('user-res-limit'), limit)
      .pipe(map(response => new IResourceLimitationExt(response)))
      .toPromise();
  }
}


