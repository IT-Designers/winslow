import {Component, Input, OnInit} from '@angular/core';
import {NodeInfo, NodesApiService} from '../api/nodes-api.service';

@Component({
  selector: 'app-server',
  templateUrl: './server.component.html',
  styleUrls: ['./server.component.css']
})
export class ServerComponent implements OnInit {

  constructor(private nodes: NodesApiService) {
  }

  static readonly MAX_ENTRIES = 120;

  @Input('node') node: NodeInfo;

  memory: any[] = [];
  network: any[] = [];
  disk: any[] = [];
  diskUsage: any[] = [];
  cpus: any[] = [];

  rawNetwork: [Date, number[]][] = [];
  rawDisk: [Date, number[]][] = [];

  unitNetwork = '';
  unitDisk = '';

  colorSchemeDiskUsageW95 = {
    domain: [
      '#ff00ff', '#0000ff'
    ]
  };

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

  private static orNow(date?: Date): Date {
    if (date == null) {
      return new Date();
    } else {
      return date;
    }
  }

  diskUsageLabelFormatting = value => value + ' GiB';
  diskUsageToolTipFormatting = value => value.value.toFixed(1) + ' GiB';

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
    const backupNode = this.node;
    this.node = new NodeInfo(this.node.name, this.node.cpuInfo.modelName, this.node.cpuInfo.utilization.length, this.node.buildInfo);

    const date = new Date();
    for (let i = ServerComponent.MAX_ENTRIES; i >= 0; --i) {
      this.update(new Date(date.getTime() - (i * 1000)));
    }

    this.node = backupNode;
    this.node.update = (node) => {
      // load all the new goodies without replacing the object
      Object.keys(node).forEach(key => {
        this.node[key] = node[key];
      });
      this.update();
    };
    this.update(date);
  }

  update(date: Date = null) {
    date = ServerComponent.orNow(date);

    if (this.memory.length === 0) {
      this.initMemorySeries();
    }

    if (this.node.memInfo) {
      this.updateMemorySeries(date);
    }

    if (this.network.length === 0) {
      this.initNetworkSeries();
    }
    if (this.node.netInfo) {
      this.updateNetworkSeries(date);
      this.scaleNetwork();
    }

    if (this.disk.length === 0) {
      this.initDiskSeries();
    }
    if (this.diskUsage.length === 0) {
      this.initDiskUsageSeries();
    }
    if (this.node.diskInfo) {
      this.updateDiskSeries(date);
      this.updateDiskUsageSeries();
      this.scaleDisk();
    }

    if (this.node.cpuInfo && this.node.cpuInfo.utilization) {
      this.updateCpuSeries();
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

  private updateMemorySeries(date: Date = null) {
    date = ServerComponent.orNow(date);
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
      if (entry.series.length > ServerComponent.MAX_ENTRIES) {
        entry.series.splice(0, entry.series.length - ServerComponent.MAX_ENTRIES);
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

  private updateNetworkSeries(date: Date = null) {
    this.rawNetwork.push([ServerComponent.orNow(date), [this.node.netInfo.transmitting, this.node.netInfo.receiving]]);
    if (this.rawNetwork.length > ServerComponent.MAX_ENTRIES) {
      this.rawNetwork.splice(0, this.rawNetwork.length - ServerComponent.MAX_ENTRIES);
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

  private updateDiskSeries(date: Date = null) {
    this.rawDisk.push([ServerComponent.orNow(date), [this.node.diskInfo.writing, this.node.diskInfo.reading]]);
    if (this.rawDisk.length > ServerComponent.MAX_ENTRIES) {
      this.rawDisk.splice(0, this.rawDisk.length - ServerComponent.MAX_ENTRIES);
    }
  }

  private initDiskUsageSeries() {
    this.diskUsage.push({
      name: 'Free',
      value: 1
    });
    this.diskUsage.push({
      name: 'Used',
      value: 1
    });
  }

  private updateDiskUsageSeries() {
    this.diskUsage[0].value = this.bytesToGigabyte(this.node.diskInfo.free);
    this.diskUsage[1].value = this.bytesToGigabyte(this.node.diskInfo.used);
    this.diskUsage = this.diskUsage.map(u => u);
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
