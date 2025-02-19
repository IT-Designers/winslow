import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {Subscription} from 'rxjs';
import {ChangeType} from '../api/api.service';
import {NodeInfoExt, NodesApiService} from '../api/nodes-api.service';
import {GpuInfo} from '../api/winslow-api';
import {EChartsOption} from "echarts";

@Component({
  selector: 'app-server-details',
  templateUrl: './server-details.component.html',
  styleUrls: ['./server-details.component.css']
})
export class ServerDetailsComponent implements OnInit, OnDestroy {

  constructor(private api: NodesApiService) {
    this.initMemorySeries();
    this.initNetworkSeries();
    this.initDiskSeries();
    this.initTimeSeries();
  }

  static readonly MAX_ENTRIES = 120;

  @Input() nodeName!: string;
  @Input() historyEnabled!: boolean;
  lastNodeName = '';

  isLive = true;
  panelOpenState = false;

  nodes: NodeInfoExt[] = [];
  node?: NodeInfoExt;
  selectedNodeIndex?: number;
  historyButtonValue = 'live';
  loadError = null;
  subscription?: Subscription;

  date?: Date;
  lastTimestamp?: number;
  formatter: (value: any) => string = (params: any): string => {
    params = params[0];
    const date = new Date(params.name);
    const zero = (date.getMinutes() < 10 ? '0' : '');
    return date.getDate() + '/' + (date.getMonth() + 1) + '/' + date.getFullYear() + '  ' +
      date.getHours() + ':' + zero + date.getMinutes() + '<br>' +
      params.seriesName + ': ' + params.value[1] + '%';
  };

  unitNetwork = '';
  unitDisk = '';
  unitGpu = '';

  memoryTotal = '';
  memoryFree = '';
  memoryUsed = ';';

  diskTotal = '';
  diskFree = '';

  cpus: any[] = [];
  cpuUsage = 0;
  mergeOptionCpu: EChartsOption = {};
  chartOptionCpu: EChartsOption = {
    tooltip: {
      position: 'top',
      confine: true,
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      formatter: (value: any) => this.formatter(value),
    },
    calculable: false,
    grid: {
      top: '5%',
      bottom: '25',
      left: '50',
      right: '10'
    },
    xAxis: [
      {
        type: 'time',
        splitLine: {
          show: false,
        },
        show: true,
        axisLabel: {
          formatter: (value: any) => this.formatter(value)
        }
      },
    ],
    yAxis: {
      type: 'value',
      axisLabel: {
        formatter: '{value} %',
      },
      scale: true,
      max: 100,
      min: 0,
      splitNumber: 5,
      splitLine: {
        show: true,
      },
    },
    series: [],
  };

  memory: any[] = [];
  mergeOptionMemory: EChartsOption = {};
  chartOptionMemory: EChartsOption = {
    tooltip: {
      position: 'top',
      confine: true,
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      formatter: (params: any) => {
        const date = new Date(params[0].name);
        const zero = (date.getMinutes() < 10 ? '0' : '');
        return `  ${date.getDate()}/${date.getMonth() + 1}/${date.getFullYear()}  ${date.getHours()}:${zero + date.getMinutes()} <br>
                  Memory Usage: <br />
                  ${params[0].seriesName}: ${(params[0].value[1] ? params[0].value[1] : 0) + ' GiB'}<br />
                  ${params[1].seriesName}: ${(params[1].value[1] ? params[1].value[1] : 0) + ' GiB'}<br />
                  ${params[2].seriesName}: ${(params[2].value[1] ? params[2].value[1] : 0) + ' GiB'}
                  `;
      },
    },
    grid: {
      top: '5%',
      bottom: '25',
      left: '50',
      right: '10'
    },
    calculable: false,
    xAxis: [
      {
        show: true,
        type: 'time',
        axisLabel: {
          formatter: (value: any) => this.formatter(value)
        },
        splitLine: {
          show: false,
        },
      },
    ],
    yAxis: [
      {
        type: 'value',
        min: 0,
        axisLabel: {
          formatter: '{value} GiB',
        },
        splitNumber: 4,
        splitLine: {
          show: true,
        },
      },
    ],
    series: [],
  };

