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
  network: any[] = [];

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

  schemeMemory = {
    domain: [
      '#00FF00', // heap
      '#F0FF00', // system cache
      '#FF0000', // swap
    ]
  };

  constructor(private nodes: NodesApiService) {
  }

  bytesToGigabyte(bytes: number) {
    return bytes / (1024 * 1024 * 1024);
  }

  totalMemoryAllPools() {
    if (this.node.memInfo) {
      return this.bytesToGigabyte(this.node.memInfo.memoryTotal + this.node.memInfo.swapTotal);
    } else {
      return 0;
    }
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

      if (this.node == null) {
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

        this.cpus = this.series;
      } else {
        if (this.memory.length === 0) {
          this.initMemorySeries();
        }

        if (this.node.memInfo) {
          this.updateMemorySeries();
        }

        if (this.network.length === 0) {
          this.initNetworkSeries();
        }
        if (this.node.netInfo) {
          this.updateNetworkSeries();
        }


        this.nodes.getNodeInfo(this.node.name).toPromise().then(result => this.node = result);
        const cpus = this.node.cpuInfo.utilization;
        if (cpus) {
          const cpusReplacement = [];
          for (let i = 0; i < cpus.length; ++i) {
            const value = Number(Math.max(0, Math.min(100, cpus[i] * 100)));
            cpusReplacement.push({
              name: i,
              value
            });
            if (Number.isNaN(value)) {
              return; // nope out of it
            }
          }
          if (cpusReplacement.length === 0) {
            return;
          }
          this.cpus = cpusReplacement;
        }
      }
    }, 1000);
  }

  private initMemorySeries() {
    this.memory.push({
      name: 'Heap',
      series: []
    });
    this.memory.push({
      name: 'Cache',
      series: []
    });
    this.memory.push({
      name: 'Swap',
      series: []
    });
  }

  private updateMemorySeries() {
    this.memory[0].series.push({
      name: new Date(),
      value: this.bytesToGigabyte(this.node.memInfo.memoryTotal - this.node.memInfo.memoryFree),
    });
    this.memory[1].series.push({
      name: new Date(),
      value: this.bytesToGigabyte(this.node.memInfo.systemCache),
    });
    this.memory[2].series.push({
      name: new Date(),
      value: this.bytesToGigabyte(this.node.memInfo.swapTotal - this.node.memInfo.swapFree),
    });
    this.memory = [this.memory[0], this.memory[1], this.memory[2]];
    for (const entry of this.memory) {
      if (entry.series.length > 120) {
        entry.series.splice(0, entry.series.length - 120);
      }
    }
  }


  private initNetworkSeries() {
    this.network.push({
      name: 'Tx',
      series: []
    });
    this.network.push({
      name: 'Rx',
      series: []
    });
  }

  private updateNetworkSeries() {
    this.network[0].series.push({
      name: new Date(),
      value: this.node.netInfo.transmitting,
    });
    this.network[1].series.push({
      name: new Date(),
      value: this.node.netInfo.receiving,
    });
    this.network = [this.network[0], this.network[1]];
    for (const entry of this.network) {
      if (entry.series.length > 120) {
        entry.series.splice(0, entry.series.length - 120);
      }
    }
  }
}
