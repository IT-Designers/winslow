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
   * @param chunkSpanMillis The duration in millis to chunk data into a single entry
   */
  public getNodeUtilization(nodeName: string, from?: number, to?: number, chunkSpanMillis?: number): Promise<NodeUtilization[]> {
    const params = [['from', from], ['to', to], ['chunkSpanMillis', chunkSpanMillis]]
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






