import {Component, Input, OnInit} from '@angular/core';
import {ExecutionGroupInfo, ProjectApiService, ProjectInfo} from "../api/project-api.service";
import {MatDialog} from '@angular/material/dialog';
import {LogAnalysisChartDialogComponent} from "../log-analysis-chart-dialog/log-analysis-chart-dialog.component";
import {LongLoadingDetector} from "../long-loading-detector";
import {FileInfo, FilesApiService} from "../api/files-api.service";
import {ChartData, ChartSettings} from "../log-analysis-chart/log-analysis-chart.component";
import {LogAnalysisManageChartsDialogComponent} from "../log-analysis-manage-charts-dialog/log-analysis-manage-charts-dialog.component";

export interface ChartDialogData {
  chart: LogChart;
  csvFiles: CsvFile[];
}

export class LogChart {
  settings = new ChartSettings();
  file = "logfile.csv";
  formatter = "\"$TIMESTAMP;$0;$1;$2;$3;$SOURCE;$ERROR;!;$WINSLOW_PIPELINE_ID\""
  xVariable = "";
  yVariable = "$1";
  displayAmount: null | number = null;

  static dataFromFiles(chart: LogChart, csvFiles: CsvFile[]): ChartData {
    const csvFile = csvFiles.find(csvFile => csvFile.name == chart.file);
    if (!csvFile) return [];

    const formatterVariables = chart.formatter.split(";");
    const xIndex = formatterVariables.findIndex(variable => variable == chart.xVariable);
    const yIndex = formatterVariables.findIndex(variable => variable == chart.yVariable);

    const chartData = [];

    let index = 0;
    if (chart.displayAmount > 0) {
      index = csvFile.content.length - chart.displayAmount;
    }
    for (index; index < csvFile.content.length; index++) {
      const line = csvFile.content[index];
      chartData.push([line[xIndex] ?? index, line[yIndex] ?? index])
    }

    return chartData;
  }
}

export interface CsvFile {
  name: string;
  content: [number][];
}

@Component({
  selector: 'app-log-analysis',
  templateUrl: './log-analysis.component.html',
  styleUrls: ['./log-analysis.component.css']
})
export class LogAnalysisComponent implements OnInit {
  private static readonly LONG_LOADING_HISTORY_FLAG = 'history';
  private static readonly LONG_LOADING_CHARTS_FLAG = 'charts';
  private static readonly LONG_LOADING_CSV_FLAG = 'csv';

  private static readonly CHART_FILE_EXTENSION = 'chart';
  private static readonly REGISTRY_FILE_EXTENSION = 'charts';
  private static readonly PATH_TO_CHARTS = '/resources/.config/charts';
  private static readonly PATH_TO_WORKSPACES = '/workspaces';

  selectedProject: ProjectInfo = null;
  selectedStageId: string = null;

  longLoading = new LongLoadingDetector();

  selectedExecutionGroup: ExecutionGroupInfo = null;
  latestExecutionGroup: ExecutionGroupInfo = null;

  projectHistory: ExecutionGroupInfo[] = [];
  charts: LogChart[] = [];
  csvFiles: CsvFile[] = [];
  historyEntriesToCompare: ExecutionGroupInfo[] = [];

  @Input()
  set project(project: ProjectInfo) {
    this.selectedProject = project;

    this.longLoading.raise(LogAnalysisComponent.LONG_LOADING_HISTORY_FLAG);

    this.projectApi.getProjectHistory(this.selectedProject.id).then(projectHistory => {
      this.projectHistory = projectHistory;
      this.latestExecutionGroup = this.getLatestExecutionGroup();

      this.autoSelectExecutionGroup();
      console.log({projectHistory, project})

      this.loadCharts();
      this.longLoading.clear(LogAnalysisComponent.LONG_LOADING_HISTORY_FLAG);
    });
  }

  @Input()
  set selectedStage(id: string) {
    this.selectedStageId = id;

    this.autoSelectExecutionGroup();
  }

  constructor(
    private dialog: MatDialog,
    private projectApi: ProjectApiService,
    private filesApi: FilesApiService,
  ) {
  }

  ngOnInit(): void {
  }

  isLongLoading(): boolean {
    return this.longLoading.isLongLoading();
  }

  isLatestStage(): boolean {
    return this.selectedExecutionGroup == this.latestExecutionGroup;
  }

  autoSelectExecutionGroup() {
    if (this.selectedStageId && this.projectHistory) {
      const executionGroup = this.projectHistory.find(entry => entry.id == this.selectedStageId)
      this.selectExecutionGroup(executionGroup)
    } else if (this.projectHistory) {
      this.selectExecutionGroup(this.getLatestExecutionGroup())
    }
  }

  selectExecutionGroup(executionGroup: ExecutionGroupInfo) {
    if (executionGroup == null) {
      this.selectedExecutionGroup = this.latestExecutionGroup;
    }
    this.selectedExecutionGroup = executionGroup;
    console.log(`Selected execution group ${executionGroup.id}`);
    this.loadCsv();
  }

  getLatestExecutionGroup(): ExecutionGroupInfo {
    return this.filterHistory().slice(-1)[0];
  }

  isComparing() {
    return this.historyEntriesToCompare.length > 0;
  }

  startComparing() {

  }

  stopComparing() {

  }

  filterHistory(): ExecutionGroupInfo[] {
    const history: ExecutionGroupInfo[] = this.projectHistory;
    return history.filter(entry => !entry.configureOnly);
  }

