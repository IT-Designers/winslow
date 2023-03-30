import {Component, Inject, OnDestroy} from '@angular/core';
import {MAT_DIALOG_DATA} from '@angular/material/dialog';
import {
  ChartAxisType,
  LogChart,
  LogChartDefinition,
  LogChartSnapshot
} from '../log-chart-definition';
import {Subscription} from 'rxjs';
import {CsvFile, CsvFilesService} from '../csv-files.service';

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

  constructor(
    private csvFilesService: CsvFilesService,
    @Inject(MAT_DIALOG_DATA) dialogData: LogChartDefinition,
  ) {
    const definition = dialogData;

    this.chart = new LogChart(this.csvFilesService, null, definition);

    this.definition = {...definition};
    this.definition.displaySettings = {...definition.displaySettings};
    this.subscription = this.chart.snapshot$.subscribe(snapshot => this.latestSnapshot = snapshot);

    this.refresh();
  }

  ngOnDestroy() {
    this.subscription?.unsubscribe();
  }

  refresh() {
    this.definition.displaySettings = Object.assign({}, this.definition.displaySettings);
    this.chart.definition$.next(this.definition);
  }

  findEmptyCsvFiles(snapshot: LogChartSnapshot): CsvFile[] {
    if (snapshot == null) {
      return [];
    }
    return snapshot.csvFiles.filter(csvFile => csvFile.content.length == 0);
  }

  isValidVariable(variable: string) {
    if (variable == '') {
      return true;
    }
    return this.latestSnapshot.formatterVariables.includes(variable);
  }

  isValidEntryLimit(entryLimit: number | null) {
    if (entryLimit == null) {
      return true;
    }
    return entryLimit > 1;
  }
}
