import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {NodeInfo, NodesApiService} from '../nodes-api.service';

@Component({
  selector: 'app-server-overview',
  templateUrl: './server-overview.component.html',
  styleUrls: ['./server-overview.component.css']
})
export class ServerOverviewComponent implements OnInit, OnDestroy {

  @Input('node') node: NodeInfo;

  single: any[] = [];
  series: any[] = [];
  memory: any[] = [];
  network: any[] = [];
  disk: any[] = [];

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

  schemeWriteRead = {
    domain: [
      '#FF0000',
      '#00FF00',
    ]
  };

  interval = null;

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

  ngOnDestroy(): void {
    if (this.interval) {
      clearInterval(this.interval);
      this.interval = null;
    }
  }

  ngOnInit() {
    for (let i = 0; i < 8; ++i) {
      this.single.push({
        name: 'CPU' + i,
        value: Math.random() * 100,
      });
    }

    this.update();
    this.interval = setInterval(() => this.update(), 1_000);
  }

  update() {
    if (this.node != null) {
      this.nodes.getNodeInfo(this.node.name).toPromise().then(result => {
        this.node = result;

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

        if (this.disk.length === 0) {
          this.initDiskSeries();
        }
        if (this.node.diskInfo) {
          this.updateDiskSeries();
        }

        if (this.node.cpuInfo && this.node.cpuInfo.utilization) {
          this.updateCpuSeries();
        }
      });
    }
  }

  private updateCpuSeries() {
    const cpus = this.node.cpuInfo.utilization;
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


  private initDiskSeries() {
    this.disk.push({
      name: 'Write',
      series: []
    });
    this.disk.push({
      name: 'Read',
      series: []
    });
  }

  private updateDiskSeries() {
    this.disk[0].series.push({
      name: new Date(),
      value: this.node.diskInfo.writing,
    });
    this.disk[1].series.push({
      name: new Date(),
      value: this.node.diskInfo.reading,
    });
    this.disk = [this.disk[0], this.disk[1]];
    for (const entry of this.disk) {
      if (entry.series.length > 120) {
        entry.series.splice(0, entry.series.length - 120);
      }
    }
  }

  getTotalMemoryFormatted() {
    let gigabytes = 0;
    if (this.node !== null) {
      gigabytes = this.bytesToGigabyte(this.node.memInfo.memoryTotal);
    }
    return gigabytes.toFixed(1) + ' GiB';
  }
}
