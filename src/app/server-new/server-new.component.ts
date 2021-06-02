import { Component, Input, OnInit } from "@angular/core";
import { GpuInfo, NodeInfo, NodesApiService } from "../api/nodes-api.service";
import { EChartOption } from "echarts";

@Component({
  selector: "app-server-new",
  templateUrl: "./server-new.component.html",
  styleUrls: ["./server-new.component.css"],
})
export class ServerNewComponent implements OnInit {
  constructor(private nodes: NodesApiService) {}

  static readonly MAX_ENTRIES = 120;

  @Input("node") node: NodeInfo;

  // ##############################################
  // ##############################################
  // ##############################################


  mergeOptionCpu = {};
  chartOptionCpu = {
    title: {
      text: "CPU Usage",
      x: 'center',
      y: "1%"
    },
    series: [{
        type: 'gauge',
        axisLine: {
            lineStyle: {
                width: 20,
                color: [
                    [0.3, '#5cb85c'],
                    [0.7, '#f0ad4e'],
                    [1, '#d9534f']
                ]
            }
        },
        pointer: {
            itemStyle: {
                color: 'auto'
            }
        },
        axisTick: {
            distance: -30,
            length: 8,
            lineStyle: {
                color: '#fff',
                width: 3
            }
        },
        splitLine: {
            distance: -30,
            length: 30,
            lineStyle: {
                color: '#fff',
                width: 1
            }
        },
        axisLabel: {
            color: 'auto',
            distance: 40,
            fontSize: 10,
            fontStyle: "bold"
        },
        detail: {
            valueAnimation: true,
            formatter: '{value}%',
            color: 'auto'
        },
        data: [{
          value: 0
        }]
    }]
};

