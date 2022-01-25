import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {ExecutionGroupInfo, LogEntry, ProjectApiService, ProjectInfo} from "../api/project-api.service";
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

  longLoading = new LongLoadingDetector();
  logSubscription: Subscription = null;
  selectedProject: ProjectInfo = null;
  selectedStageId: string = null;

  projectHistory: ExecutionGroupInfo[] = [];

  logs?: LogEntry[] = [];
  charts: LogChart[] = [];

  @Input()
  set project(project: ProjectInfo) {
    this.selectedProject = project;
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
    this.api.getProjectHistory(this.selectedProject.id).then(result => {
      this.projectHistory = result;
    });
    this.resubscribe(this.selectedStageId);
    if (this.selectedStageId == null) {
      this.selectedStageId = this.getLatestStageId();
    }
  }

  ngOnDestroy() {
    if (this.logSubscription) {
      this.logSubscription.unsubscribe();
      this.logSubscription = null;
    }
  }

  resubscribe(stageId: string) {
    this.logs = [];
    if (this.logSubscription != null) {
      this.logSubscription.unsubscribe();
    }
    this.subscribeLogs(this.selectedProject.id, stageId);
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

  selectLatestStage() {
    this.selectedStageId = this.getLatestStageId();
    this.resubscribe(this.selectedStageId);
  }

  filteredProjectHistory() {
    return this.projectHistory.filter(entry => !entry.configureOnly)
  }
}
