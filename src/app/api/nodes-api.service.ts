import {Injectable} from '@angular/core';
import {RxStompService} from '@stomp/ng2-stompjs';
import {ChangeEvent} from './api.service';
import {Subscription} from 'rxjs';
import {Message} from '@stomp/stompjs';

@Injectable({
  providedIn: 'root'
})
export class NodesApiService {

  constructor(private rxStompService: RxStompService) {
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
  time: number;
  cpuInfo: CpuInfo;
  memInfo: MemInfo;
  netInfo: NetInfo;
  diskInfo: DiskInfo;
  gpuInfo: GpuInfo[];
  buildInfo: BuildInfo;

  // local only
  update: (node: NodeInfo) => void;

  constructor(name: string, time: number, cpuInfo: CpuInfo, memInfo: MemInfo, gpus: GpuInfo[], buildInfo?: BuildInfo) {
    this.name = name;
    this.time = time;
    this.cpuInfo = new CpuInfo(cpuInfo.modelName, cpuInfo.utilization.length);
    this.memInfo = new MemInfo(memInfo.memoryTotal, memInfo.swapTotal);
    this.netInfo = new NetInfo();
    this.diskInfo = new DiskInfo();
    this.gpuInfo = [];
    this.buildInfo = buildInfo == null ? new BuildInfo() : buildInfo;

    for (const gpu of gpus) {
      this.gpuInfo.push(new GpuInfo(gpu.id, gpu.vendor, gpu.name));
    }
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

  constructor(memoryTotal: number, swapTotal: number) {
    this.memoryTotal = memoryTotal;
    this.memoryFree = memoryTotal;
    this.swapTotal = swapTotal;
    this.swapFree = swapTotal;
  }
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
  id: string;
  vendor: string;
  name: string;
  computeUtilization = 0;
  memoryUtilization = 0;

  constructor(id: string, vendor: string, name: string) {
    this.id = id;
    this.vendor = vendor;
    this.name = name;
  }
}

export class BuildInfo {
  date: string;
  commitHashShort: string;
  commitHashLong: string;
}
