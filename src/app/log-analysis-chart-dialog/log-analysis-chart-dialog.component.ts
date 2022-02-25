import {Component, Inject, OnDestroy, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA} from '@angular/material/dialog';
import {
  ChartAxisType,
  ChartDialogData,
} from "../log-analysis/log-chart-definition";

@Component({
  selector: 'app-log-analysis-chart-dialog',
  templateUrl: './log-analysis-chart-dialog.component.html',
  styleUrls: ['./log-analysis-chart-dialog.component.css']
})
export class LogAnalysisChartDialogComponent implements OnInit, OnDestroy {

  AxisTypes = Object.values(ChartAxisType);

  constructor (@Inject(MAT_DIALOG_DATA) public data: ChartDialogData) {
  }

  ngOnInit(): void {
  }

  ngOnDestroy(): void {
  }

}
