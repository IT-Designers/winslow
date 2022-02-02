import {Component, Inject} from '@angular/core';
import {MAT_DIALOG_DATA} from '@angular/material/dialog';
import {ChartAxisType} from "../log-analysis-chart/log-analysis-chart.component";
import {LogChart} from "../log-analysis/log-analysis.component";

@Component({
  selector: 'app-log-analysis-chart-dialog',
  templateUrl: './log-analysis-chart-dialog.component.html',
  styleUrls: ['./log-analysis-chart-dialog.component.css']
})
export class LogAnalysisChartDialogComponent {

  AxisTypes = Object.values(ChartAxisType);

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: { chart: LogChart }
  ) {
  }
}
