import {Component, OnDestroy, OnInit} from '@angular/core';
import {NodeInfo, NodesApiService} from '../api/nodes-api.service';
import {Subscription} from 'rxjs';
import {ChangeType} from '../api/api.service';
import { EChartOption } from 'echarts';

@Component({
  selector: 'app-servers-new',
  templateUrl: './servers-new.component.html',
  styleUrls: ['./servers-new.component.css']
})
export class ServersNewComponent implements OnInit, OnDestroy {

  nodes: NodeInfo[] = [];
  loadError = null;
  subscription: Subscription = null;

  constructor(private api: NodesApiService) {
  }

  ngOnInit() {
    this.subscription = this.api.watchNodes(update => {
      switch (update.type) {
        case ChangeType.CREATE:
        case ChangeType.UPDATE:
          if (update.value != null) {
            const indexUpdate = this.nodes.findIndex(value => value.name === update.identifier);
            if (indexUpdate >= 0) {
              if (this.nodes[indexUpdate]?.update != null) {
                this.nodes[indexUpdate]?.update(update.value);
              }
            } else {
              this.nodes.push(update.value);
              this.sortNodesByName();
            }
          }
          break;
        case ChangeType.DELETE:
          const indexDelete = this.nodes.findIndex(value => value.name === update.identifier);
          if (indexDelete >= 0) {
            this.nodes.splice(indexDelete, 1);
          }
          break;
      }
    });
  }

  sortNodesByName() {
    this.nodes = this.nodes.sort((a, b) => a.name > b.name ? 1 : -1);
  }

  ngOnDestroy(): void {
    if (this.subscription) {
      this.subscription.unsubscribe();
      this.subscription = null;
    }
  }

  public trackNodeInfo(nodeInfo: NodeInfo): string {
    return nodeInfo?.name;
  }

}
