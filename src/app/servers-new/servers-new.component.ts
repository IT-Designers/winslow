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

  isLoading = false;
  options = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross',
        label: {
          backgroundColor: '#6a7985'
        }
      }
    },
    legend: {
      data: ['X-1', 'X-2', 'X-3', 'X-4', 'X-5']
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: [
      {
        type: 'category',
        boundaryGap: false,
        data: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
      }
    ],
    yAxis: [
      {
        type: 'value'
      }
    ],
    series: [
      {
        name: 'X-1',
        type: 'line',
        stack: 'counts',
        areaStyle: { normal: {} },
        data: [120, 132, 101, 134, 90, 230, 210]
      },
      {
        name: 'X-2',
        type: 'line',
        stack: 'counts',
        areaStyle: { normal: {} },
        data: [220, 182, 191, 234, 290, 330, 310]
      },
      {
        name: 'X-3',
        type: 'line',
        stack: 'counts',
        areaStyle: { normal: {} },
        data: [150, 232, 201, 154, 190, 330, 410]
      },
      {
        name: 'X-4',
        type: 'line',
        stack: 'counts',
        areaStyle: { normal: {} },
        data: [320, 332, 301, 334, 390, 330, 320]
      },
      {
        name: 'X-5',
        type: 'line',
        stack: 'counts',
        label: {
          normal: {
            show: true,
            position: 'top'
          }
        },
        areaStyle: { normal: {} },
        data: [820, 932, 901, 934, 1290, 1330, 1320]
      }
    ]
  };
}
