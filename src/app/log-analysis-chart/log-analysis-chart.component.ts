import {Component, Input} from '@angular/core';
import {ChartDataSet, ChartDisplaySettings} from "../log-analysis/log-chart-definition";

@Component({
  selector: 'app-log-analysis-chart',
  templateUrl: './log-analysis-chart.component.html',
  styleUrls: ['./log-analysis-chart.component.css']
})
export class LogAnalysisChartComponent {

  options: {}

  @Input() set settings(settings: ChartDisplaySettings) {
    const newOptions = {
      title: {
        text: settings.name,
      },
      grid: {
        top: 50,
        bottom: 35,
        left: 40,
        right: 10,
      },
      xAxis: {
        name: settings.xAxisName,
        type: settings.xAxisType,
        min: settings.xAxisMinValue ?? 'dataMin',
        max: settings.xAxisMaxValue ?? 'dataMax',
        nameLocation: 'center',
        nameGap: 25,
      },
      yAxis: {
        name: settings.yAxisName,
        type: settings.yAxisType,
        min: settings.yAxisMinValue ?? 'dataMin',
        max: settings.yAxisMaxValue ?? 'dataMax',
        nameLocation: 'center',
        nameGap: 25,
      },
      animation: false,
    }
    this.updateOptions(newOptions);
  };

  @Input() set data(chartData: ChartDataSet[]) {
    const series = chartData.map(data => ({
      type: 'line',
      showSymbol: false,
      data: data,
    }))
    const newOptions = {
      series: series,
    }
    console.log(series)
    this.updateOptions(newOptions);
  }

  constructor() {
  }

  ngOnInit(): void {
  }

  ngOnDestroy(): void {
  }

  private static sanitiseNumberInput(input: string, alt: string): string {
    if (Number.isNaN(parseFloat(input))) {
      return alt;
    } else {
      return input;
    }
  }

  getDataByStep(chart: Chart, graph: ChartGraph, logs: LogEntry[]): [number, number][] {
    let points = []

    let step = 0;
    for (let log of logs) {
      try {
        if (log.source != LogSource.STANDARD_IO) continue;

        let match = log.message.match(chart.regExpSource);
        if (!match) continue;

        let y = parseFloat(match.groups[graph.yAxisGroup]);
        if (isNaN(y)) continue;

        points.push([step, y]);
        step++;
      } catch (ignored) {
      }
    }
    return points;
  }
}