  rawNetwork: [Date, number[]][] = [];
  network: any[] = [];
  mergeOptionNetwork: EChartsOption = {};
  chartOptionNetwork: EChartsOption = {
    tooltip: {
      position: 'top',
      confine: true,
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      formatter: (params: any) => {
        const date = new Date(params[0].name);
        const zero = (date.getMinutes() < 10 ? '0' : '');
        return `  ${date.getDate()}/${date.getMonth() + 1}/${date.getFullYear()}  ${date.getHours()}:${zero + date.getMinutes()} <br>
                  Network IO: <br />
                  ${params[0].seriesName}: ${params[0].value[1] + ' ' + this.unitNetwork + 'Byte/s'}<br />
                  ${params[1].seriesName}: ${params[1].value[1] + ' ' + this.unitNetwork + 'Byte/s'}
                  `;
      },
    },
    grid: {
      top: '5%',
      bottom: '25',
      left: '90',
      right: '10'
    },
    calculable: false,
    xAxis: [
      {
        show: true,
        type: 'time',
        axisLabel: {
          formatter: (value: any) => this.formatter(value)
        },
        splitLine: {
          show: false,
        },
        splitNumber: 4,
      },
    ],
    yAxis: [
      {
        type: 'value',
        axisLabel: {
          formatter: '{value}GB',
        },
        scale: true,
        min: 0,
        splitNumber: 6,
        splitLine: {
          show: true,
        },
      },
    ],
    series: [],
  };

  rawDisk: [Date, number[]][] = [];
  disk: any[] = [];
  mergeOptionDisk: EChartsOption = {};
  chartOptionDisk: EChartsOption = {
    tooltip: {
      position: 'top',
      confine: true,
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      formatter: (params: any) => {
        const date = new Date(params[0].name);
        const zero = (date.getMinutes() < 10 ? '0' : '');
        return `  ${date.getDate()}/${date.getMonth() + 1}/${date.getFullYear()}  ${date.getHours()}:${zero + date.getMinutes()} <br>
                  Disk IO: <br />
                  ${params[0].seriesName}: ${params[0].value[1] + ' ' + this.unitDisk + 'Byte/s'}<br />
                  ${params[1].seriesName}: ${params[1].value[1] + ' ' + this.unitDisk + 'Byte/s'}
                  `;
      },
    },
    grid: {
      top: '5%',
      bottom: '25',
      left: '90',
      right: '10'
    },
    calculable: false,
    xAxis: [
      {
        show: true,
        type: 'time',
        axisLabel: {
          formatter: (value: any) => this.formatter(value)
        },
        splitLine: {
          show: false,
        },
        splitNumber: 4,
      },
    ],
    yAxis: [
      {
        type: 'value',
        axisLabel: {
          formatter: '{value}GB',
        },
        scale: true,
        min: 0,
        splitNumber: 6,
        splitLine: {
          show: true,
        },
      },
    ],
    series: [],
  };

  gpus: any[] = [];
  gpuName: string[] = [];
  mergeOptionGpu: EChartsOption[] = [];
  chartOptionGpu: EChartsOption = {
    tooltip: {
      position: 'top',
      confine: true,
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      formatter: (params: any) => {
        const date = new Date(params[0].name);
        const zero = (date.getMinutes() < 10 ? '0' : '');
        return ` ${date.getDate()}/${date.getMonth() + 1}/${date.getFullYear()}  ${date.getHours()}:${zero + date.getMinutes()} <br>
                ${params[0].seriesName}: ${params[0].value[1] + '%'}<br>
                ${params[1].seriesName}: ${params[1].value[1] + '%'} `;
      },
    },
    calculable: false,
    grid: {
      top: '10%',
      bottom: '25',
      left: '50',
      right: '10'
    },
    xAxis: [
      {
        type: 'time',
        splitLine: {
          show: false,
        },
        show: true,
        axisLabel: {
          formatter: (value: any) => this.formatter(value)
        }
      },
    ],
    yAxis: {
      type: 'value',
      axisLabel: {
        formatter: '{value} %',
      },
      scale: true,
      max: 100,
      min: 0,
      splitNumber: 5,
      splitLine: {
        show: true,
      },
    },

    series: [],
  };

  /*Ressource management using groups*/
  mockGroups = [
    {
      name: 'NewGroup1',
      role: 'OWNER'
    }
  ];

  average = (arr: number[]) => arr.reduce((p, c) => p + c, 0) / arr.length;

  axisLabelFormatterMinutes(value: string | number | Date): string {
    const date = new Date(value);
    if (date.getSeconds() === 0) {
      const zero = (date.getMinutes() < 10 ? ':0' : ':');
      return date.getHours() + zero + date.getMinutes();
    }
    return ''
  }

