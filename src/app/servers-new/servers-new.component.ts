import { Component, OnDestroy, OnInit } from "@angular/core";
import { GpuInfo, NodeInfo, NodesApiService } from "../api/nodes-api.service";
import { Subscription } from "rxjs";
import { ChangeType } from "../api/api.service";

@Component({
  selector: "app-servers-new",
  templateUrl: "./servers-new.component.html",
  styleUrls: ["./servers-new.component.css"],
})
export class ServersNewComponent implements OnInit, OnDestroy {

  static readonly MAX_ENTRIES = 120;

  nodes: NodeInfo[] = [];
  node: NodeInfo;
  loadError = null;
  subscription: Subscription = null;

  date: Date;

  unitNetwork = "";
  unitDisk = "";
  unitGpu = "";

  memoryTotal = "";
  memoryFree = "";

  diskTotal = "";
  diskFree = "";

  average = (arr) => arr.reduce((p, c) => p + c, 0) / arr.length;

  constructor(private api: NodesApiService) {
    this.initMemorySeries();
    this.initNetworkSeries();
    this.initDiskSeries();

    for (let i = 0; i < 120; i++) {
      var date = new Date();
      date.setSeconds(date.getSeconds() - i);

      this.cpus.unshift({
        name: date.toString(),
        value: [date, ],
      });

      this.network[0].series.unshift({
        name: date.toString(),
        value: [date, ],
      });
      this.network[1].series.unshift({
        name: date.toString(),
        value: [date, ],
      });

      this.memory[0].series.unshift({
        name: date.toString(),
        value: [date, ]
      });
      this.memory[1].series.unshift({
        name: date.toString(),
        value: [date, ]
      });
      this.memory[2].series.unshift({
        name: date.toString(),
        value: [date, ]
      });
      this.memory = [this.memory[0], this.memory[1], this.memory[2]];

      this.disk[0].series.unshift({
        name: date.toString(),
        value: [date, ]
      });
      this.disk[1].series.unshift({
        name: date.toString(),
        value: [date, ]
      });

      this.gpus.forEach(function (gpu) {
        gpu[0].series.unshift({
          name: date.toString(),
        value: [date, ]
        });
        gpu[1].series.unshift({
          name: date.toString(),
        value: [date, ]
        });
      });
    }
  }

  cpus: any[] = [];
  cpuUsage: number = 0;
  mergeOptionCpu = {};
  chartOptionCpu = {
    calculable: false,
    grid: {
      top: "5%",
      bottom: "25",
      left: "50",
      right: "10"
    },
    xAxis: [
      {
        type: "time",
        splitLine: {
          show: false,
        },
        show: true,
        axisLabel: {
          formatter: function (value) {
            const date = new Date(value);
            if (date.getSeconds() === 0) {
              let zero = (date.getMinutes() < 10 ? ":0" : ":")
              return date.getHours() + zero + date.getMinutes();
            }
          },
        }
      },
    ],
    yAxis: {
      type: "value",
      axisLabel: {
        formatter: "{value} %",
      },
      scale : true,
      max : 100,
      min : 0,
      splitNumber : 5,
      splitLine: {
        show: true,
      },
    },

    series: [],
  };

  memory: any[] = [];
  mergeOptionMemory = {};
  chartOptionMemory = {
    grid: {
      top: "5%",
      bottom: "25",
      left: "50",
      right: "10"
    },
    calculable: false,
    xAxis: [
      {
        show: true,
        type: "time",
        axisLabel: {
          formatter: function (value) {
            const date = new Date(value);
            if (date.getSeconds() === 0) {
              let zero = (date.getMinutes() < 10 ? ":0" : ":")
              return date.getHours() + zero + date.getMinutes();
            }
          },
        },
        splitLine: {
          show: false,
        },
      },
    ],
    yAxis: [
      {
        type: "value",
        min: 0,
        max: 32,
        axisLabel: {
          formatter: "{value} GiB",
        },
        // scale : true,
        // max : 32,
        //min : 0,
        splitNumber : 4,
        splitLine: {
          show: true,
        },
      },
    ],
    series: [],
  };

