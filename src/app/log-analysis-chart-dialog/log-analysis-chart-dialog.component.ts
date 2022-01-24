import {Component, Inject} from '@angular/core';
import {MAT_DIALOG_DATA} from '@angular/material/dialog';
import {
  ChartAxisType,
  ChartDataSeries,
  ChartDialogData,
  LogChartDefinition
} from "../log-analysis/log-chart-definition";

@Component({
  selector: 'app-log-analysis-chart-dialog',
  templateUrl: './log-analysis-chart-dialog.component.html',
  styleUrls: ['./log-analysis-chart-dialog.component.css']
})
export class LogAnalysisChartDialogComponent {
  AxisTypes = Object.values(ChartAxisType);

  AxisTypes = Object.values(LogChartAxisType);

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: ChartDialogData
  ) {
  }

  getChartData(): ChartDataSeries[] {
    const chart = this.data.chartDefinition;
    const stages = this.data.stages;
    return stages.map(stage => LogChartDefinition.getDataSeries(chart, stage.csvFiles));
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
