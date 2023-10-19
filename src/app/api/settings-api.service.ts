import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../environments/environment';
import {map} from 'rxjs/operators';
import {ResourceLimitation} from './winslow-api';
import {loadResourceLimitation} from './project-api.service';
import {lastValueFrom} from "rxjs";

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
    return lastValueFrom(
      this.client
        .get<Map<string, string>>(SettingsApiService.getUrl('global-env'))
        .pipe(map(response => new Map(Object.entries(response))))
    );
  }

  setGlobalEnvironmentVariables(env: any): Promise<void> {
    return lastValueFrom(
      this.client.post<void>(SettingsApiService.getUrl('global-env'), env)
    );
  }

  getUserResourceLimitation(): Promise<ResourceLimitation> {
    return lastValueFrom(
      this.client
        .get<ResourceLimitation>(SettingsApiService.getUrl('user-res-limit'))
        .pipe(map(response => loadResourceLimitation(response)))
    );
  }

  setUserResourceLimitation(limit: ResourceLimitation): Promise<ResourceLimitation> {
    return lastValueFrom(
      this.client
        .post<ResourceLimitation>(SettingsApiService.getUrl('user-res-limit'), limit)
        .pipe(map(response => loadResourceLimitation(response)))
    );
  }
}
