import {Component, Inject, OnDestroy} from '@angular/core';
import {MAT_DIALOG_DATA} from '@angular/material/dialog';
import {AnalysisChart, ChartSnapShot} from '../chart-definition';
import {Observable, Subscription} from 'rxjs';
import {CsvFile, CsvFilesService} from '../csv-files.service';
import {map} from "rxjs/operators";
import {ChartDefinition, ChartAxisType} from "../../../api/winslow-api";

@Component({
  selector: 'app-log-analysis-chart-dialog',
  templateUrl: './log-analysis-chart-dialog.component.html',
  styleUrls: ['./log-analysis-chart-dialog.component.css']
})
export class LogAnalysisChartDialogComponent implements OnDestroy {

  // These are values from ChartAxisType from winslow-api.ts
  value: ChartAxisType = 'VALUE';
  log: ChartAxisType = 'LOG';
  time: ChartAxisType = 'TIME';
  AxisTypes = [this.value, this.log, this.time];

  chart: AnalysisChart;
  definition: ChartDefinition;
  latestSnapshot: ChartSnapShot | undefined;
  subscription: Subscription;
  fileSuggestions: Observable<string[]>;

  constructor(
    private csvFilesService: CsvFilesService,
    @Inject(MAT_DIALOG_DATA) dialogData: ChartDefinition,
  ) {
    const definition: ChartDefinition = dialogData;

    this.chart = new AnalysisChart(this.csvFilesService, definition);

    this.definition = {...definition};
    this.subscription = this.chart.snapshot$.subscribe(snapshot => this.latestSnapshot = snapshot);

    this.fileSuggestions = csvFilesService.getFileSuggestions$(this.chart.definition$.pipe(map(definition => definition.file)))

    this.refresh();
  }

  ngOnDestroy() {
    this.subscription?.unsubscribe();
  }

  refresh() {
    this.chart.definition$.next(this.definition);
  }

  findEmptyCsvFiles(snapshot: ChartSnapShot): CsvFile[] {
    if (snapshot == null) {
      return [];
    }
    return snapshot.csvFiles.filter(csvFile => csvFile.content.length == 0);
  }

  isInvalidVariable(variable: string | null | undefined): boolean {
    if (variable == undefined || variable == '') {
      return false; // Empty variable means use default instead
    }
    if (this.latestSnapshot == undefined) {
      return false; // Cannot validate yet, so assume no mistakes were made
    }
    return !this.latestSnapshot.formatterVariables.includes(variable);
  }

  isInvalidEntryLimit(entryLimit: number | null | undefined): boolean {
    if (entryLimit == null) {
      return false;
    }
    return entryLimit <= 1;
  }

}
