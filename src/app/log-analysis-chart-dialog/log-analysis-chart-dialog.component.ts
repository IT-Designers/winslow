import {Component, Inject} from '@angular/core';
import {MAT_DIALOG_DATA} from '@angular/material/dialog';
import {LogEntry} from "../api/project-api.service";

export interface LogChart {
  name: string;
  regExpSource: string;
  xAxisGroup: string;
  yAxisGroup: string;
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
