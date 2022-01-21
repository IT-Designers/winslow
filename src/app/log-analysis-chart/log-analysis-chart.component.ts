import {Component, Input, OnInit} from '@angular/core';
import {
  LogChart,
  LogChartAxisType,
  LogChartGraph
} from "../log-analysis-chart-dialog/log-analysis-chart-dialog.component";
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
        bottom: '35',
        left: '40',
        right: '10',
      },
      xAxis: {
        type: 'value',
        min: 'dataMin',
        max: 'dataMax',
        name: 'x-Axis',
        nameLocation: 'center',
        nameGap: '25',
      },
      yAxis: {
        type: 'value',
        min: 'dataMin',
        max: 'dataMax',
        name: 'y-Axis',
        nameLocation: 'center',
        nameGap: '25',
      },
      series: []
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

    options.xAxis.name = chart.xAxisLabel;
    options.yAxis.name = chart.yAxisLabel;

    options.xAxis.max = this.sanitizeAxisLimit(chart.xAxisMaxValue, 'max');
    options.xAxis.min = this.sanitizeAxisLimit(chart.xAxisMinValue, 'min');
    options.yAxis.max = this.sanitizeAxisLimit(chart.yAxisMaxValue, 'max');
    options.yAxis.min = this.sanitizeAxisLimit(chart.yAxisMinValue, 'min');

    switch (chart.xAxisType) {
      case LogChartAxisType.GROUP:
        options.series = this.getSeriesList(chart, this.getDataByGroup, logs);
        break;
      case LogChartAxisType.TIME:
        options.xAxis.type = 'time';
        options.series = this.getSeriesList(chart, this.getDataByTime, logs);
        break;
      case LogChartAxisType.STEPS:
        options.series = this.getSeriesList(chart, this.getDataByStep, logs);
        break;
    }

    return options;
  }

  getSeriesList(chart: LogChart, getDataFunction: (chart: LogChart, graph: LogChartGraph, logs: LogEntry[]) => [number, number][], logs: LogEntry[]) {
    let seriesList = [];
    for (let i = 0; i < chart.graphs.length; i++) {
      seriesList.push({
        type: 'line',
        showSymbol: false,
        data: getDataFunction(chart, chart.graphs[i], logs),
      })
    }
    return seriesList;
  }

  getDataByGroup(chart: LogChart, graph: LogChartGraph, logs: LogEntry[]): [number, number][] {
    let points = []

    for (let log of logs) {
      if (log.source != LogSource.STANDARD_IO) continue;

      let match = log.message.match(chart.regExpSource);
      if (!match) continue;

      let x = parseFloat(match.groups[chart.xAxisGroup]);
      if (isNaN(x)) continue;

      let y = parseFloat(match.groups[graph.yAxisGroup]);
      if (isNaN(y)) continue;

      points.push([x, y]);
    }
    points.sort((point1, point2) => point1[0] - point2[0])
    return points;
  }

  getDataByTime(chart: LogChart, graph: LogChartGraph, logs: LogEntry[]): [number, number][] {
    let points = []

    for (let log of logs) {
      if (log.source != LogSource.STANDARD_IO) continue;

      let match = log.message.match(chart.regExpSource);
      if (!match) continue;

      let y = parseFloat(match.groups[graph.yAxisGroup]);
      if (isNaN(y)) continue;

      points.push([log.time, y]);
    }
    points.sort((point1, point2) => point1[0] - point2[0])
    return points;
  }

  getDataByStep(chart: LogChart, graph: LogChartGraph, logs: LogEntry[]): [number, number][] {
    let points = []

    let step = 0;
    for (let log of logs) {
      if (log.source != LogSource.STANDARD_IO) continue;

      let match = log.message.match(chart.regExpSource);
      if (!match) continue;

      let y = parseFloat(match.groups[graph.yAxisGroup]);
      if (isNaN(y)) continue;

      points.push([step, y]);
      step++;
    }
    return points;
  }
}
