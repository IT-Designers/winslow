import { Component } from "@angular/core";
import { colorSets } from "@swimlane/ngx-charts";

@Component({
  selector: "app-server-bar",
  templateUrl: "./server-bar.component.html",
  styleUrls: ["./server-bar.component.css"],
})
export class ServerBarComponent {
  constructor() {}

  mergeOptionCpu = {};
  chartOptionCpu = {
    series: [
      {
        name: "CPU",
        type: "gauge",
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
            color: "#69B34C",
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
          fontSize: 14,
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
        extraCssText: 'z-index: 9999'
    },
    // legend: {
    //   data: ["Heap", "Cache", "Swap"],
    //   itemWidth: 10
    // },
    // grid: {
    //   top: '25%',
    //   bottom: '25%'
    // },
    xAxis: {
      type: "value",
      max: 32,
      show: false,
    },
    yAxis: {
      type: "category",
      show: false,
      data: ["Memory"],
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
          color: "#69B34C",
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
          color: "#FAB733",
        },
        data: [2],
      },
      {
        name: "Swap",
        type: "bar",
        stack: "total",
        emphasis: {
          focus: "series",
        },
        itemStyle: {
          color: "#FF0D0D",
          barBorderRadius: [0, 3, 3, 0]
        },
        data: [3],
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
    // grid: {
    //   right: '25%'
    // },
    xAxis: {
      type: "value",
      max: 1024,
      show: false,
    },
    yAxis: {
      type: "category",
      show: false,
    },
    series: [
      {
        name: "TX",
        type: "bar",
        showBackground: true,
        itemStyle: {
          color: "#FF0D0D",
          borderRadius: 3,
        },
        data: [768],
      },
      {
        name: "RX",
        type: "bar",
        showBackground: true,
        itemStyle: {
          color: "#69B34C",
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
    // grid: {
    //   right: '25%'
    // },
    xAxis: {
      type: "value",
      max: 1024,
      show: false,
    },
    yAxis: {
      type: "category",
      show: false,
    },
    series: [
      {
        name: "W",
        type: "bar",
        showBackground: true,
        itemStyle: {
          color: "#FF0D0D",
          borderRadius: 3
        },
        data: [1024],
      },
      {
        name: "R",
        type: "bar",
        showBackground: true,
        itemStyle: {
          color: "#69B34C",
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
            color: "#FF0D0D",
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
          fontSize: 14,
          fontWeight: 'normal',
          color: "black",
          formatter: "{value}%",
        },
      },
    ],
  };

  update() {
    let cpuValue: number = +(Math.random() * 100).toFixed(0);

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

    let gpuValue: number = +(Math.random() * 100).toFixed(0);

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
      color = "#FF0D0D";
    }

    return color;
  }
}
