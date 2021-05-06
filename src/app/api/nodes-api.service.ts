import {Injectable} from '@angular/core';
import {RxStompService} from '@stomp/ng2-stompjs';
import {ChangeEvent} from './api.service';
import {Subscription} from 'rxjs';
import {Message} from '@stomp/stompjs';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class NodesApiService {

    constructor(
        private rxStompService: RxStompService,
        private client: HttpClient) {
    }

    static getUrl(more?: string) {
        if (more != null) {
            while (more.startsWith('/')) {
                more = more.substr(1);
            }
        }
        return `${environment.apiLocation}nodes${more != null ? `/${more}` : ''}`;
    }

    public watchNodes(listener: (update: ChangeEvent<string, NodeInfo>) => void): Subscription {
        return this.rxStompService.watch('/nodes').subscribe((message: Message) => {
            const events: ChangeEvent<string, NodeInfo>[] = JSON.parse(message.body);
            events.forEach(event => listener(event));
        });
    }

    /**
     * Retrieves the `NodeInfo` for all active nodes
     */
    public getNodes(): Promise<NodeInfo> {
        return this.client
            .get<NodeInfo>(NodesApiService.getUrl())
            .toPromise();
    }

    /**
     * Retrieves `NodeUtilization`-reports for a given time span
     *
     * @param nodeName The name of the node to return the utilization report for
     * @param from Unix epoch timestamp in millis from when to fetch the earliest report
     * @param to Unix epoch timestamp in millis from when to fetch the last report
     */
    public getNodeUtilization(nodeName: string, from?: number, to?: number): Promise<NodeUtilization[]> {
        return this.client
            .get<NodeUtilization[]>(NodesApiService.getUrl(nodeName + '/utilization'))
            .toPromise();
    }
}

export class NodeInfo {
  name: string;
  // The time this info was generated at (UNIX timestamp in ms)
  time: number;
  // The time in ms this node is up for
  uptime: number;
  cpuInfo: CpuInfo;
  memInfo: MemInfo;
  netInfo: NetInfo;
  diskInfo: DiskInfo;
  gpuInfo: GpuInfo[];
  buildInfo: BuildInfo;

  // local only
  update: (node: NodeInfo) => void;

  constructor(name: string, time: number, uptime: number, cpuInfo: CpuInfo, memInfo: MemInfo, gpus: GpuInfo[], buildInfo?: BuildInfo) {
    this.name = name;
    this.time = time;
    this.uptime = uptime;
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

export class NodeUtilization {
  time: number;
  uptime: number;
  cpuUtilization: number[];
  memoryInfo: MemInfo;
  netInfo: NetInfo;
  diskInfo: DiskInfo;
  gpuComputeUtilization: number[];
  gpuMemoryUtilization: number[];
}
