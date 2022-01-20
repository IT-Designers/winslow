import {Component, Inject} from '@angular/core';
import {MAT_DIALOG_DATA} from '@angular/material/dialog';
import {LogEntry} from "../api/project-api.service";

export class LogChart {
  name: string = "Unnamed chart";
  regExpSource: string = 'DummyLogEntry:.*entry="(?<entry>\\d+)".*value1="(?<v1>\\d+)".*value2="(?<v2>\\d+)".*value3="(?<v3>\\d+)"';
  xAxisGroup: string = "entry";
  xAxisMinValue: string = "";
  xAxisMaxValue: string = "";
  yAxisGroup: string = "v1";
  yAxisMinValue: string = "0";
  yAxisMaxValue: string = "10";
  useTimeAsXAxis: boolean = false;
}

@Component({
  selector: 'app-log-analysis-chart-dialog',
  templateUrl: './log-analysis-chart-dialog.component.html',
  styleUrls: ['./log-analysis-chart-dialog.component.css']
})
export class LogAnalysisChartDialogComponent {

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: { chart: LogChart, logs: LogEntry[] }
  ) {
  }

}
