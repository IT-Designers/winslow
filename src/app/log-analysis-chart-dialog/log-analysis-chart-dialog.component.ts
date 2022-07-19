import {Component, Inject, OnDestroy} from '@angular/core';
import {MAT_DIALOG_DATA} from '@angular/material/dialog';
import {
  ChartAxisType,
  ChartDialogData,
  ChartDisplaySettings,
  LogChart,
  LogChartDefinition,
  LogChartSnapshot
} from "../log-analysis/log-chart-definition";
import {CsvFile} from "../log-analysis/csv-file-controller";
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

  constructor(@Inject(MAT_DIALOG_DATA) private data: ChartDialogData) {
    this.chart = new LogChart(data.csvFileController, data.chart.filename, data.definition);

    this.definition = Object.assign(new LogChartDefinition(), data.definition);
    this.definition.displaySettings = Object.assign(new ChartDisplaySettings(), data.definition.displaySettings);
    this.subscription = this.chart.snapshot$.subscribe(snapshot => this.latestSnapshot = snapshot)

    this.refresh()
  }

  ngOnDestroy() {
    this.subscription?.unsubscribe()
  }

  refresh() {
    this.definition.displaySettings = Object.assign({}, this.definition.displaySettings)
    this.chart.definition$.next(this.definition)
  }

  findEmptyCsvFiles(snapshot: LogChartSnapshot): CsvFile[] {
    if (snapshot == null) return []
    return snapshot.csvFiles.filter(csvFile => csvFile.content.length == 0)
  }

  isValidVariable(variable: string) {
    if (variable == "") return true
    return this.latestSnapshot.formatterVariables.includes(variable)
  }

  isValidEntryLimit(entryLimit: number | null) {
    if (entryLimit == null) return true
    return entryLimit > 1
  }
}
