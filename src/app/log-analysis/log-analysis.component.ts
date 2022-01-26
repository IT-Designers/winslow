import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {ExecutionGroupInfo, LogEntry, ProjectApiService, ProjectInfo} from "../api/project-api.service";
import {Subscription} from "rxjs";
import {MatDialog} from '@angular/material/dialog';
import {
  LogAnalysisChartDialogComponent,
  Chart
} from "../log-analysis-chart-dialog/log-analysis-chart-dialog.component";
import {LongLoadingDetector} from "../long-loading-detector";
import {FilesApiService} from "../api/files-api.service";

@Component({
  selector: 'app-log-analysis',
  templateUrl: './log-analysis.component.html',
  styleUrls: ['./log-analysis.component.css']
})
export class LogAnalysisComponent implements OnInit, OnDestroy {
  private static readonly LONG_LOADING_FLAG = 'logs';
  private static readonly CHART_FILE_EXTENSION = 'chart';
  private static readonly CHART_FILE_PATH = '/resources/.config';


  longLoading = new LongLoadingDetector();
  logSubscription: Subscription = null;
  selectedProject: ProjectInfo = null;
  selectedStageId: string = null;
  latestStageId: string;

  projectHistory: ExecutionGroupInfo[] = [];
  logs: LogEntry[] = [];
  charts: Chart[] = [];

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
    private projectApi: ProjectApiService,
    private filesApi: FilesApiService,
  ) {
  }

  ngOnInit(): void {
    this.projectApi.getProjectHistory(this.selectedProject.id).then(result => {
      this.projectHistory = result;
      this.latestStageId = this.getLatestStageId(result);
    });
    this.selectStage(this.selectedStageId);
    this.downloadCharts();
  }

  ngOnDestroy() {
    if (this.logSubscription) {
      this.logSubscription.unsubscribe();
      this.logSubscription = null;
    }
  }

  selectStage(stageId: string) {
    if (this.logSubscription != null) {
      this.logSubscription.unsubscribe();
    }
    if (stageId == null) {
      stageId = this.latestStageId;
    }
    this.logs = [];
    this.selectedStageId = stageId;
    this.subscribeLogs(this.selectedProject.id, stageId);
  }

  private subscribeLogs(projectId: string, stageId = ProjectApiService.LOGS_LATEST) {
    if (stageId == null) {
      stageId = ProjectApiService.LOGS_LATEST;
    }
    this.longLoading.raise(LogAnalysisComponent.LONG_LOADING_FLAG);
    this.logSubscription = this.projectApi.watchLogs(projectId, (logs) => {
      this.longLoading.clear(LogAnalysisComponent.LONG_LOADING_FLAG);
      if (logs?.length > 0) {
        this.logs.push(...logs);
      } else {
        this.logs = [];
      }
    }, stageId);
  }

  addNewChart() {
    let chart: Chart = new Chart();
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

  isLongLoading(): boolean {
    return this.longLoading.isLongLoading();
  }

  getLatestStageId(history: ExecutionGroupInfo[]): string {
    return this.filterHistory(history).slice(-1)[0].id;
  }

  filterHistory(history: ExecutionGroupInfo[]): ExecutionGroupInfo[] {
    return history.filter(entry => !entry.configureOnly)
  }

  private uploadChart(filename: string, chart: Chart) {
    let file = new File(
      [JSON.stringify(chart, null, "\t")],
      `${filename}.${(LogAnalysisComponent.CHART_FILE_EXTENSION)}`,
      {type: "application/json"},);
    this.filesApi.uploadFile(LogAnalysisComponent.CHART_FILE_PATH, file).toPromise().then(result => {
      console.log(result);
    });
  }

  private downloadCharts() {
    let filepath = `${LogAnalysisComponent.CHART_FILE_PATH}/default.charts`;
    this.filesApi.getFile(filepath).toPromise().then(result => {
      for (let [key, value] of Object.entries(result)) {
        console.log(key, value);
        this.downloadChart(value);
      }
    })
  }

  private downloadChart(filename: string) {
    let filepath = `${LogAnalysisComponent.CHART_FILE_PATH}/${filename}`;
    this.filesApi.getFile(filepath).toPromise().then(result => {
      let chart = new Chart();
      Object.assign(chart, result);
      this.charts.push(chart);
    })
  }
}
