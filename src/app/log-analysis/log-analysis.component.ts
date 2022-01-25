import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {LogEntry, ProjectApiService, ProjectInfo} from "../api/project-api.service";
import {Subscription} from "rxjs";
import {MatDialog} from '@angular/material/dialog';
import {
  LogAnalysisChartDialogComponent,
  LogChart
} from "../log-analysis-chart-dialog/log-analysis-chart-dialog.component";
import {LongLoadingDetector} from "../long-loading-detector";

@Component({
  selector: 'app-log-analysis',
  templateUrl: './log-analysis.component.html',
  styleUrls: ['./log-analysis.component.css']
})
export class LogAnalysisComponent implements OnInit, OnDestroy {
  private static readonly LONG_LOADING_FLAG = 'logs';

  logSubscription: Subscription = null;

  logs?: LogEntry[] = [];

  selectedProject: ProjectInfo = null;
  selectedStageId: string = null;
  charts: LogChart[] = [];
  longLoading = new LongLoadingDetector();

  @Input()
  set project(value: ProjectInfo) {
    this.selectedProject = value;
  }

  @Input()
  set selectedStage(id: string) {
    this.selectedStageId = id;
  }

  constructor(
    private dialog: MatDialog,
    private api: ProjectApiService) {
  }

  ngOnInit(): void {
    this.subscribeLogs(this.selectedProject.id, this.selectedStageId)
  }

  ngOnDestroy() {
    if (this.logSubscription) {
      this.logSubscription.unsubscribe();
      this.logSubscription = null;
    }
  }

  private subscribeLogs(projectId: string, stageId = ProjectApiService.LOGS_LATEST) {
    if (stageId == null) {
      stageId = ProjectApiService.LOGS_LATEST;
    }
    this.longLoading.raise(LogAnalysisComponent.LONG_LOADING_FLAG);
    this.logSubscription = this.api.watchLogs(projectId, (logs) => {
      this.longLoading.clear(LogAnalysisComponent.LONG_LOADING_FLAG);
      if (logs?.length > 0) {
        this.logs.push(...logs);
      } else {
        this.logs = [];
      }
    }, stageId);
  }

  lineId(index, log): string {
    return log.stageId + log.line;
  }

  addNewChart() {
    let chart: LogChart = new LogChart();
    let index = this.charts.push(chart) - 1;
    this.openEditChartDialog(index);
  }

  openEditChartDialog(chartIndex: number) {
    let dialogRef = this.dialog.open(LogAnalysisChartDialogComponent, {
      data: {
        chart: this.charts[chartIndex],
        logs: this.logs
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      console.log(result)
    })
  }

  removeChart(chartIndex: number) {
    this.charts.splice(chartIndex, 1);
  }

  isLongLoading() {
    return this.longLoading.isLongLoading();
  }

  showLatestLogs() {

  }
}
