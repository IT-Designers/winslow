import { Component, Input, OnInit } from "@angular/core";
import { GpuInfo, NodeInfo, NodesApiService } from "../api/nodes-api.service";

@Component({
  selector: "app-server-bar",
  templateUrl: "./server-bar.component.html",
  styleUrls: ["./server-bar.component.css"],
})
export class ServerBarComponent implements OnInit {

  @Input("node") node: NodeInfo;

  constructor(private nodes: NodesApiService) {}

  static readonly MAX_ENTRIES = 120;

  mergeOptionCpu = {};
  chartOptionCpu = {

    series: [
      {
        name: "CPU",
        type: "gauge",
        radius: 27,
        startAngle: 90,
        endAngle: -270,
        pointer: {
          show: false,
        },
        progress: {
          show: true,
          overlap: false,
          roundCap: true,
          clip: false,
          itemStyle: {
            color: "#69B34C"
          },
        },
        axisLine: {
          lineStyle: {
            width: 5,

          },
        },
        splitLine: {
          show: false,
          distance: 0,
          length: 10,
        },
        axisTick: {
          show: false,
        },
        axisLabel: {
          show: false,
          distance: 50,
        },
        data: [
          {
            value: 27,
            detail: {
              offsetCenter: ["0%", "0%"],
            },
          },
        ],
        detail: {
          width: 50,
          height: 14,
          fontSize: 15,
          fontWeight: 'normal',
          color: "black",
          formatter: "{value}%",
        },
      },
    ],
  };

