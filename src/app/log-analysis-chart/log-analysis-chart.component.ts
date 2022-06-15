import {Component, Input} from '@angular/core';
import {ChartDataSet, ChartDisplaySettings, LogChart} from "../log-analysis/log-chart-definition";

@Component({
  selector: 'app-log-analysis-chart',
  templateUrl: './log-analysis-chart.component.html',
  styleUrls: ['./log-analysis-chart.component.css']
})
export class LogAnalysisChartComponent {

  options: {}

  @Input() set chart(chart: LogChart) {
    chart.snapshot$.subscribe({
      next: snapshot => {
        this.settings(snapshot.definition.displaySettings)
        this.data(snapshot.chartData)
      }
    })
  }

  settings(settings: ChartDisplaySettings) {
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

  data(chartData: ChartDataSet[]) {
    const series = chartData.map(data => ({
      type: 'line',
      showSymbol: false,
      data: data,
    }))
    const newOptions = {
      series: series,
    }
    this.updateOptions(newOptions);
  }

  constructor() {
  }

  private updateOptions(newOptions) {
    this.options = {...this.options, ...newOptions}
  }
}
