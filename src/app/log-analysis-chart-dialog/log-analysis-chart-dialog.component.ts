import {Component, Inject} from '@angular/core';
import {MAT_DIALOG_DATA} from '@angular/material/dialog';
import {LogEntry, LogSource} from "../api/project-api.service";
import {ChartSettings, ChartAxisType} from "../log-analysis-chart/log-analysis-chart.component";

@Component({
  selector: 'app-log-analysis-chart-dialog',
  templateUrl: './log-analysis-chart-dialog.component.html',
  styleUrls: ['./log-analysis-chart-dialog.component.css']
})
export class LogAnalysisChartDialogComponent {

  AxisTypes = Object.values(ChartAxisType);

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: { chart: ChartSettings, logs: LogEntry[] }
  ) {
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
