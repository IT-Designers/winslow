import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../environments/environment';
import {map} from 'rxjs/operators';

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

  getUserResourceLimitation(): Promise<UserResourceLimitation> {
    return this.client.get<UserResourceLimitation>(SettingsApiService.getUrl('user-res-limit'))
      .pipe(map(response => new UserResourceLimitation(response)))
      .toPromise();
  }

  setUserResourceLimitation(limit: UserResourceLimitation): Promise<UserResourceLimitation> {
    return this.client.post<UserResourceLimitation>(SettingsApiService.getUrl('user-res-limit'), limit)
      .pipe(map(response => new UserResourceLimitation(response)))
      .toPromise();
  }
}


export class UserResourceLimitation {
  cpu?: number;
  mem?: number;
  gpu?: number;

  constructor(origin?: UserResourceLimitation) {
    if (origin != null) {
      console.log('Loading: ' + JSON.stringify(origin));
      Object.assign(this, origin);
    }
  }
}
