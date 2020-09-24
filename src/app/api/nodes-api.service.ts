import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../environments/environment';
import {RxStompService} from '@stomp/ng2-stompjs';
import {ChangeEvent} from './api.service';
import {Subscription} from 'rxjs';
import {Message} from '@stomp/stompjs';
import {StatsInfo} from './project-api.service';

@Injectable({
  providedIn: 'root'
})
export class NodesApiService {

  constructor(private client: HttpClient, private rxStompService: RxStompService) {
  }

  getAllNodeInfo() {
    return this.client.get<NodeInfo[]>(`${environment.apiLocation}nodes`);
  }
  getNodeInfo(node: string) {
    return this.client.get<NodeInfo>(`${environment.apiLocation}nodes/${node}`);
  }

  public watchNodes(listener: (update: ChangeEvent<string, NodeInfo>) => void): Subscription {
    return this.rxStompService.watch('/nodes').subscribe((message: Message) => {
      const events: ChangeEvent<string, NodeInfo>[] = JSON.parse(message.body);
      events.forEach(event => listener(event));
    });
  }
}

export class NodeInfo {
  name: string;
  cpuInfo: CpuInfo;
  memInfo: MemInfo;
  netInfo: NetInfo;
  diskInfo: DiskInfo;
  gpuInfo: GpuInfo[];
  buildInfo: BuildInfo;

  // local only
  update: (node: NodeInfo) => void;

  constructor(name: string, cpuModel: string, cpuCores: number, buildInfo?: BuildInfo) {
    this.name = name;
    this.cpuInfo = new CpuInfo(cpuModel, cpuCores);
    this.memInfo = new MemInfo();
    this.netInfo = new NetInfo();
    this.diskInfo = new DiskInfo();
    this.gpuInfo = [];
    this.buildInfo = buildInfo == null ? new BuildInfo() : buildInfo;
  }

}

export class CpuInfo {
  modelName: string;
  utilization: number[];

  constructor(model: string, cores: number) {
    this.modelName = model;
    this.utilization = [];
    for (let i = 0; i < cores; ++i) {
      this.utilization.push(0);
    }
  }

}

export class MemInfo {
  memoryTotal = 0;
  memoryFree = 0;
  systemCache = 0;
  swapTotal = 0;
  swapFree = 0;
}

export class NetInfo {
  transmitting = 0;
  receiving = 0;
}

export class DiskInfo {
  reading = 0;
  writing = 0;
  free = 0;
  used = 0;
}

export class GpuInfo {
  vendor: string;
  name: string;

}

export class BuildInfo {
  date: string;
  commitHashShort: string;
  commitHashLong: string;
}