  axisLabelFormatterDays(value: string | number | Date): string {
    const date = new Date(value);
    if (date.getSeconds() === 0) {
      return date.getDate() + '.' + (date.getMonth() + 1);
    }
    return ''
  }

  ngOnInit() {
    this.formatter = this.axisLabelFormatterMinutes;
    this.subscription = this.api.watchNodes((update) => {

      switch (update.type) {
        case ChangeType.CREATE:
        case ChangeType.UPDATE:
          if (update.value != null) {
            const indexUpdate = this.nodes.findIndex(
              (value) => value.name === update.identifier
            );

            if (indexUpdate >= 0) {
              this.nodes[indexUpdate] = update.value;
            } else {
              this.nodes.push(update.value);
              this.sortNodesByName();
            }


            this.selectedNodeIndex = this.nodes.findIndex(
              (value) => value.name === this.nodeName
            );

            if (this.selectedNodeIndex == null || this.selectedNodeIndex === -1) {
              this.selectedNodeIndex = 0;
              this.node = this.nodes[0];
            } else {
              this.node = this.nodes[this.selectedNodeIndex];
            }

            // save last timestamp
            if (!this.lastTimestamp) {
              this.lastTimestamp = this.node.time; // update.value.time;
            }

            if (this.lastNodeName !== this.node.name) {
              this.lastNodeName = this.node.name;
              this.setNode(this.selectedNodeIndex);
            }

            // check if new timestamp is different
            // and if charts are in live modus
            // if yes, update diagrams
            if (this.lastTimestamp !== this.node.time && this.isLive) {
              this.lastTimestamp = this.node.time;

              this.date = new Date();
              this.updateCpuStatus();
              this.updateMemoryStatus();
              this.updateNetworkSeries(this.date);
              this.scaleNetwork();
              this.updateNetworkStatus();
              this.updateDiskSeries();
              this.scaleDisk();
              this.updateDiskStatus();

              if (this.node?.gpuInfo?.length > 0) {
                if (this.gpus.length === 0) {
                  this.initGpuSeries();
                }

                this.updateGpuSeries();
                this.updateGpuStatus();
              }

            }
          }
          break;
        case ChangeType.DELETE:
          const indexDelete = this.nodes.findIndex(
            (value) => value.name === update.identifier
          );
          if (indexDelete >= 0) {
            this.nodes.splice(indexDelete, 1);
          }
          break;
      }
    });
  }

  private initMemorySeries() {
    this.memory.push({
      name: 'Heap',
      series: [],
    });
    this.memory.push({
      name: 'Cache',
      series: [],
    });
    this.memory.push({
      name: 'Swap',
      series: [],
    });
  }

  private initNetworkSeries() {
    this.network.push({
      name: 'Tx',
      series: [],
    });
    this.network.push({
      name: 'Rx',
      series: [],
    });
  }

  private initDiskSeries() {
    this.disk.push({
      name: 'Write',
      series: [],
    });
    this.disk.push({
      name: 'Read',
      series: [],
    });
  }

  private initGpuSeries() {
    this.gpus = [];
    this.node?.gpuInfo?.forEach(gpu => {
      this.gpus.push({
        name: 'Compute',
        series: [],
      });
      this.gpus.push({
        name: 'Memory Usage',
        series: [],
      });
    });

    for (let i = 0; i < ServerDetailsComponent.MAX_ENTRIES; i++) {
      const date = new Date();
      date.setSeconds(date.getSeconds() - i);

      this.gpus.forEach((gpu) => {
        gpu.series.unshift({
          name: date.toString(),
          value: [date,]
        });
      });
    }
  }

  private initTimeSeries() {
    for (let i = 0; i < ServerDetailsComponent.MAX_ENTRIES; i++) {
      const date = new Date();
      date.setSeconds(date.getSeconds() - i);

      this.cpus.unshift({
        name: date.toString(),
        value: [date,],
      });

      this.network[0].series.unshift({
        name: date.toString(),
        value: [date,],
      });
      this.network[1].series.unshift({
        name: date.toString(),
        value: [date,],
      });

      this.memory[0].series.unshift({
        name: date.toString(),
        value: [date,]
      });
      this.memory[1].series.unshift({
        name: date.toString(),
        value: [date,]
      });
      this.memory[2].series.unshift({
        name: date.toString(),
        value: [date,]
      });
      this.memory = [this.memory[0], this.memory[1], this.memory[2]];

      this.disk[0].series.unshift({
        name: date.toString(),
        value: [date,]
      });
      this.disk[1].series.unshift({
        name: date.toString(),
        value: [date,]
      });

      this.gpus.forEach((gpu) => {
        gpu[0].series.unshift({
          name: date.toString(),
          value: [date,]
        });
        gpu[1].series.unshift({
          name: date.toString(),
          value: [date,]
        });
      });
    }
  }

