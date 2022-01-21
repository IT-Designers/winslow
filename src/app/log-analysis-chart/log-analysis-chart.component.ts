import {Component, Input, OnInit} from '@angular/core';
import {LogChart, LogChartAxisType} from "../log-analysis-chart-dialog/log-analysis-chart-dialog.component";
import {LogEntry, LogSource} from "../api/project-api.service";

@Component({
  selector: 'app-log-analysis-chart',
  templateUrl: './log-analysis-chart.component.html',
  styleUrls: ['./log-analysis-chart.component.css']
})
export class LogAnalysisChartComponent implements OnInit {

  @Input() chart: LogChart;

  @Input() logs: LogEntry[];

  constructor() {
  }

  ngOnInit(): void {
  }

  getDefaultChart() {
    return {
      title: {
        text: "",
      },
      grid: {
        top: '50',
        bottom: '20',
        left: '40',
        right: '10',
      },
      xAxis: {
        type: 'value',
        min: 'dataMin',
        max: 'dataMax',
      },
      yAxis: {
        type: 'value',
        min: 'dataMin',
        max: 'dataMax',
      },
      series: [
        {
          type: 'line',
          showSymbol: false,
          data: [],
        }
      ]
    }
  }

  sanitizeAxisLimit(input: string, type: 'min' | 'max'): string {
    if (Number.isNaN(parseFloat(input))) {
      return type == 'max' ? "dataMax" : "dataMin";
    } else {
      return input;
    }
  }

  getChartOptions(chart: LogChart, logs: LogEntry[]) {
    let options = this.getDefaultChart();

    options.title.text = chart.name;

    options.xAxis.max = this.sanitizeAxisLimit(chart.xAxisMaxValue, 'max');
    options.xAxis.min = this.sanitizeAxisLimit(chart.xAxisMinValue, 'min');
    options.yAxis.max = this.sanitizeAxisLimit(chart.yAxisMaxValue, 'max');
    options.yAxis.min = this.sanitizeAxisLimit(chart.yAxisMinValue, 'min');

    switch (chart.xAxisType) {
      case LogChartAxisType.GROUP:
        options.series[0].data = this.getGroupData(chart, logs);
        break;
      case LogChartAxisType.TIME:
        options.xAxis.type = 'time';
        options.series[0].data = this.getTimeData(chart, logs);
        break;
      case LogChartAxisType.STEPS:
        options.series[0].data = this.getStepData(chart, logs);
        break;
    }
    return options;
  }

  getGroupData(chart: LogChart, logs: LogEntry[]) {
    let results = [];
    for (let log of logs) {
      if (log.source != LogSource.STANDARD_IO) continue;

      let match = log.message.match(chart.regExpSource);
      if (!match) continue;

      let x = parseFloat(match.groups[chart.xAxisGroup]);
      let y = parseFloat(match.groups[chart.yAxisGroup]);
      if (isNaN(x) || isNaN(y)) continue;

      results.push([x, y]);
    }
    results.sort((pair1, pair2) => {
      return pair1[0] - pair2[0]
    })
    return results;
  }

  getTimeData(chart: LogChart, logs: LogEntry[]) {
    let results = [];
    for (let log of logs) {
      if (log.source != LogSource.STANDARD_IO) continue;

      let match = log.message.match(chart.regExpSource);
      if (!match) continue;

      let x = log.time;
      let y = parseFloat(match.groups[chart.yAxisGroup]);
      if (isNaN(x) || isNaN(y)) continue;

      results.push([x, y]);
    }
    results.sort((pair1, pair2) => {
      return pair1[0] - pair2[0]
    })
    return results;
  }

  getStepData(chart: LogChart, logs: LogEntry[]) {
    let results = [];
    let iteration = 0;
    for (let log of logs) {
      if (log.source != LogSource.STANDARD_IO) continue;

      let match = log.message.match(chart.regExpSource);
      if (!match) continue;

      let x = iteration;
      let y = parseFloat(match.groups[chart.yAxisGroup]);
      if (isNaN(x) || isNaN(y)) continue;

      results.push([x, y]);
      iteration++;
    }
    return results;
  }
}
