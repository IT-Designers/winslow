import {Component, Input, OnInit} from '@angular/core';
import {NodeInfo, NodesApiService} from '../nodes-api.service';

@Component({
  selector: 'app-server-overview',
  templateUrl: './server-overview.component.html',
  styleUrls: ['./server-overview.component.css']
})
export class ServerOverviewComponent implements OnInit {

  @Input('node') node: NodeInfo;

  single: any[] = [];
  series: any[] = [];
  memory: any[] = [];

  cpus: any[] = [];


  colorScheme = {
//    domain: ['#FF6666', '#66FF66', '#6666FF', '#777777']
    domain: [
      '#FF0000', '#FF7F00',
      '#FFFF00', '#00FF00',
      '#0000FF', '#0000FF',
      '#4B0082', '#9400D3'
    ]
  };

  constructor(private nodes: NodesApiService) {
  }

  ngOnInit() {
    for (let i = 0; i < 8; ++i) {
      this.single.push({
        'name': 'CPU' + i,
        'value': Math.random() * 100,
      });
    }

    setInterval(() => {

      const count = this.single.length;
      this.single = [];
      for (let i = 0; i < count; ++i) {
        this.single.push({
          'name': 'CPU' + i,
          'value': Math.random() * 100
        });
      }


      this.series = [];
      const series1 = [];
      const series2 = [];
      for (let i = 0; i < 120; ++i) {
        series1.push({
          name: new Date(new Date().getTime() + i * 10_000),
          value: Math.random() * 128 * 1024,
        });
        series2.push({
          name: new Date(new Date().getTime() + i * 10000),
          value: Math.random() * 128 * 1024,
        });
      }
      this.series.push({
        'name': 'Tx',
        'series': series1
      });
      this.series.push({
        'name': 'Rx',
        'series': series2
      });


      this.memory = [];
      const mem1 = [];
      const mem2 = [];
      for (let i = 0; i < 120; ++i) {
        mem1.push({
          name: new Date(new Date().getTime() + i * 10_000),
          value: 4 * 1024 * 1024 + Math.random() * 124 * 1024,
        });
        mem2.push({
          name: new Date(new Date().getTime() + i * 10000),
          value: 1024 * 1024 + Math.random() * 1024 * 1024,
        });
      }
      this.memory.push({
        'name': 'Heap',
        'series': mem1
      });
      this.memory.push({
        'name': 'SWAP',
        'series': mem2
      });

      if (this.node == null) {
        this.cpus = this.series;
      } else {
        this.nodes.getNodeInfo(this.node.name).toPromise().then(result => this.node = result);
        const cpus = this.node.cpuInfo.utilization;
        const cpusReplacement = [];
        for (let i = 0; i < cpus.length; ++i) {
          const value = Number(Math.max(0, Math.min(100, cpus[i] * 100)));
          cpusReplacement.push({
            name: i,
            value
          });
          if (Number.isNaN(value)) {
            return;
          }
        }
        if (cpusReplacement.length === 0) {
          return;
        }
        this.cpus = cpusReplacement;
      }
    }, 1000);
  }
}
