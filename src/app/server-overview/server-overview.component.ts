import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {NodeInfo, NodesApiService} from '../api/nodes-api.service';

@Component({
  selector: 'app-server-overview',
  templateUrl: './server-overview.component.html',
  styleUrls: ['./server-overview.component.css']
})
export class ServerOverviewComponent implements OnInit, OnDestroy {

  @Input('node') node: NodeInfo;

  memory: any[] = [];
  network: any[] = [];
  disk: any[] = [];
  cpus: any[] = [];

  rawNetwork: [Date, number[]][] = [];
  rawDisk: [Date, number[]][] = [];

  unitNetwork = '';
  unitDisk = '';

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
          this.scaleNetwork();
        }

        if (this.disk.length === 0) {
          this.initDiskSeries();
        }
        if (this.node.diskInfo) {
          this.updateDiskSeries();
          this.scaleDisk();
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
    const date = new Date();
    this.memory[0].series.push({
      name: date,
      value: this.bytesToGigabyte(this.node.memInfo.memoryTotal - this.node.memInfo.memoryFree),
    });
    this.memory[1].series.push({
      name: date,
      value: this.bytesToGigabyte(this.node.memInfo.systemCache),
    });
    this.memory[2].series.push({
      name: date,
      value: this.bytesToGigabyte(this.node.memInfo.swapTotal - this.node.memInfo.swapFree),
    });
    this.memory = [this.memory[0], this.memory[1], this.memory[2]];
    for (const entry of this.memory) {
      if (entry.series.length > 120) {
        entry.series.splice(0, entry.series.length - 120);
      }
    }
    this.memory = [this.memory[0], this.memory[1], this.memory[2]];
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
    this.rawNetwork.push([new Date(), [this.node.netInfo.transmitting, this.node.netInfo.receiving]]);
    if (this.rawNetwork.length > 120) {
      this.rawNetwork.splice(0, this.rawNetwork.length - 120);
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
    this.rawDisk.push([new Date(), [this.node.diskInfo.writing, this.node.diskInfo.reading]]);
    if (this.rawDisk.length > 120) {
      this.rawDisk.splice(0, this.rawDisk.length - 120);
    }
  }

  getTotalMemoryFormatted() {
    let gigabytes = 0;
    if (this.node !== null) {
      gigabytes = this.bytesToGigabyte(this.node.memInfo.memoryTotal);
    }
    return gigabytes.toFixed(1) + ' GiB';
  }

  scaleNetwork() {
    this.unitNetwork = this.scaleUnits(this.rawNetwork, this.network);
    this.network = [this.network[0], this.network[1]]; // to notify the binding
  }

  scaleDisk() {
    this.unitDisk = this.scaleUnits(this.rawDisk, this.disk);
    this.disk = [this.disk[0], this.disk[1]]; // to notify the binding
  }

  scaleUnits(input: [Date, number[]][], output: any[]): string {
    let maxPot = 0;
    let div = 1;

    for (const v of input) {
      for (const series of v[1]) {
        while (series / div > 1024) {
          div *= 1024;
          maxPot += 1;
        }
      }
    }
    for (let i = 0; i < input.length; ++i) {
      for (let n = 0; n < input[i][1].length; ++n) {
        if (output[n].series.length <= i) {
          output[n].series.push({
            name: input[i][0],
            value: input[i][1][n] / div,
          });
        } else {
          output[n].series[i] = {
            name: input[i][0],
            value: input[i][1][n] / div,
          };
        }
      }
    }

    switch (maxPot) {
      default:
      case 0:
        return '';
      case 1:
        return 'Ki';
      case 2:
        return 'Mi';
      case 3:
        return 'Gi';
      case 4:
        return 'Ti';
      case 5:
        return 'Pi';
    }
  }
}