  private updateCpuStatus() {
    if (this.node == undefined) {
      console.error("Cannot update CPU status: Node is not initialised.");
      return;
    }

    if (this.isLive) {
      this.cpuUsage = +(
        this.average(this.node.cpuInfo.utilization) * 100
      ).toFixed(0);

      this.cpus.push({
        name: this.date?.toString(),
        value: [
          this.date,
          (this.average(this.node.cpuInfo.utilization) * 100).toFixed(0),
        ],
      });

      if (this.cpus.length > ServerDetailsComponent.MAX_ENTRIES) {
        this.cpus.shift();
      }
    }

    this.mergeOptionCpu = {
      series: [
        {
          name: 'CPU',
          type: 'line',
          showSymbol: false,
          color: '#5ac8fa',
          data: this.cpus,
        },
      ],
    };
  }

  private updateMemoryStatus() {
    if (this.node == undefined) {
      console.error("Cannot update memory status: Node is not initialised.");
      return;
    }

    if (this.isLive) {

      const heap = this.bytesToGigabyte(this.node.memInfo.memoryTotal - this.node.memInfo.memoryFree).toFixed(2);
      const cache = this.bytesToGigabyte(this.node.memInfo.systemCache).toFixed(2);
      const swap = this.bytesToGigabyte(this.node.memInfo.swapTotal - this.node.memInfo.swapFree).toFixed(2);

      this.memoryTotal = this.bytesToGigabyte(this.node.memInfo.memoryTotal + this.node.memInfo.swapTotal).toFixed(2);
      this.memoryUsed = (+heap + +cache + +swap).toFixed(2);

      this.memory[0].series.push({
        name: this.date?.toString(),
        value: [
          this.date,
          heap
        ],
      });
      this.memory[1].series.push({
        name: this.date?.toString(),
        value: [
          this.date,
          cache,
        ],
      });
      this.memory[2].series.push({
        name: this.date?.toString(),
        value: [
          this.date,
          swap,
        ],
      });
      this.memory = [this.memory[0], this.memory[1], this.memory[2]];
      for (const entry of this.memory) {
        if (entry.series.length > ServerDetailsComponent.MAX_ENTRIES) {
          entry.series.splice(
            0,
            entry.series.length - ServerDetailsComponent.MAX_ENTRIES
          );
        }
      }
      this.memory = [this.memory[0], this.memory[1], this.memory[2]];
    }


    this.mergeOptionMemory = {
      yAxis: [
        {
          max: this.bytesToGigabyte(this.node.memInfo.memoryTotal + this.node.memInfo.swapTotal).toFixed(0)
        }
      ],
      series: [
        {
          name: 'Heap',
          type: 'line',
          stack: 'mem',
          showSymbol: false,
          color: '#007aff',
          data: this.memory[0].series,
        },
        {
          name: 'Cache',
          type: 'line',
          stack: 'mem',
          showSymbol: false,
          color: '#5ac8fa',
          data: this.memory[1].series,
        },
        {
          name: 'Swap',
          type: 'line',
          stack: 'mem',
          showSymbol: false,
          color: '#5856d6',
          data: this.memory[2].series,
        },
      ],
    };
  }

  private updateNetworkSeries(date: Date) {
    if (this.node == undefined) {
      console.error("Cannot update network series: Node is not initialised.");
      return;
    }
    this.rawNetwork.push([
      date,
      [this.node.netInfo.transmitting, this.node.netInfo.receiving],
    ]);
    if (this.rawNetwork.length > ServerDetailsComponent.MAX_ENTRIES) {
      this.rawNetwork.splice(
        0,
        this.rawNetwork.length - ServerDetailsComponent.MAX_ENTRIES
      );
    }
  }

  private updateNetworkStatus() {
    this.mergeOptionNetwork = {
      yAxis: [
        {
          type: 'value',
          axisLabel: {
            formatter: '{value} ' + this.unitNetwork + 'Byte/s',
          },
          splitLine: {
            show: true,
          },
        },
      ],
      series: [
        {
          name: 'Tx',
          type: 'line',
          showSymbol: false,
          color: '#007aff',
          data: this.network[0].series,
        },
        {
          name: 'Rx',
          type: 'line',
          showSymbol: false,
          color: '#5ac8fa',
          data: this.network[1].series,
        },
      ],
    };
  }

