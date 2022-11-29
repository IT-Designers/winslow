import { Injectable } from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../environments/environment';
import {IStorageInfo} from './winslow-api';

@Injectable({
  providedIn: 'root'
})
export class StorageApiService {

  constructor(private client: HttpClient) {
  }

  private static getUrl(more?: string) {
    if (more != null && more.startsWith('/')) {
      more = more.substr(1);
    }
    return `${environment.apiLocation}storage${more != null ? `/${more}` : ''}`;
  }

  getAll() {
    return this.client.get<IStorageInfo[]>(StorageApiService.getUrl());
  }

  getFilePathInfo(path: string) {
    return this
      .client
      .get<IStorageInfo>(StorageApiService.getUrl(path))
      .toPromise();
  }

  getResourcesInfo() {
    return this
      .client
      .get<IStorageInfo>(StorageApiService.getUrl(`resources`))
      .toPromise();
  }

  getWorkspaceInfo(projectId: string) {
    return this
      .client
      .get<IStorageInfo>(StorageApiService.getUrl(`workspaces/${projectId}`))
      .toPromise();
  }
}

