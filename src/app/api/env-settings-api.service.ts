import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../environments/environment';
import {map} from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class EnvSettingsApiService {

  constructor(private client: HttpClient) {
  }

  private static getUrl(more?: string) {
    return `${environment.apiLocation}settings${more != null ? `/${more}` : ''}`;
  }

  getGlobalEnvironmentVariables(): Promise<Map<string, string>> {
    return this.client.get<Map<string, string>>(EnvSettingsApiService.getUrl('global-env'))
      .pipe(map(response => new Map(Object.entries(response))))
      .toPromise();
  }

  setGlobalEnvironmentVariables(env: any): Promise<void> {
    const form = new FormData();
    form.set('env', JSON.stringify(env));
    return this.client.post<any>(EnvSettingsApiService.getUrl('global-env'), form)
      .pipe(map(v => {
        return;
      }))
      .toPromise();
  }

}
