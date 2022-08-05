import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {ChartDataSet, ChartDisplaySettings, LogChart} from "../log-analysis/log-chart-definition";
import {Subscription} from "rxjs";
import {getColorSet} from "../log-analysis/colors";

@Component({
  selector: 'app-log-analysis-chart',
  templateUrl: './log-analysis-chart.component.html',
  styleUrls: ['./log-analysis-chart.component.css']
})
export class LogAnalysisChartComponent implements OnInit, OnDestroy {

  options: {}
  subscription: Subscription;

  @Input() chart: LogChart

  constructor() {
  }

  ngOnInit() {
    this.subscription = this.chart.snapshot$.subscribe({
      next: snapshot => {
        this.settings(snapshot.definition.displaySettings)
        this.data(snapshot.chartData)
      }
    })
  }

  ngOnDestroy() {
    this.subscription?.unsubscribe()
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
      color: getColorSet(10),
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

  private updateOptions(newOptions) {
    this.options = {...this.options, ...newOptions}
  }
}
