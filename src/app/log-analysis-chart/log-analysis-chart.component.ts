import {Component, Input, OnInit} from '@angular/core';
import {ChartData, ChartSettings} from "../log-analysis/log-chart";

@Component({
  selector: 'app-log-analysis-chart',
  templateUrl: './log-analysis-chart.component.html',
  styleUrls: ['./log-analysis-chart.component.css']
})
export class LogAnalysisChartComponent implements OnInit {

  @Input() settings: ChartSettings;

  @Input() data: ChartData;

  constructor() {
  }

  ngOnInit(): void {
  }

  eChartOptions() {
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
      series: this.data.map(series => ({
        type: 'line',
        showSymbol: false,
        data: series,
      })),
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
