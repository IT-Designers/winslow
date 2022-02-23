import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {ChartDataSeries, ChartDisplaySettings} from "../log-analysis/log-chart-definition";
import {Observable, Subscription} from "rxjs";

@Component({
  selector: 'app-log-analysis-chart',
  templateUrl: './log-analysis-chart.component.html',
  styleUrls: ['./log-analysis-chart.component.css']
})
export class LogAnalysisChartComponent implements OnInit, OnDestroy {

  private options;

  private chartSeries;

  private dataSubscription: Subscription = null;

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
        min: this.sanitiseNumberInput(settings.xAxisMinValue, 'dataMin'),
        max: this.sanitiseNumberInput(settings.xAxisMaxValue, 'dataMax'),
        nameLocation: 'center',
        nameGap: '25',
      },
      yAxis: {
        name: settings.yAxisName,
        type: settings.yAxisType,
        min: this.sanitiseNumberInput(settings.yAxisMinValue, 'dataMin'),
        max: this.sanitiseNumberInput(settings.yAxisMaxValue, 'dataMax'),
        nameLocation: 'center',
        nameGap: '25',
      },
      animation: false,
      series: this.chartSeries,
    }
  };

  @Input() set dataSource(dataSource: Observable<ChartDataSeries[]>) {
    this.dataSubscription = dataSource.subscribe({
      next: chartData => this.insertDataSeries(chartData)
    })
  }

  constructor() {
  }

  ngOnDestroy(): void {
    this.dataSubscription?.unsubscribe();
  }

  ngOnInit(): void {
  }

  eChartOptions() {
    this.options.series = this.chartSeries;
    return this.options;
  }

  sanitiseNumberInput(input: string, alt: string): string {
    if (Number.isNaN(parseFloat(input))) {
      return alt;
    } else {
      return input;
    }
  }

  private insertDataSeries(chartData: ChartDataSeries[]) {
    this.chartSeries = chartData.map(data => {
      return {
        type: 'line',
        showSymbol: false,
        data: data,
      }
    })
  }
}
