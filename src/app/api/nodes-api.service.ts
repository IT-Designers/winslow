import {Injectable} from '@angular/core';
import {RxStompService} from '@stomp/ng2-stompjs';
import {ChangeEvent} from './api.service';
import {Subscription} from 'rxjs';
import {Message} from '@stomp/stompjs';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../environments/environment';
import {AllocInfo, BuildInfo, CpuInfo, DiskInfo, GpuInfo, MemInfo, NetInfo, NodeInfo, NodeUtilization} from './winslow-api';

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

  public watchNodes(listener: (update: ChangeEvent<string, NodeInfoExt>) => void): Subscription {
    return this.rxStompService.watch('/nodes').subscribe((message: Message) => {
      const events: ChangeEvent<string, NodeInfoExt>[] = JSON.parse(message.body);
      events.forEach(event => listener(event));
    });
  }

  /**
   * Retrieves the `NodeInfo` for all active nodes
   */
  public getNodes(): Promise<NodeInfoExt> {
    return this.client
      .get<NodeInfoExt>(NodesApiService.getUrl())
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
        const params = [['from', from], ['to', to]]
        .filter(p => p != null && p[1] != null)
        .map(p => p[0] + '=' + p[1])
        .join('&');

      return this.client
        .get<NodeUtilization[]>(NodesApiService.getUrl(
          nodeName + '/utilization' + (params.length > 0 ? '?' + params : '')
        ))
        .toPromise();
    }
}

/**
 *  The time this info was generated at (UNIX timestamp in ms)
 *  time
 *  The time in ms this node is up for
 *  uptime
 */
export class NodeInfoExt extends NodeInfo {
  // local only
  update: (node: NodeInfoExt) => void;
}

export class NodeInfo {
  name: string;

  time: number;

  uptime: number;
  cpuInfo: ICpuInfo;
  memInfo: IMemInfo;
  netInfo: INetInfo;
  diskInfo: IDiskInfo;
  gpuInfo: IGpuInfo[];
  buildInfo: IBuildInfo;
  allocInfo: IAllocInfo[];

  // local only
  update: (node: NodeInfo) => void;

  constructor(
    name: string,
    time: number,
    uptime: number,
    cpuInfo: CpuInfo,
    memInfo: MemInfo,
    gpus: GpuInfo[],
    buildInfo?: BuildInfo,
    allocInfo?: AllocInfo[]
  ) {

    this.name = name;
    this.time = time;
    this.uptime = uptime;
    this.cpuInfo = new CpuInfo(cpuInfo.modelName, cpuInfo.utilization.length);
    this.memInfo = new MemInfo(memInfo.memoryTotal, memInfo.swapTotal);
    this.netInfo = new NetInfo();
    this.diskInfo = new DiskInfo();
    this.gpuInfo = [];
    this.buildInfo = buildInfo == null ? new BuildInfo() : buildInfo;
    this.allocInfo = allocInfo == null ? [] : allocInfo;

    for (const gpu of gpus) {
      this.gpuInfo.push(new GpuInfo(gpu.id, gpu.vendor, gpu.name));
    }
  }

}






export class GpuInfo {
  id: string;
  vendor: string;
  name: string;
  computeUtilization = 0;
  memoryUtilization = 0;
  memoryUsedMegabytes = 0;
  memoryTotalMegabytes = 0;

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
  gpuUtilization: GpuUtilization[];
}

export class GpuUtilization {
  computeUtilization = 0;
  memoryUtilization = 0;
  memoryUsedMegabytes = 0;
  memoryTotalMegabytes = 0;
}

export class AllocInfo {
  title: string;
  cpu: number;
  memory: number;
  gpu: number;
}

