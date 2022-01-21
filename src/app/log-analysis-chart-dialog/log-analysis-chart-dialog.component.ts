import {Component, Inject} from '@angular/core';
import {MAT_DIALOG_DATA} from '@angular/material/dialog';
import {LogEntry} from "../api/project-api.service";

export enum LogChartAxisType {
  GROUP = "Capturing Group",
  TIME = "Time",
  STEPS = "Steps",
}

export class LogChart {
  name: string = "Unnamed chart";
  regExpSource: string = 'ExampleLogEntry:.*thing1=(?<group1>\\d+).*thing2=(?<group2>\\d+)';
  xAxisType: LogChartAxisType = LogChartAxisType.GROUP;
  xAxisGroup: string = "group1";
  xAxisMinValue: string = "";
  xAxisMaxValue: string = "";
  yAxisGroup: string = "group2";
  yAxisMinValue: string = "0";
  yAxisMaxValue: string = "10";
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

}