  rawNetwork: [Date, number[]][] = [];
  network: any[] = [];
  mergeOptionNetwork = {};
  chartOptionNetwork = {
    grid: {
      top: "5%",
      bottom: "25",
      left: "90",
      right: "10"
    },
    calculable: false,
    xAxis: [
      {
        show: true,
        type: "time",
        axisLabel: {
          formatter: function (value) {
            const date = new Date(value);
            if (date.getSeconds() === 0) {
              let zero = (date.getMinutes() < 10 ? ":0" : ":")
              return date.getHours() + zero + date.getMinutes();
            }
          },
        },
        splitLine: {
          show: false,
        },
        splitNumber : 4,
        // boundaryGap : [ 0.2, 0.2 ]
      },
    ],
    yAxis: [
      {
        type: "value",
        // min: 0,
        // max: 32,
        axisLabel: {
          formatter: "{value}GB",
          // showMaxLabel: true,
          // interval: 6
        },
        scale : true,
        // max : 1024,
        min : 0,
        splitNumber : 6,
        splitLine: {
          show: true,
        },
      },
    ],
    series: [],
  };

  rawDisk: [Date, number[]][] = [];
  disk: any[] = [];
  mergeOptionDisk = {};
  chartOptionDisk = {
    grid: {
      top: "5%",
      bottom: "25",
      left: "90",
      right: "10"
    },
    calculable: false,
    xAxis: [
      {
        show: true,
        type: "time",
        axisLabel: {
          formatter: function (value) {
            const date = new Date(value);
            if (date.getSeconds() === 0) {
              let zero = (date.getMinutes() < 10 ? ":0" : ":")
              return date.getHours() + zero + date.getMinutes();
            }
          },
        },
        splitLine: {
          show: false,
        },
        splitNumber : 4,
        // boundaryGap : [ 0.2, 0.2 ]
      },
    ],
    yAxis: [
      {
        type: "value",
        // min: 0,
        // max: 32,
        axisLabel: {
          formatter: "{value}GB",
          // showMaxLabel: true,
          // interval: 6
        },
        scale : true,
        // max : 1024,
        min : 0,
        splitNumber : 6,
        splitLine: {
          show: true,
        },
      },
    ],
    series: [],
  };

  gpus: any[] = [];
  mergeOptionGpu: any[]  = [];
  chartOptionGpu = {
    calculable: false,
    grid: {
      top: "10%",
      bottom: "25",
      left: "50",
      right: "10"
    },
    xAxis: [
      {
        type: "time",
        splitLine: {
          show: false,
        },
        show: true,
        axisLabel: {
          formatter: function (value) {
            const date = new Date(value);
            if (date.getSeconds() === 0) {
              let zero = (date.getMinutes() < 10 ? ":0" : ":")
              return date.getHours() + zero + date.getMinutes();
            }
          },
        }
      },
    ],
    yAxis: {
      type: "value",
      axisLabel: {
        formatter: "{value} %",
      },
      scale : true,
      max : 100,
      min : 0,
      splitNumber : 5,
      splitLine: {
        show: true,
      },
    },

    series: [],
  };

