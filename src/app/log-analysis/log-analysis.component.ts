import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {ExecutionGroupInfo, ProjectApiService, ProjectInfo} from "../api/project-api.service";
import {MatDialog} from '@angular/material/dialog';
import {LogAnalysisChartDialogComponent,} from "../log-analysis-chart-dialog/log-analysis-chart-dialog.component";
import {LongLoadingDetector} from "../long-loading-detector";
import {FilesApiService} from "../api/files-api.service";
import {ChartSettings} from "../log-analysis-chart/log-analysis-chart.component";

export class LogChart {
  settings = new ChartSettings();
  file = "logfile.csv";
  formatter = "\"$TIMESTAMP;$0;$1;$2;$3;$SOURCE;$ERROR;!;$WINSLOW_PIPELINE_ID\""
  xVariable = "";
  yVariable = "$1";
}

@Component({
  selector: 'app-log-analysis',
  templateUrl: './log-analysis.component.html',
  styleUrls: ['./log-analysis.component.css']
})
export class LogAnalysisComponent implements OnInit, OnDestroy {
  private static readonly LONG_LOADING_FLAG = 'charts';
  private static readonly CHART_FILE_EXTENSION = 'chart';
  private static readonly CHART_FILE_PATH = '/resources/.config/charts';

  longLoading = new LongLoadingDetector();
  selectedProject: ProjectInfo = null;
  selectedStageId: string = null;
  latestStageId: string;

  projectHistory: ExecutionGroupInfo[] = [];
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
  }

  addChart(chart: LogChart = new LogChart()) {
    this.charts.push(chart);
  }

  removeChart(chartIndex: number) {
    this.charts.splice(chartIndex, 1);
  }

  openEditChartDialog(chartIndex: number) {
    let dialogRef = this.dialog.open(LogAnalysisChartDialogComponent, {
      data: {
        chart: this.charts[chartIndex],
        //logs: this.logs
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      console.log(result)
    })
  }

  private downloadCharts() {
    this.longLoading.raise(LogAnalysisComponent.LONG_LOADING_FLAG);
    let filepath = `${LogAnalysisComponent.CHART_FILE_PATH}/default.charts`;

    this.filesApi.getFile(filepath).toPromise().then(filenames => {
      let promises: Promise<Object>[] = [];
      for (let filename of filenames as string[]) {
        promises.push(this.downloadChart(filename));
      }
      Promise.all(promises).then(_ => this.longLoading.clear(LogAnalysisComponent.LONG_LOADING_FLAG))
    })
  }

  private downloadChart(filename: string): Promise<Object> {
    let filepath = `${LogAnalysisComponent.CHART_FILE_PATH}/${filename}.${LogAnalysisComponent.CHART_FILE_EXTENSION}`;
    let promise = this.filesApi.getFile(filepath).toPromise()
    promise.then(result => {
      this.addChart(result as LogChart);
    })
    return promise;
  }

  uploadCharts() {
    let filenames = [];
    this.charts.forEach((chart, index) => {
      let filename = `chart_${index}`;
      this.uploadChart(filename, chart);
      filenames.push(filename);
    })
    let file = new File(
      [JSON.stringify(filenames, null, "\t")],
      `default.charts`,
      {type: "application/json"},);
    this.filesApi.uploadFile(LogAnalysisComponent.CHART_FILE_PATH, file).toPromise().then(result => {
      console.log(result);
    });
  }

  private uploadChart(filename: string, chart: LogChart) {
    let file = new File(
      [JSON.stringify(chart, null, "\t")],
      `${filename}.${(LogAnalysisComponent.CHART_FILE_EXTENSION)}`,
      {type: "application/json"},);
    this.filesApi.uploadFile(LogAnalysisComponent.CHART_FILE_PATH, file).toPromise().then(result => {
      console.log(result);
    });
  }

  selectStage(stageId: string) {
    if (stageId == null) {
      stageId = this.latestStageId;
    }
    this.selectedStageId = stageId;
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
}
