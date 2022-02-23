import {Component, Input, OnInit} from '@angular/core';
import {ChartDataSeries, ChartDisplaySettings} from "../log-analysis/log-chart-definition";

@Component({
  selector: 'app-log-analysis-chart',
  templateUrl: './log-analysis-chart.component.html',
  styleUrls: ['./log-analysis-chart.component.css']
})
export class LogAnalysisChartComponent implements OnInit {

  @Input() settings: ChartDisplaySettings = new ChartDisplaySettings();

  @Input() data: ChartDataSeries[] = [];

  constructor() {
  }

  ngOnInit(): void {
  }

  eChartOptions() {
    if (this.data == null || this.settings == null) {
      return null;
    }

    const chartSeries = this.data.map(dataSeries => ({
      type: 'line',
      showSymbol: false,
      data: dataSeries,
    }))

    return {
      title: {
        text: this.settings.name,
      },
      grid: {
        top: '50',
        bottom: '35',
        left: '40',
        right: '10',
      },
      xAxis: {
        name: this.settings.xAxisName,
        type: this.settings.xAxisType,
        min: this.sanitiseNumberInput(this.settings.xAxisMinValue, 'dataMin'),
        max: this.sanitiseNumberInput(this.settings.xAxisMaxValue, 'dataMax'),
        nameLocation: 'center',
        nameGap: '25',
      },
      yAxis: {
        name: this.settings.yAxisName,
        type: this.settings.yAxisType,
        min: this.sanitiseNumberInput(this.settings.yAxisMinValue, 'dataMin'),
        max: this.sanitiseNumberInput(this.settings.yAxisMaxValue, 'dataMax'),
        nameLocation: 'center',
        nameGap: '25',
      },
      animation: false,
      series: chartSeries,
    };
  }

  sanitiseNumberInput(input: string, alt: string): string {
    if (Number.isNaN(parseFloat(input))) {
      return alt;
    } else {
      return input;
    }
  }
}
