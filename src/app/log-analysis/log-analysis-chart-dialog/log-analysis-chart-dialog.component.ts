import {Component, Inject, OnDestroy} from '@angular/core';
import {MAT_DIALOG_DATA} from '@angular/material/dialog';
import {
  ChartAxisType,
  ChartDialogData,
  ChartDisplaySettings,
  LogChart,
  LogChartDefinition,
  LogChartSnapshot
} from "../log-chart-definition";
import {CsvFile} from "../csv-file-controller";
import {Subscription} from "rxjs";

@Component({
  selector: 'app-log-analysis-chart-dialog',
  templateUrl: './log-analysis-chart-dialog.component.html',
  styleUrls: ['./log-analysis-chart-dialog.component.css']
})
export class LogAnalysisChartDialogComponent implements OnDestroy {

  AxisTypes = Object.values(ChartAxisType);
  chart: LogChart;
  definition: LogChartDefinition;
  latestSnapshot: LogChartSnapshot;
  subscription: Subscription;

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

  isValidEntryLimit(entryLimit: number | null) {
    if (entryLimit == null) return true
    return entryLimit > 1
  }
}
