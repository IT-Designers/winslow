import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from '../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class NodesApiService {

  constructor(private client: HttpClient) {
  }

  getNodeInfo(node: string) {
    return this.client.get<NodeInfo>(`${environment.apiLocation}/nodes/${node}`);
  }
}

export class NodeInfo {
  name: string;
  cpuInfo: CpuInfo;
  memInfo: MemInfo;
  netInfo: NetInfo;
  diskInfo: DiskInfo;
}

export class CpuInfo {
  modelName: string;
  utilization: number[];
}

export class MemInfo {
  memoryTotal: number;
  memoryFree: number;
  systemCache: number;
  swapTotal: number;
  swapFree: number;
}

export class NetInfo {
  transmitting: number;
  receiving: number;
}

export class DiskInfo {
  reading: number;
  writing: number;
}
