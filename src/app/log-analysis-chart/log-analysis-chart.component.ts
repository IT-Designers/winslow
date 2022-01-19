import {Component, Input, OnInit} from '@angular/core';
import {LogChart} from "../log-analysis-chart-dialog/log-analysis-chart-dialog.component";
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

  getChartOptions(chart: LogChart, logs: LogEntry[]) {
    // noinspection UnnecessaryLocalVariableJS
    let chartOption = {
      title: {
        text: chart.name
      },
      grid: {
        top: '50',
        bottom: '20',
        left: '40',
        right: '10',
      },
      xAxis: {
        type: 'value',
        min: 0,
        max: 'dataMax',
      },
      yAxis: {
        type: 'value',
        min: 0,
        max: 'dataMax',
      },
      series: [
        {
          type: 'line',
          showSymbol: false,
          data: this.getLogValuePairs(chart, logs)
        }
      ]
    };

    return chartOption
  }

  getLogValuePairs(chart: LogChart, logs: LogEntry[]) {
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

}
