import { Injectable } from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class StorageApiService {

  constructor(private client: HttpClient) {
  }

  getAll() {
    return this.client.get<StorageInfo[]>(`${environment.apiLocation}storage`);
  }
}

export class StorageInfo {
  name: string;
  bytesUsed: number;
  bytesFree: number;
}