  addChart(chart: LogChart = new LogChart()) {
    this.charts.push(chart);
    this.openEditChartDialog(chart);
  }

  removeChart(chartIndex: number) {
    this.charts.splice(chartIndex, 1);
    this.uploadCharts();
  }

  openEditChartDialog(chart: LogChart) {
    const dialogData: ChartDialogData = {
      chart: chart,
      csvFiles: this.csvFiles,
    }

    const dialogRef = this.dialog.open(LogAnalysisChartDialogComponent, {
      data: dialogData,
    });

    dialogRef.afterClosed().subscribe(result => {
      console.log(result);
      this.uploadCharts();
    })
  }

  openManageChartsDialog() {
    const dialogRef = this.dialog.open(LogAnalysisManageChartsDialogComponent, {
      data: {
        //todo
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      console.log(result)
    })
  }

  historyEntryLabel(executionGroup: ExecutionGroupInfo): string {
    const date = new Date(executionGroup.getMostRecentStartOrFinishTime()).toLocaleString();
    const name = executionGroup.stageDefinition.name;
    return `${date} · ${name}`
  }

  private loadCharts() {
    this.longLoading.raise(LogAnalysisComponent.LONG_LOADING_CHARTS_FLAG);
    this.charts = [];

    LogAnalysisComponent.getChartFilenames(this.selectedProject.id, this.filesApi)
      .then(filenames => LogAnalysisComponent.getChartFiles(filenames, this.filesApi))
      .then(charts => this.charts = charts)
      .finally(() => this.longLoading.clear(LogAnalysisComponent.LONG_LOADING_CHARTS_FLAG))
  }

  private static getChartFilenames(filename: string, filesApi: FilesApiService): Promise<string[]> {
    const filepath = `${LogAnalysisComponent.PATH_TO_CHARTS}/${filename}.${LogAnalysisComponent.REGISTRY_FILE_EXTENSION}`;
    return filesApi.getFile(filepath).toPromise().then(result => {
      return JSON.parse(result)
    }) as Promise<string[]>;
  }

  private static getChartFiles(filenames, filesApi: FilesApiService): Promise<LogChart[]> {
    const promises: Promise<LogChart>[] = [];
    for (const filename of filenames as string[]) {
      const filepath = `${LogAnalysisComponent.PATH_TO_CHARTS}/${filename}`;
      promises.push(filesApi.getFile(filepath).toPromise().then(text => LogAnalysisComponent.parseChart(text)));
    }
    return Promise.all(promises);
  }

  private static parseChart(text: string) {
    const chart = new LogChart();
    Object.assign(chart, JSON.parse(text));
    return chart;
  }

  private loadCsv() {
    this.longLoading.raise(LogAnalysisComponent.LONG_LOADING_CSV_FLAG);
    this.csvFiles = [];

    LogAnalysisComponent.getCsvFilenames(this.workspaceDir(), this.filesApi)
      .then(files => LogAnalysisComponent.getCsvFiles(files, this.filesApi))
      .then(csvFiles => this.csvFiles = csvFiles)
      .finally(() => this.longLoading.clear(LogAnalysisComponent.LONG_LOADING_CSV_FLAG))
  }

  private static getCsvFilenames(workspace: string, filesApi: FilesApiService) {
    const filepath = `${this.PATH_TO_WORKSPACES}/${workspace}`
    return filesApi.listFiles(filepath)
  }

  private static getCsvFiles(files: FileInfo[], filesApi: FilesApiService): Promise<CsvFile[]> {
    const promises: Promise<CsvFile>[] = [];
    for (const {name, path} of files) {
      promises.push(filesApi.getFile(path).toPromise().then(text => LogAnalysisComponent.parseCSV(name, text)));
    }
    return Promise.all(promises);
  }

  private static parseCSV(name: string, sourceText: string): CsvFile {
    const lines = sourceText.split('\n');
    const content = [];
    lines.forEach(line => {
      if (line.trim().length != 0) { // ignore empty lines
        content.push(line.split(';'));
      }
    })

    return {
      name: name,
      content: content,
    }
  }

  uploadCharts() {
    const filenames = [];
    this.charts.forEach((chart, index) => {
      const filename = `${this.selectedProject.pipelineDefinition.id}.${index}.${(LogAnalysisComponent.CHART_FILE_EXTENSION)}`;
      this.uploadChart(filename, chart);
      filenames.push(filename);
    })
    const file = new File(
      [JSON.stringify(filenames, null, "\t")],
      `${this.selectedProject.pipelineDefinition.id}.${LogAnalysisComponent.REGISTRY_FILE_EXTENSION}`,
      {type: "application/json"},);
    this.filesApi.uploadFile(LogAnalysisComponent.PATH_TO_CHARTS, file).toPromise().then(result => {
      console.log(result);
    });
  }

  private uploadChart(filename: string, chart: LogChart) {
    const file = new File(
      [JSON.stringify(chart, null, "\t")], filename, {type: "application/json"},);
    this.filesApi.uploadFile(LogAnalysisComponent.PATH_TO_CHARTS, file).toPromise().then(result => {
      console.log(result);
    });
  }

  private workspaceDir() {
    return this.selectedExecutionGroup.stages[0].workspace;
  }

  getChartData(chart: LogChart) {
    return LogChart.dataFromFiles(chart, this.csvFiles);
  }
}
