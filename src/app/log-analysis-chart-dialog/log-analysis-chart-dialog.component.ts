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

  constructor(@Inject(MAT_DIALOG_DATA) private data: ChartDialogData) {
    this.chart = new LogChart(data.csvFileController, data.chart.filename, data.definition);

    this.definition = Object.assign(new LogChartDefinition(), data.definition);
    this.definition.displaySettings = Object.assign(new ChartDisplaySettings(), data.definition.displaySettings);

    this.snapshot$ = this.chart.snapshot$
    this.variableSuggestions$ = this.chart.snapshot$.pipe(
      map(snapshot => snapshot.formatterVariables)
    )

    this.refresh()
  }

  refresh() {
    this.definition.displaySettings = Object.assign({}, this.definition.displaySettings);
    this.chart.definition$.next(this.definition);
  }

  findEmptyCsvFiles(snapshot: LogChartSnapshot): CsvFileInfo[] {
    if (snapshot == null) return []
    return snapshot.csvFiles.filter(csvFile => csvFile.content.length == 0)
  }



}