  private updateDiskSeries() {
    if (this.node == undefined) {
      console.error("Cannot update disk series: Node is not initialised.");
      return;
    }
    if (this.date == undefined) {
      console.error("Cannot update disk series: Date is not initialised.");
      return;
    }
    this.rawDisk.push([
      this.date,
      [this.node.diskInfo.writing, this.node.diskInfo.reading],
    ]);
    if (this.rawDisk.length > ServerDetailsComponent.MAX_ENTRIES) {
      this.rawDisk.shift();
    }
  }

  private updateDiskStatus() {
    if (this.node == undefined) {
      console.error("Cannot update disk status: Node is not initialised.");
      return;
    }
    this.diskTotal = this.bytesToGigabyte(this.node.diskInfo.used + this.node.diskInfo.free).toFixed(0);
    this.diskFree = this.bytesToGigabyte(this.node.diskInfo.free).toFixed(0);

    this.mergeOptionDisk = {
      yAxis: [
        {
          type: 'value',
          axisLabel: {
            formatter: '{value} ' + this.unitDisk + 'Byte/s',
          },
          splitLine: {
            show: true,
          },
        },
      ],
      series: [
        {
          name: 'Write',
          type: 'line',
          showSymbol: false,
          color: '#007aff',
          data: this.disk[0].series,
        },
        {
          name: 'Read',
          type: 'line',
          showSymbol: false,
          color: '#5ac8fa',
          data: this.disk[1].series,
        },
      ],
    };
  }

  private updateGpuSeries() {
    if (this.node == undefined) {
      console.error("Cannot update GPU series: Node is not initialised.");
      return;
    }
    let counter = 0;
    for (const gpu of this.node.gpuInfo) {
      this.gpus[counter++].series.push({
        name: this.date?.toString(),
        value: [this.date, Number(Math.max(0, Math.min(100, gpu.computeUtilization)))]
      });
      this.gpus[counter++].series.push({
        name: this.date?.toString(),
        value: [this.date, Number(Math.max(0, Math.min(100, (gpu.memoryUsedMegabytes / gpu.memoryTotalMegabytes) * 100))).toFixed(0)]
      });
    }
  }

  private updateGpuStatus() {
    if (this.isLive) {
      if (this.gpus[0].series.length > ServerDetailsComponent.MAX_ENTRIES) {
        this.gpus.forEach(gpu => gpu.series.shift());
      }
    }

    if (this.gpuName.length === 0) {
      this.node?.gpuInfo.forEach(gpu => {
        this.gpuName.push(gpu.name + ' (' + gpu.id + ')');
      });
    }


    for (let counter = 0, i = 0; counter < this.gpus.length; counter += 2, i++) {
      this.mergeOptionGpu[i] = {
        title: [{
          left: '15%',
          top: '1%',
          text: this.gpuName[i],
          textStyle: {
            fontSize: 10
          }
        }],
        series: [
          {
            name: 'Compute',
            type: 'line',
            showSymbol: false,
            color: '#007aff',
            data: this.gpus[counter]?.series,
          },
          {
            name: 'Memory Usage',
            type: 'line',
            showSymbol: false,
            color: '#5ac8fa',
            data: this.gpus[counter + 1]?.series,
          },
        ],
      };
    }
  }