  ngOnInit() {
    this.subscription = this.api.watchNodes((update) => {
      switch (update.type) {
        case ChangeType.CREATE:
        case ChangeType.UPDATE:
          if (update.value != null) {
            const indexUpdate = this.nodes.findIndex(
              (value) => value.name === update.identifier
            );
            if (indexUpdate >= 0) {
              if (this.nodes[indexUpdate]?.update != null) {
                this.nodes[indexUpdate]?.update(update.value);
              }
            } else {
              this.nodes.push(update.value);
              this.sortNodesByName();
            }

            // TODO -> get selected node information

            this.node = this.nodes[0];
            console.log(this.node)
            console.log(this.node?.allocInfo)

            this.date = new Date();
            this.updateCpuStatus();
            this.updateMemoryStatus();
            this.updateNetworkSeries(this.date)
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
            };
            this.updateGpuStatus();
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
      name: "Heap",
      series: [],
    });
    this.memory.push({
      name: "Cache",
      series: [],
    });
    this.memory.push({
      name: "Swap",
      series: [],
    });
  }

  private initNetworkSeries() {
    this.network.push({
      name: "Tx",
      series: [],
    });
    this.network.push({
      name: "Rx",
      series: [],
    });
  }

  private initDiskSeries() {
    this.disk.push({
      name: "Write",
      series: [],
    });
    this.disk.push({
      name: "Read",
      series: [],
    });
  }

  private initGpuSeries() {
    this.gpus = [];
    let counter = 0;
    this.node?.gpuInfo?.forEach(gpu => {
      this.gpus.push({
        name: "Compute",
        series: [],
      });
      this.gpus.push({
        name: "Memory Bus",
        series: [],
      });
    });

    for (let i = 0; i < 120; i++) {
      var date = new Date();
      date.setSeconds(date.getSeconds() - i);

      this.gpus.forEach(function (gpu) {
        gpu.series.unshift({
          name: date.toString(),
          value: [date, ]
        });
      });
    }
  }

  private updateCpuStatus() {
    this.cpuUsage = +(
      this.average(this.node.cpuInfo.utilization) * 100
    ).toFixed(0);

    this.cpus.push({
      name: this.date.toString(),
      value: [
        this.date,
        (this.average(this.node.cpuInfo.utilization) * 100).toFixed(0),
      ],
    });

    if (this.cpus.length > 120) {
      this.cpus.shift();
    }

    this.mergeOptionCpu = {
      tooltip: {
        trigger: 'axis',
        axisPointer: {
            type: 'shadow'
        },
        formatter: (params) => {
          return ` ${params[0].seriesName}: ${params[0].value[1] + "%"} `;
        },
      },
      series: [
        {
          name: "CPU",
          type: "line",
          hoverAnimation: false,
          showSymbol: false,
          color: "#5ac8fa",
          itemStyle: { normal: { areaStyle: { type: "default" } } },
          data: this.cpus,
        },
      ],
    };
  }

  private updateMemoryStatus() {

    this.memoryTotal = this.bytesToGigabyte(this.node.memInfo.memoryTotal).toFixed(2);
    this.memoryFree = this.bytesToGigabyte(this.node.memInfo.memoryFree).toFixed(2);

    this.memory[0].series.push({
      name: this.date.toString(),
      value: [
        this.date,
        this.bytesToGigabyte(
          this.node.memInfo.memoryTotal - this.node.memInfo.memoryFree - this.node.memInfo.systemCache - (this.node.memInfo.swapTotal - this.node.memInfo.swapFree)
        ).toFixed(2),
      ],
    });
    this.memory[1].series.push({
      name: this.date.toString(),
      value: [
        this.date,
        this.bytesToGigabyte(this.node.memInfo.systemCache).toFixed(2),
      ],
    });
    this.memory[2].series.push({
      name: this.date.toString(),
      value: [
        this.date,
        this.bytesToGigabyte(
          this.node.memInfo.swapTotal - this.node.memInfo.swapFree
        ).toFixed(2),
      ],
    });
    this.memory = [this.memory[0], this.memory[1], this.memory[2]];
    for (const entry of this.memory) {
      if (entry.series.length > ServersNewComponent.MAX_ENTRIES) {
        entry.series.splice(
          0,
          entry.series.length - ServersNewComponent.MAX_ENTRIES
        );
      }
    }
    this.memory = [this.memory[0], this.memory[1], this.memory[2]];

    if (this.memory.length > 120) {
      this.memory.shift();
    }

    this.mergeOptionMemory = {
      tooltip: {
        trigger: 'axis',
        axisPointer: {
            type: 'shadow'
        },
        formatter: (params) => {
          return `
                    Memory Usage: <br />
                    ${params[0].seriesName}: ${(params[0].value[1] ? params[0].value[1] : 0) + " GiB"}<br />
                    ${params[1].seriesName}: ${(params[1].value[1] ? params[1].value[1] : 0) + " GiB"}<br />
                    ${params[2].seriesName}: ${(params[2].value[1] ? params[2].value[1] : 0) + " GiB"}
                    `;
        },
      },
      yAxis: [
        {
          max: this.bytesToGigabyte(this.node.memInfo.memoryTotal)
        }
      ],
      series: [
        {
          name: "Heap",
          type: "line",
          stack: 'mem',
          hoverAnimation: false,
          showSymbol: false,
          color: "#007aff",
          itemStyle: { normal: { areaStyle: { type: "default" } } },
          data: this.memory[0]["series"],
        },
        {
          name: "Cache",
          type: "line",
          stack: 'mem',
          hoverAnimation: false,
          showSymbol: false,
          color: "#5ac8fa",
          itemStyle: { normal: { areaStyle: { type: "default" } } },
          data: this.memory[1]["series"],
        },
        {
          name: "Swap",
          type: "line",
          stack: 'mem',
          hoverAnimation: false,
          showSymbol: false,
          color: "#5856d6",
          itemStyle: { normal: { areaStyle: { type: "default" } } },
          data: this.memory[2]["series"],
        },
      ],
    };
  }

  private updateNetworkSeries(date: Date = null) {
    this.rawNetwork.push([
      date,
      [this.node.netInfo.transmitting, this.node.netInfo.receiving],
    ]);
    if (this.rawNetwork.length > ServersNewComponent.MAX_ENTRIES) {
      this.rawNetwork.splice(
        0,
        this.rawNetwork.length - ServersNewComponent.MAX_ENTRIES
      );
    }
  }

  private updateNetworkStatus() {
    this.mergeOptionNetwork = {
      tooltip: {
        trigger: 'axis',
        axisPointer: {
            type: 'shadow'
        },
        formatter: (params) => {
          return `
                    Network IO: <br />
                    ${params[0].seriesName}: ${params[0].value[1] + " " + this.unitNetwork + "Byte/s"}<br />
                    ${params[1].seriesName}: ${params[1].value[1] + " " + this.unitNetwork + "Byte/s"}
                    `;
        },
      },
      yAxis: [
        {
          type: "value",
          // min: 0,
          // max: 32,
          axisLabel: {
            formatter: "{value} " + this.unitNetwork + "Byte/s",
            // showMaxLabel: true,
            // interval: 8
          },
          splitLine: {
            show: true,
          },
        },
      ],
      series: [
        {
          name: "Tx",
          type: "line",
          hoverAnimation: false,
          showSymbol: false,
          color: "#007aff",
          itemStyle: { normal: { areaStyle: { type: "default" } } },
          data: this.network[0]["series"],
        },
        {
          name: "Rx",
          type: "line",
          hoverAnimation: false,
          showSymbol: false,
          color: "#5ac8fa",
          itemStyle: { normal: { areaStyle: { type: "default" } } },
          data: this.network[1]["series"],
        },
      ],
    };
  }

  private updateDiskSeries() {
    this.rawDisk.push([
      this.date,
      [this.node.diskInfo.writing, this.node.diskInfo.reading],
    ]);
    if (this.rawDisk.length > ServersNewComponent.MAX_ENTRIES) {
      this.rawDisk.shift();
    }
  }

  private updateDiskStatus() {
    this.diskTotal = this.bytesToGigabyte(this.node.diskInfo.used).toFixed(0);
    this.diskFree = this.bytesToGigabyte(this.node.diskInfo.free).toFixed(0);

    this.mergeOptionDisk = {
      tooltip: {
        trigger: 'axis',
        axisPointer: {
            type: 'shadow'
        },
        formatter: (params) => {
          return `
                    Disk IO: <br />
                    ${params[0].seriesName}: ${params[0].value[1] + " " + this.unitDisk + "Byte/s"}<br />
                    ${params[1].seriesName}: ${params[1].value[1] + " " + this.unitDisk + "Byte/s"}
                    `;
        },
      },
      yAxis: [
        {
          type: "value",
          // min: 0,
          // max: 32,
          axisLabel: {
            formatter: "{value} " + this.unitDisk + "Byte/s",
            // showMaxLabel: true,
            // interval: 8
          },
          splitLine: {
            show: true,
          },
        },
      ],
      series: [
        {
          name: "Write",
          type: "line",
          hoverAnimation: false,
          showSymbol: false,
          color: "#007aff",
          itemStyle: { normal: { areaStyle: { type: "default" } } },
          data: this.disk[0]["series"],
        },
        {
          name: "Read",
          type: "line",
          hoverAnimation: false,
          showSymbol: false,
          color: "#5ac8fa",
          itemStyle: { normal: { areaStyle: { type: "default" } } },
          data: this.disk[1]["series"],
        },
      ],
    };
  }

  private updateGpuSeries() {
    let counter = 0;
    for (const gpu of this.node.gpuInfo) {
      this.gpus[counter++].series.push({
        name: this.date.toString(),
        value: [this.date, Number(Math.max(0, Math.min(100, gpu.computeUtilization)))]
      });
      this.gpus[counter++].series.push({
        name: this.date.toString(),
        value: [this.date, Number(Math.max(0, Math.min(100, gpu.memoryUtilization)))]
      });
    }
  }

  private updateGpuStatus() {

    if (this.gpus[0].series.length > 120) {
      this.gpus.forEach(gpu => gpu.series.shift())
    }

    let counter = 0;
    this.gpus.forEach(gpu => {
      this.mergeOptionGpu[counter++] = {
        title: [{
          left: '15%',
          top: '1%',
          text: 'GPU Utilization GeForce GTX 1080 Ti (nvidia-0)',
          textStyle: {
            fontSize: 10
          }
      }],
        tooltip: {
          trigger: 'axis',
          axisPointer: {
              type: 'shadow'
          },
          formatter: (params) => {
            return ` ${params[0].seriesName}: ${params[0].value[1] + "%"}<br>
                     ${params[1].seriesName}: ${params[1].value[1] + "%"} `;
          },
        },
        series: [
          {
            name: "Compute",
            type: "line",
            hoverAnimation: false,
            showSymbol: false,
            color: "#007aff",
            itemStyle: { normal: { areaStyle: { type: "default" } } },
            data: this.gpus[counter-1]?.series,
          },
          {
            name: "Memory Bus",
            type: "line",
            hoverAnimation: false,
            showSymbol: false,
            color: "#5ac8fa",
            itemStyle: { normal: { areaStyle: { type: "default" } } },
            data: this.gpus[counter]?.series,
          },
        ],
      };
    })
  }

  scaleNetwork() {
    if (this.network.length > 120) {
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
        return "";
      case 1:
        return "Ki";
      case 2:
        return "Mi";
      case 3:
        return "Gi";
      case 4:
        return "Ti";
      case 5:
        return "Pi";
    }
  }

  sortNodesByName() {
    this.nodes = this.nodes.sort((a, b) => (a.name > b.name ? 1 : -1));
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

  bytesToGigabyte(bytes: number) {
    return bytes / (1024 * 1024 * 1024);
  }

  trackGpu(gpuInfo: GpuInfo): string {
    return gpuInfo?.id;
  }

  uptimeToDateTime(time: number, uptime?: number): string {
    if (uptime) {
      return new Date(time - uptime).toLocaleString();
    } else {
      return '';
    }
  }
}
