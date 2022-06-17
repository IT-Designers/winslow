import {Component, Inject} from '@angular/core';
import {MAT_DIALOG_DATA} from '@angular/material/dialog';
import {
  ChartAxisType,
  ChartDialogData,
  ChartDisplaySettings,
  LogChart,
  LogChartDefinition, LogChartSnapshot
} from "../log-analysis/log-chart-definition";
import {Observable} from "rxjs";
import {map} from "rxjs/operators";
import {CsvFileInfo} from "../log-analysis/csv-file-controller";

@Component({
  selector: 'app-log-analysis-chart-dialog',
  templateUrl: './log-analysis-chart-dialog.component.html',
  styleUrls: ['./log-analysis-chart-dialog.component.css']
})
export class LogAnalysisChartDialogComponent {

  AxisTypes = Object.values(ChartAxisType);
  chart: LogChart;
  definition: LogChartDefinition;

  snapshot$: Observable<LogChartSnapshot>
  variableSuggestions$: Observable<string[]>;

  AxisTypes = Object.values(ChartAxisType);

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: ChartDialogData
  ) {

  ngOnInit(): void {
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
