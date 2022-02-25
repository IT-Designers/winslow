import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {ChartDataSet, ChartDisplaySettings} from "../log-analysis/log-chart-definition";

@Component({
  selector: 'app-log-analysis-chart',
  templateUrl: './log-analysis-chart.component.html',
  styleUrls: ['./log-analysis-chart.component.css']
})
export class LogAnalysisChartComponent implements OnInit, OnDestroy {

  options: any;
  merge = null;

  @Input() set settings(settings: ChartDisplaySettings) {
    this.options = {
      title: {
        text: settings.name,
      },
      grid: {
        top: '50',
        bottom: '35',
        left: '40',
        right: '10',
      },
      xAxis: {
        name: settings.xAxisName,
        type: settings.xAxisType,
        min: LogAnalysisChartComponent.sanitiseNumberInput(settings.xAxisMinValue, 'dataMin'),
        max: LogAnalysisChartComponent.sanitiseNumberInput(settings.xAxisMaxValue, 'dataMax'),
        nameLocation: 'center',
        nameGap: '25',
      },
      yAxis: {
        name: settings.yAxisName,
        type: settings.yAxisType,
        min: LogAnalysisChartComponent.sanitiseNumberInput(settings.yAxisMinValue, 'dataMin'),
        max: LogAnalysisChartComponent.sanitiseNumberInput(settings.yAxisMaxValue, 'dataMax'),
        nameLocation: 'center',
        nameGap: '25',
      },
      animation: false,
      series: [],
    }
  };

  @Input() set data(chartData: ChartDataSet[]) {
    const series = chartData.map(data => ({
      type: 'line',
      showSymbol: false,
      data: data,
    }))

    this.merge = {
      series: series
    }
  }

  constructor() {
  }

  ngOnDestroy(): void {
  }

  ngOnInit(): void {
  }

  private static sanitiseNumberInput(input: string, alt: string): string {
    if (Number.isNaN(parseFloat(input))) {
      return alt;
    } else {
      return input;
    }
  }
}