  diskUsage: any[] = [];
  mergeOptionDiskUsage = {};
  chartOptionDiskUsage = {
    title: {
      text: "Disk Usage",
      x: 'center',
      y: "6%"
    },
    tooltip: {
      trigger: "item",
      formatter: "{a} <br/>{b} : {c}GiB ({d}%)",
    },
    series: [
      {
        name: "Disk Usage",
        type: "pie",
        radius: "50%",
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: "rgba(0, 0, 0, 0.5)",
          },
        },
      },
    ],
  };

  mergeOptionMemory = {};
  chartOptionMemory = {
    title: {
      text: "Memory usage",
      x: "left",
      y: "3%",
    },
    tooltip: {
      trigger: "axis",
      axisPointer: {
        type: "cross",
        label: {
          backgroundColor: "#6a7985",
        },
      },
    },
    legend: {
      orient: "vertical",
      x: "right",
      data: ["Heap", "Cache", "Swap"],
    },
    calculable: false,
    xAxis: [
      {
        type: "time",
        axisLabel: {
          formatter: (function(value){
            let now = new Date(value);
            let zero = (now.getMinutes() < 10 ? ":0" : ":")
            return now.getHours() + zero + now.getMinutes();
          })
        },
        splitLine: {
          show: false,
        },
      },
    ],
    yAxis: [
      {
        type: "value",
        /*     min: 0,
        max: 100,
        axisLabel : {
            formatter: '{value} %'
        } */
      },
    ],
    series: [],
  };

  mergeOptionNetwork = {};
  chartOptionNetwork = {
    title: {
      text: "Network IO",
      x: "left%",
      y: "3%",
    },
    tooltip: {
      trigger: "axis",
      axisPointer: {
        type: "cross",
        label: {
          backgroundColor: "#6a7985",
        },
      },
    },
    legend: {
      orient: "vertical",
      right: 10,
      data: ["Tx", "Rx"],
    },
    calculable: false,
    xAxis: [
      {
        type: "time",
        axisLabel: {
          formatter: (function(value){
            let now = new Date(value);
            let zero = (now.getMinutes() < 10 ? ":0" : ":")
            return now.getHours() + zero + now.getMinutes();
          })
        },
        splitLine: {
          show: false,
        },
      },
    ],
    yAxis: [
      {
        type: "value",
        /*     min: 0,
        max: 100,
        axisLabel : {
            formatter: '{value} %'
        } */
      },
    ],
    series: [],
  };

  mergeOptionDisk = {};
  chartOptionDisk = {
    title: {
      text: "Disk IO",
      x: "left",
      y: "3%",
    },
    tooltip: {
      trigger: "axis",
      axisPointer: {
        type: "cross",
        label: {
          backgroundColor: "#6a7985",
        },
      },
    },
    legend: {
      orient: "vertical",
      right: 10,
      data: ["Write", "Read"],
    },
    calculable: false,
    xAxis: {
      type: "time",
      axisLabel: {
        formatter: (function(value){
          let now = new Date(value);
          let zero = (now.getMinutes() < 10 ? ":0" : ":")
          return now.getHours() + zero + now.getMinutes();
        })
      },
      splitLine: {
        show: false,
      },
    },
    yAxis: {
      type: "value",
      boundaryGap: [0, "100%"],
      splitLine: {
        show: false,
      },
    },
    series: [],
  };

  mergeOptionGpu = {};
  chartOptionGpu = {
    /*     title: {
    text: 'GPU',
    x: '50%',
    y: '3%'
  }, */
    tooltip: {
      trigger: "axis",
      axisPointer: {
        type: "cross",
        label: {
          backgroundColor: "#6a7985",
        },
      },
    },
    legend: {
      orient: "vertical",
      right: 10,
      data: ["Compute", "Memory Bus"],
    },
    calculable: false,
    xAxis: [
      {
        type: "time",
        axisLabel: {
          formatter: (function(value){
            let now = new Date(value);
            let zero = (now.getMinutes() < 10 ? ":0" : ":")
            return now.getHours() + zero + now.getMinutes();
          })
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
        max: 100,
        axisLabel: {
          formatter: "{value} %",
        },
      },
    ],
    series: [],
  };



  mergeOptionGpuHistory = {};
  chartOptionGpuHistory = {
    /*     title: {
    text: 'GPU',
    x: '50%',
    y: '3%'
  }, */
    tooltip: {
      trigger: "axis",
      axisPointer: {
        type: "cross",
        label: {
          backgroundColor: "#6a7985",
        },
      },
    },
    legend: {
      orient: "vertical",
      right: 10,
      data: ["Compute", "Memory Bus"],
    },
    calculable: false,
    xAxis: [
      {
        type: "time",
        axisLabel: {
          formatter: (function(value){
            let now = new Date(value);
            let zero = (now.getMinutes() < 10 ? ":0" : ":")
            return now.getHours() + zero + now.getMinutes();
          })
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
        max: 100,
        axisLabel: {
          formatter: "{value} %",
        },
      },
    ],
    series: [],
  };

  // ##############################################
  // ##############################################
  // ##############################################

  memory: any[] = [];
  network: any[] = [];
  disk: any[] = [];
  //diskUsage: any[] = [];
  cpus: any[] = [];
  gpus: any[] = [];

  rawNetwork: [Date, number[]][] = [];
  rawDisk: [Date, number[]][] = [];
  rawGpu: [Date, number[]][] = [];

  unitNetwork = "";
  unitDisk = "";
  unitGpu = "";

  colorSchemeDiskUsageW95 = {
    domain: ["#ff00ff", "#0000ff"],
  };

  colorScheme = {
    //    domain: ['#FF6666', '#66FF66', '#6666FF', '#777777']
    domain: [
      "#FF0000",
      "#FF7F00",
      "#FFFF00",
      "#00FF00",
      "#0000FF",
      "#0000FF",
      "#4B0082",
      "#9400D3",
    ],
  };

  schemeMemory = {
    domain: [
      "#00FF00", // heap
      "#F0FF00", // system cache
      "#FF0000", // swap
    ],
  };

  schemeWriteRead = {
    domain: ["#FF0000", "#00FF00"],
  };

  schemeGpuComputeMemory = {
    //    domain: ['#FF6666', '#66FF66', '#6666FF', '#777777']
    domain: ["#FF0000", "#00FF00"],
  };

  private static orNow(date?: Date): Date {
    if (date == null) {
      return new Date();
    } else {
      return date;
    }
  }

  diskUsageLabelFormatting = (value) => value + " GiB";
  diskUsageToolTipFormatting = (value) => value.value.toFixed(1) + " GiB";

  bytesToGigabyte(bytes: number) {
    return bytes / (1024 * 1024 * 1024);
  }

  totalMemoryAllPools() {
    if (this.node.memInfo) {
      return this.bytesToGigabyte(
        this.node.memInfo.memoryTotal + this.node.memInfo.swapTotal
      );
    } else {
      return 0;
    }
  }

  ngOnInit() {
    const backupNode = this.node;
    this.node = new NodeInfo(
      this.node.name,
      this.node.time,
      this.node.uptime,
      this.node.cpuInfo,
      this.node.memInfo,
      this.node.gpuInfo,
      this.node.buildInfo
    );

    const date = new Date();
    for (let i = ServerNewComponent.MAX_ENTRIES; i >= 0; --i) {
      this.update(new Date(date.getTime() - i * 1000));
    }

    this.node = backupNode;
    this.node.update = (node) => {
      // load all the new goodies without replacing the object
      if (node != null && node.time !== this.node.time) {
        Object.keys(node).forEach((key) => {
          this.node[key] = node[key];
        });
        this.update();
      }
    };
    this.update(date);

    let gpu_history: any[] = [];

    gpu_history.push({
      name: "Compute",
      series: [],
    });
    gpu_history.push({
      name: "Memory Bus",
      series: [],
    });

    let minutes = 120;
    let to = new Date().getTime();
    let from = to - (minutes * 60 * 1000);

    console.log(from, to)

    this.nodes.getNodeUtilization(this.node.name, from, to).then(val => {
      let date = new Date(val[0]['time'])
      console.log(val)
      console.log(val[0]['gpuComputeUtilization'])
      console.log(val[0]['gpuMemoryUtilization'])
      console.log(date)

      for (const v of val) {
        date = new Date(v.time)

        gpu_history[0].series.push({
          name: date,
          value: [
            date,
            Number(Math.max(0, Math.min(100, v.gpuComputeUtilization[0]))),
          ]
        });

        gpu_history[1].series.push({
          name: date,
          value: [
            date,
            Number(Math.max(0, Math.min(100, v.gpuMemoryUtilization[0]))),
          ]
        })
      }

      console.log(gpu_history)



      this.mergeOptionGpuHistory = {
        series: [
          {
            name: "Compute",
            type: "line",
            hoverAnimation: false,
            showSymbol: false,
            color: "#FF0000",
            itemStyle: { normal: { areaStyle: { type: "default" } } },
            data: gpu_history[0]["series"],
          },
          {
            name: "Memory Bus",
            type: "line",
            hoverAnimation: false,
            showSymbol: false,
            color: "#00FF00",
            itemStyle: { normal: { areaStyle: { type: "default" } } },
            data: gpu_history[1]["series"],
          },
        ],
      };
    })
  }

  update(date: Date = null) {
    date = ServerNewComponent.orNow(date);

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

    if (this.node?.gpuInfo?.length > 0) {
      if (this.gpus.length === 0) {
        this.initGpuSeries();
      }
      this.updateGpuSeries(date);
    }
  }

  private updateGpuSeries(date: Date = null) {
    date = ServerNewComponent.orNow(date);
    let counter = 0;
    for (const gpu of this.node.gpuInfo) {
      this.gpus[counter++].series.push({
        name: date.toString(),
        value: [
          date,
          Number(Math.max(0, Math.min(100, gpu.computeUtilization))),
        ],
      });
      this.gpus[counter++].series.push({
        name: date.toString(),
        value: [
          date,
          Number(Math.max(0, Math.min(100, gpu.memoryUtilization))),
        ],
      });

      for (const entry of this.gpus) {
        if (entry.series.length > ServerNewComponent.MAX_ENTRIES) {
          entry.series.shift();
        }
      }

      console.log(this.gpus)

      this.mergeOptionGpu = {
        series: [
          {
            name: "Compute",
            type: "line",
            hoverAnimation: false,
            showSymbol: false,
            color: "#FF0000",
            itemStyle: { normal: { areaStyle: { type: "default" } } },
            data: this.gpus[0]["series"],
          },
          {
            name: "Memory Bus",
            type: "line",
            hoverAnimation: false,
            showSymbol: false,
            color: "#00FF00",
            itemStyle: { normal: { areaStyle: { type: "default" } } },
            data: this.gpus[1]["series"],
          },
        ],
      };
    }

    for (const entry of this.gpus) {
      if (entry.series.length > ServerNewComponent.MAX_ENTRIES) {
        entry.series.splice(
          0,
          entry.series.length - ServerNewComponent.MAX_ENTRIES
        );
      }
    }

    // this.gpus = [...this.gpus];
  }

  private initGpuSeries() {
    this.gpus = [];
    for (const gpu of this.node.gpuInfo) {
      this.gpus.push({
        name: "Compute",
        series: [],
      });
      this.gpus.push({
        name: "Memory Bus",
        series: [],
      });
    }
  }

  private updateCpuSeries() {
    const cpus = this.node.cpuInfo.utilization;
    const cpusReplacement = [];

    for (let i = 0; i < cpus.length; ++i) {
      const value = Number(Math.max(0.1, Math.min(100, cpus[i] * 100)));
      cpusReplacement.push({
        name: i,
        value,
      });
      if (Number.isNaN(value)) {
        return; // nope out of it
      }
    }
    if (cpusReplacement.length === 0) {
      return;
    }
    this.cpus = cpusReplacement;

    let cpuUsage = ((((cpus.reduce(function(a, b) { return a + b; }, 0))) * 100)/8).toFixed(0);


    this.mergeOptionCpu = {
      series: {
        data: [{
          value: Number(cpuUsage)
        }]
      }
    }

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

  private updateMemorySeries(date: Date = null) {
    date = ServerNewComponent.orNow(date);
    this.memory[0].series.push({
      name: date.toString(),
      value: [
        date,
        this.bytesToGigabyte(
          this.node.memInfo.memoryTotal - this.node.memInfo.memoryFree
        ),
      ],
    });
    this.memory[1].series.push({
      name: date.toString(),
      value: [date, this.bytesToGigabyte(this.node.memInfo.systemCache)],
    });
    this.memory[2].series.push({
      name: date.toString(),
      value: [
        date,
        this.bytesToGigabyte(
          this.node.memInfo.swapTotal - this.node.memInfo.swapFree
        ),
      ],
    });
    this.memory = [this.memory[0], this.memory[1], this.memory[2]];
    for (const entry of this.memory) {
      if (entry.series.length > ServerNewComponent.MAX_ENTRIES) {
        entry.series.splice(
          0,
          entry.series.length - ServerNewComponent.MAX_ENTRIES
        );
      }
    }
    this.memory = [this.memory[0], this.memory[1], this.memory[2]];


    this.mergeOptionMemory = {
      series: [
        {
          name: "Heap",
          type: "line",
          hoverAnimation: false,
          showSymbol: false,
          color: "#00FF00",
          itemStyle: { normal: { areaStyle: { type: "default" } } },
          data: this.memory[0]["series"],
        },
        {
          name: "Cache",
          type: "line",
          hoverAnimation: false,
          showSymbol: false,
          color: "#F0FF00",
          itemStyle: { normal: { areaStyle: { type: "default" } } },
          data: this.memory[1]["series"],
        },
        {
          name: "Swap",
          type: "line",
          hoverAnimation: false,
          showSymbol: false,
          color: "#FF0000",
          itemStyle: { normal: { areaStyle: { type: "default" } } },
          data: this.memory[2]["series"],
        },
      ],
    };
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

  private updateNetworkSeries(date: Date = null) {
        this.rawNetwork.push([
          date = ServerNewComponent.orNow(date),
      [this.node.netInfo.transmitting, this.node.netInfo.receiving],
    ]);
    if (this.rawNetwork.length > ServerNewComponent.MAX_ENTRIES) {
      this.rawNetwork.splice(
        0,
        this.rawNetwork.length - ServerNewComponent.MAX_ENTRIES
      );
    }

    this.mergeOptionNetwork = {
      series: [
        {
          name: "Tx",
          type: "line",
          hoverAnimation: false,
          showSymbol: false,
          color: "#FF0000",
          itemStyle: { normal: { areaStyle: { type: "default" } } },
          data: this.network[0]["series"],
        },
        {
          name: "Rx",
          type: "line",
          hoverAnimation: false,
          showSymbol: false,
          color: "#00FF00",
          itemStyle: { normal: { areaStyle: { type: "default" } } },
          data: this.network[1]["series"],
        },
      ],
    };

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

  private updateDiskSeries(date: Date = null) {
    this.rawDisk.push([
      ServerNewComponent.orNow(date),
      [this.node.diskInfo.writing, this.node.diskInfo.reading],
    ]);
    if (this.rawDisk.length > ServerNewComponent.MAX_ENTRIES) {
      this.rawDisk.shift();
    }

    this.mergeOptionDisk = {
      series: [
        {
          name: "Write",
          type: "line",
          hoverAnimation: false,
          showSymbol: false,
          color: "#FF0000",
          itemStyle: { normal: { areaStyle: { type: "default" } } },
          data: this.disk[0]["series"],
        },
        {
          name: "Read",
          type: "line",
          hoverAnimation: false,
          showSymbol: false,
          color: "#00FF00",
          itemStyle: { normal: { areaStyle: { type: "default" } } },
          data: this.disk[1]["series"],
        },
      ],
    };
  }

  private initDiskUsageSeries() {
    this.diskUsage.push({
      name: "Free",
      value: 1,
    });
    this.diskUsage.push({
      name: "Used",
      value: 1,
    });
  }

  private updateDiskUsageSeries() {
    this.diskUsage[0].value = this.bytesToGigabyte(this.node.diskInfo.free);
    this.diskUsage[1].value = this.bytesToGigabyte(this.node.diskInfo.used);
    this.diskUsage = this.diskUsage.map((u) => u);

    this.mergeOptionDiskUsage = {
      series: [
        {
          data: [
            { value: this.diskUsage[0].value.toFixed(2), name: "Free" },
            { value: this.diskUsage[1].value.toFixed(2), name: "Used" },
          ],
        },
      ],
    };
  }

  getTotalMemoryFormatted() {
    let gigabytes = 0;
    if (this.node !== null) {
      gigabytes = this.bytesToGigabyte(this.node.memInfo.memoryTotal);
    }
    return gigabytes.toFixed(1) + " GiB";
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
            name: input[i][0].toString(),
            value: [input[i][0], input[i][1][n] / div],
          });
        } else {
          output[n].series[i] = {
            name: input[i][0].toString(),
            value: [input[i][0], input[i][1][n] / div],
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

  trackGpu(gpuInfo: GpuInfo): string {
    return gpuInfo?.id;
  }

  uptimeToDateTime(time: number, uptime?: number): string {
    if (uptime) {
      return new Date(time - uptime).toLocaleString();
    } else {
      return "";
    }
  }
}