  mergeOptionMemory = {};
  chartOptionMemory = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
          type: 'shadow'
      },
    },
    // legend: {
    //   data: ["Heap", "Cache", "Swap"],
    //   itemWidth: 10
    // },
    grid: {
      top: '25%',
      bottom: '25%'
    },
    xAxis: {
      type: "value",
      max: 32,
      show: false,
    },
    yAxis: {
      type: "category",
      show: false,
      data: ["Memory Usage in GB"],
    },
    series: [
      {
        name: "Heap",
        type: "bar",
        showBackground: true,
        stack: "total",
        barMaxWidth: 20,
        emphasis: {
          focus: "series",
        },
        itemStyle: {
          color: "#007aff",
          barBorderRadius: [3, 0, 0, 3]
        },
        data: [12],
      },
      {
        name: "Cache",
        type: "bar",
        stack: "total",
        emphasis: {
          focus: "series",
        },
        itemStyle: {
          color: "#5ac8fa",
        },
        data: [4],
      },
      {
        name: "Swap",
        type: "bar",
        stack: "total",
        emphasis: {
          focus: "series",
        },
        itemStyle: {
          color: "#003876",
          barBorderRadius: [0, 3, 3, 0]
        },
        data: [1],
      },

    ],
  };

  mergeOptionNetwork = {};
  chartOptionNetwork = {
    // legend: {
    //   data: ["TX", "RX"],
    //   type: 'scroll',
    //   orient: 'vertical',
    //   top: 6,
    //   x:'right',
    //   y:'center',
    //   itemWidth: 10
    // },
    grid: {
      top: '5%',
      bottom: '5%'
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
          type: 'shadow'
      },
    },
    xAxis: {
      type: "value",
      max: 1024,
      show: false,
    },
    yAxis: {
      type: "category",
      show: false,
      data: ["Network IO"],
    },
    series: [
      {
        name: "TX",
        type: "bar",
        showBackground: true,
        itemStyle: {
          color: "#007aff",
          borderRadius: 3,
        },
        data: [768],
      },
      {
        name: "RX",
        type: "bar",
        showBackground: true,
        itemStyle: {
          color: "#5ac8fa",
          borderRadius: 3
        },
        data: [256],
      },
    ],
  };


  mergeOptionDisk = {};
  chartOptionDisk = {
    // legend: {
    //   data: ["W", "R"],
    //   type: 'scroll',
    //   orient: 'vertical',
    //   top: 6,
    //   x:'right',
    //   y:'center',
    //   itemWidth: 10
    // },
    grid: {
      top: '5%',
      bottom: '5%'
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
          type: 'shadow'
      },
      // formatter: 'Network<br>Write: {c0} MBit/s <br>Read: {c1} MBit/s'
    },
    xAxis: {
      type: "value",
      max: 1024,
      show: false,

    },
    yAxis: {
      type: "category",
      show: false,
      data: ["Disk IO"],
    },
    series: [
      {
        name: "Write",
        type: "bar",
        showBackground: true,
        itemStyle: {
          color: "#007aff",
          borderRadius: 3
        },
        data: [1024],
      },
      {
        name: "Read",
        type: "bar",
        showBackground: true,
        itemStyle: {
          color: "#5ac8fa",
          borderRadius: 3
        },
        data: [578],
      },
    ],
  };

  mergeOptionGpu = {};
  chartOptionGpu = {
    series: [
      {
        name: "GPU",
        type: "gauge",
        radius: 27,
        startAngle: 90,
        endAngle: -270,
        pointer: {
          show: false,
        },
        progress: {
          show: true,
          overlap: false,
          roundCap: true,
          clip: false,
          itemStyle: {
            color: "#FF5050",
          },
        },
        axisLine: {
          lineStyle: {
            width: 5,
          },
        },
        splitLine: {
          show: false,
          distance: 0,
          length: 10,
        },
        axisTick: {
          show: false,
        },
        axisLabel: {
          show: false,
          distance: 50,
        },
        data: [
          {
            value: 71,
            detail: {
              offsetCenter: ["0%", "0%"],
            },
          },
        ],
        detail: {
          width: 50,
          height: 14,
          fontSize: 15,
          fontWeight: 'normal',
          color: "black",
          formatter: "{value}%",
        },
      },
    ],
  };

  average = (arr) => arr.reduce((p, c) => p + c, 0) / arr.length;

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

    this.node = backupNode;
    this.node.update = (node) => {
      // load all the new goodies without replacing the object
      if (node != null && node.time !== this.node.time) {
        Object.keys(node).forEach(key => {
          this.node[key] = node[key];
        });
      }
      this.update();
    };
  }

  update() {
    this.updateCpuStatus();
    this.updateMemoryStatus();
    this.updateGpuStatus();
  }

  private updateCpuStatus() {
    let cpuValue: number = +(this.average(this.node.cpuInfo.utilization) * 100).toFixed(0);

    this.mergeOptionCpu = {
      series: [
        {
          name: "CPU",
          progress: {
            itemStyle: {
              color: this.setColor(cpuValue),
            },
          },
          data: [
            {
              value: cpuValue,
              detail: {
                offsetCenter: ["0%", "0%"],
              },
            },
          ],
        },
      ],
    };
  }

  private updateMemoryStatus() {
    let heap = this.bytesToGigabyte(this.node.memInfo.memoryTotal - this.node.memInfo.memoryFree).toFixed(2);
    let cache = this.bytesToGigabyte(this.node.memInfo.systemCache).toFixed(2);
    let swap = this.bytesToGigabyte(this.node.memInfo.swapTotal - this.node.memInfo.swapFree).toFixed(2)

    this.mergeOptionMemory = {
      series: [
        {
          name: "Heap",
          data: [
            {value: heap}
          ],
        },
        {
          name: "Cache",
          data: [
            {value: cache}
          ],
        },
        {
          name: "Swap",
          data: [
            {value: swap}
          ],
        },
      ],
    };
  }

  private updateGpuStatus() {
    let gpus: any[] = [];;

    for (const gpu of this.node.gpuInfo) {
      gpus.push(gpu.computeUtilization);
      gpus.push(gpu.memoryUtilization);
    }

    let gpuValue = +this.average(gpus).toFixed(0);

    this.mergeOptionGpu = {
      series: [
        {
          name: "GPU",
          progress: {
            itemStyle: {
              color: this.setColor(gpuValue),
            },
          },
          data: [
            {
              value: gpuValue,
              detail: {
                offsetCenter: ["0%", "0%"],
              },
            },
          ],
        },
      ],
    };
  }

  setColor(val: number) {
    let color;

    if (val < 40) {
      color = "#69B34C";
    } else if (val < 80) {
      color = "#FF8E15";
    } else {
      color = "#FF5050";
    }

    return color;
  }

  bytesToGigabyte(bytes: number) {
    return bytes / (1024 * 1024 * 1024);
  }
}
