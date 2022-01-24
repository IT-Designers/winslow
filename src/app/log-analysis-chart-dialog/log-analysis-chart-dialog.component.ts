import {Component, Inject} from '@angular/core';
import {MAT_DIALOG_DATA} from '@angular/material/dialog';
import {LogEntry, LogSource} from "../api/project-api.service";

export enum LogChartAxisType {
  GROUP = "Capturing Group",
  TIME = "Time",
  STEPS = "Steps",
}

export class LogChart {
  name: string = "Unnamed chart";
  regExpSource: string = 'ExampleLogEntry:.*x_value=(?<x>\\d+).*y_value=(?<y>\\d+)';

  xAxisLabel: string = "x-Axis";
  yAxisLabel: string = "y-Axis";
  xAxisMinValue: string = "";
  xAxisMaxValue: string = "";
  yAxisMinValue: string = "0";
  yAxisMaxValue: string = "10";

  xAxisType: LogChartAxisType = LogChartAxisType.GROUP;
  xAxisGroup: string = "x";

  graphs: LogChartGraph[] = [
    new LogChartGraph()
  ];
}

export class LogChartGraph {
  label: string = "Unnamed graph";
  yAxisGroup: string = "y";
}

@Component({
  selector: 'app-log-analysis-chart-dialog',
  templateUrl: './log-analysis-chart-dialog.component.html',
  styleUrls: ['./log-analysis-chart-dialog.component.css']
})
export class LogAnalysisChartDialogComponent {

  AxisTypes = Object.values(LogChartAxisType);

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: { chart: LogChart, logs: LogEntry[] }
  ) {
  }

  addNewGraph() {
    this.data.chart.graphs.push(new LogChartGraph());
  }

  getPreviewText() {
    let lines = [];
    for (let log of this.data.logs) {
      if (log.source == LogSource.STANDARD_IO) {
        lines.push(log.message);
      }
      if (lines.length >= 10) {
        break;
      }
    }
    return lines;
  }
}
