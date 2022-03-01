import {Component, Inject, OnDestroy, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA} from '@angular/material/dialog';
import {ChartAxisType, ChartDialogData, LogChart, LogChartDefinition} from "../log-analysis/log-chart-definition";

@Component({
  selector: 'app-log-analysis-chart-dialog',
  templateUrl: './log-analysis-chart-dialog.component.html',
  styleUrls: ['./log-analysis-chart-dialog.component.css']
})
export class LogAnalysisChartDialogComponent implements OnInit, OnDestroy {

  AxisTypes = Object.values(ChartAxisType);
  chart: LogChart;
  definition: LogChartDefinition;

  constructor(@Inject(MAT_DIALOG_DATA) public data: ChartDialogData) {
    this.chart = new LogChart(data.csvFileController, data.chart.filename);

    this.definition = Object.assign({}, data.definition);
    this.definition.displaySettings = Object.assign({}, data.definition.displaySettings);

    this.refresh()
  }

  refresh() {
    this.definition.displaySettings = Object.assign({}, this.definition.displaySettings);
    this.chart.definition$.next(this.definition);
  }

  ngOnInit(): void {
  }

  ngOnDestroy(): void {
  }
}