  scaleNetwork() {
    if (this.network.length > ServerDetailsComponent.MAX_ENTRIES) {
      this.network.shift();
    }

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
            name: input[i][0].toString(),
            value: [input[i][0], (input[i][1][n] / div).toFixed(2)],
          });
        } else {
          output[n].series[i] = {
            name: input[i][0].toString(),
            value: [input[i][0], (input[i][1][n] / div).toFixed(2)],
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

  sortNodesByName() {
    this.nodes = this.nodes.sort((a, b) => (a.name > b.name ? 1 : -1));
  }

  ngOnDestroy(): void {
    if (this.subscription) {
      this.subscription.unsubscribe();
      this.subscription = undefined;
    }
  }

  public trackNodeInfo(nodeInfo: NodeInfoExt): string {
    return nodeInfo?.name;
  }

  bytesToGigabyte(bytes: number) {
    return bytes / (1024 * 1024 * 1024);
  }

  trackGpu(_index: number, gpuInfo: GpuInfo): string {
    return gpuInfo?.id;
  }

  uptimeToDateTime(time: number, uptime?: number): string {
    if (uptime) {
      return new Date(time - uptime).toLocaleString();
    } else {
      return '';
    }
  }

  setNode(index: number) {
    this.formatter = this.axisLabelFormatterMinutes;
    this.selectedNodeIndex = index;
    this.onHistoryButtonValueChange('live');
    this.isLive = true;

    // remove series data from old node
    this.gpus = [];
    this.gpuName = [];
    this.mergeOptionGpu = [];
    this.cpus = [];
    this.memory = [];
    this.network = [];
    this.rawNetwork = [];
    this.rawDisk = [];
    this.disk = [];
    this.initMemorySeries();
    this.initNetworkSeries();
    this.initDiskSeries();
    this.initTimeSeries();
  }

  onHistoryButtonValueChange(value: string) {
    this.historyButtonValue = value;
  }

  getHistory(hours: number) {
    this.isLive = false;

    const node: NodeInfoExt = this.nodes[this.selectedNodeIndex ?? 0];
    const to = new Date();
    const from = new Date().setHours(to.getHours() - hours);
    let chunkSpanMillis;

    switch (hours) {
      case 6: {
        // 1 minute chunks
        chunkSpanMillis = 60000;
        this.formatter = this.axisLabelFormatterMinutes;
        break;
      }
      case 24: {
        // 10 minutes chunks
        chunkSpanMillis = 60000 * 10;
        this.formatter = this.axisLabelFormatterMinutes;
        break;
      }
      case 24 * 7: {
        // 30 minutes chunks
        chunkSpanMillis = 60000 * 30;
        this.formatter = this.axisLabelFormatterDays;
        break;
      }
      case 24 * 31: {
        // 60 minutes chunks
        chunkSpanMillis = 60000 * 60;
        this.formatter = this.axisLabelFormatterDays;
        break;
      }
    }

    this.api.getNodeUtilization(node.name, from, to.getTime(), chunkSpanMillis).then(val => {

      this.cpus = [];
      this.memory = [];
      this.network = [];
      this.rawNetwork = [];
      this.rawDisk = [];
      this.disk = [];
      this.initGpuSeries();

      val.map(d => {
        const date = new Date(d.time);

        this.cpus.push({
          name: date.toString(),
          value: [
            date,
            (this.average(d.cpuUtilization) * 100).toFixed(0),
          ],
        });

        const heap = this.bytesToGigabyte(d.memoryInfo.memoryTotal - d.memoryInfo.memoryFree).toFixed(2);
        const cache = this.bytesToGigabyte(d.memoryInfo.systemCache).toFixed(2);
        const swap = this.bytesToGigabyte(d.memoryInfo.swapTotal - d.memoryInfo.swapFree).toFixed(2);

        this.initMemorySeries();
        this.memory[0].series.push({
          name: date.toString(),
          value: [
            date,
            heap
          ],
        });
        this.memory[1].series.push({
          name: date.toString(),
          value: [
            date,
            cache,
          ],
        });
        this.memory[2].series.push({
          name: date.toString(),
          value: [
            date,
            swap,
          ],
        });
        this.memory = [this.memory[0], this.memory[1], this.memory[2]];

        this.initNetworkSeries();
        this.rawNetwork.push([
          date,
          [d.netInfo.transmitting, d.netInfo.receiving],
        ]);

        this.initDiskSeries();
        this.rawDisk.push([
          date,
          [d.diskInfo.writing, d.diskInfo.reading],
        ]);

        if (d.gpuUtilization && d.gpuUtilization.length > 0) {
          let counter = 0;
          for (const gpu of d.gpuUtilization) {
            this.gpus[counter++].series.push({
              name: date.toString(),
              value: [date, Number(Math.max(0, Math.min(100, gpu.computeUtilization))).toFixed(0)]
            });
            this.gpus[counter++].series.push({
              name: date.toString(),
              value: [date, Number(Math.max(0, Math.min(100, (gpu.memoryUsedMegabytes / gpu.memoryTotalMegabytes) * 100))).toFixed(0)]
            });
          }
        }
      });

      this.updateCpuStatus();
      this.updateMemoryStatus();
      this.scaleNetwork();
      this.updateNetworkStatus();
      this.scaleDisk();
      this.updateDiskStatus();
      this.updateGpuStatus();
    });
  }
}
