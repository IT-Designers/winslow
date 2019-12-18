import { Injectable } from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../environments/environment';

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
    return this.client.get<StorageInfo[]>(StorageApiService.getUrl());
  }

  getFilePathInfo(path: string) {
    return this
      .client
      .get<StorageInfo>(StorageApiService.getUrl(path))
      .toPromise();
  }

  getResourcesInfo() {
    return this
      .client
      .get<StorageInfo>(StorageApiService.getUrl(`resources`))
      .toPromise();
  }

  getWorkspaceInfo(projectId: string) {
    return this
      .client
      .get<StorageInfo>(StorageApiService.getUrl(`workspaces/${projectId}`))
      .toPromise();
  }
}

export class StorageInfo {
  name: string;
  bytesUsed: number;
  bytesFree: number;
}
