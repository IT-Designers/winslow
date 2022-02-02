import {Component, Input, OnInit} from '@angular/core';
import {ExecutionGroupInfo, ProjectApiService, ProjectInfo} from "../api/project-api.service";
import {MatDialog} from '@angular/material/dialog';
import {LogAnalysisChartDialogComponent} from "../log-analysis-chart-dialog/log-analysis-chart-dialog.component";
import {LongLoadingDetector} from "../long-loading-detector";
import {FileInfo, FilesApiService} from "../api/files-api.service";
import {ChartData, ChartSettings} from "../log-analysis-chart/log-analysis-chart.component";

class LogChart {
  settings = new ChartSettings();
  file = "logfile.csv";
  formatter = "\"$TIMESTAMP;$0;$1;$2;$3;$SOURCE;$ERROR;!;$WINSLOW_PIPELINE_ID\""
  xVariable = "";
  yVariable = "$1";
}

interface CsvFile {
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
  private static readonly PATH_TO_CHARTS = '/resources/.config/charts';
  private static readonly PATH_TO_WORKSPACES = '/workspaces';

  longLoading = new LongLoadingDetector();
  selectedProject: ProjectInfo = null;
  selectedStageId: string = null;
  latestStageId: string = null;

  projectHistory: ExecutionGroupInfo[] = [];
  charts: LogChart[] = [];
  csvFiles: CsvFile[] = [];

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
    this.longLoading.raise(LogAnalysisComponent.LONG_LOADING_HISTORY_FLAG);

    this.projectApi.getProjectHistory(this.selectedProject.id).then(projectHistory => {
      this.projectHistory = projectHistory;
      this.latestStageId = this.getLatestStageId(projectHistory);
      this.selectStage(this.selectedStageId);
      this.loadCharts();

      this.longLoading.clear(LogAnalysisComponent.LONG_LOADING_HISTORY_FLAG);
    });
  }

  isLongLoading(): boolean {
    return this.longLoading.isLongLoading();
  }

  isLatestStage(): boolean {
    return this.selectedStageId == this.latestStageId;
  }

  selectStage(stageId: string) {
    if (stageId == null) {
      stageId = this.latestStageId;
    }
    this.selectedStageId = stageId;
    console.log(`Selected stage ${stageId}`);
    this.loadCsv();
  }

  getLatestStageId(history: ExecutionGroupInfo[]): string {
    return this.filterHistory(history).slice(-1)[0].id;
  }

  filterHistory(history: ExecutionGroupInfo[]): ExecutionGroupInfo[] {
    return history.filter(entry => !entry.configureOnly)
  }

  addChart(chart: LogChart = new LogChart()) {
    this.charts.push(chart);
  }

  removeChart(chartIndex: number) {
    this.charts.splice(chartIndex, 1);
  }

  getChartGraphData(chart: LogChart): ChartData {
    const csvFile = this.csvFiles.find(csvFile => csvFile.name == chart.file);
    if (!csvFile) return [];

    const formatterVariables = chart.formatter.split(";");
    const xIndex = formatterVariables.findIndex(variable => variable == chart.xVariable);
    const yIndex = formatterVariables.findIndex(variable => variable == chart.yVariable);

    const chartData = [];

    csvFile.content.forEach((line, index) => {
      chartData.push([line[xIndex] ?? index, line[yIndex] ?? index])
    })

    return chartData;
  }

  openEditChartDialog(chartIndex: number) {
    const dialogRef = this.dialog.open(LogAnalysisChartDialogComponent, {
      data: {
        chart: this.charts[chartIndex],
        //logs: this.logs
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      console.log(result)
    })
  }

  private loadCharts() {
    this.longLoading.raise(LogAnalysisComponent.LONG_LOADING_CHARTS_FLAG);

    LogAnalysisComponent.getChartFilenames('default', this.filesApi)
      .then(filenames => LogAnalysisComponent.getChartFiles(filenames, this.filesApi))
      .then(charts => {
        this.charts = [...charts, ...this.charts];
        this.longLoading.clear(LogAnalysisComponent.LONG_LOADING_CHARTS_FLAG)
      })
  }

  private static getChartFilenames(filename: string, filesApi: FilesApiService): Promise<string[]> {
    const filepath = `${LogAnalysisComponent.PATH_TO_CHARTS}/${filename}.charts`;
    return filesApi.getFile(filepath).toPromise().then(result => {
      return JSON.parse(result)
    }) as Promise<string[]>;
  }

  private static getChartFiles(filenames, filesApi: FilesApiService): Promise<LogChart[]> {
    const promises: Promise<LogChart>[] = [];
    for (const filename of filenames as string[]) {
      const filepath = `${LogAnalysisComponent.PATH_TO_CHARTS}/${filename}.${LogAnalysisComponent.CHART_FILE_EXTENSION}`;
      promises.push(filesApi.getFile(filepath).toPromise().then(text => JSON.parse(text) as LogChart));
    }
    return Promise.all(promises);
  }

  private loadCsv() {
    this.longLoading.raise(LogAnalysisComponent.LONG_LOADING_CSV_FLAG);

    LogAnalysisComponent.getCsvFilenames(this.projectDirName(), this.stageDirName(), this.filesApi)
      .then(files => LogAnalysisComponent.getCsvFiles(files, this.filesApi))
      .then(csvFiles => {
        this.csvFiles = csvFiles;
        this.longLoading.clear(LogAnalysisComponent.LONG_LOADING_CSV_FLAG)
      })
  }

  private static getCsvFilenames(projectDirName, stageDirName, filesApi: FilesApiService) {
    const filepath = `${LogAnalysisComponent.PATH_TO_WORKSPACES}/${projectDirName}/${stageDirName}`;
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

  private projectDirName() {
    return this.selectedProject.id;
  }

  private stageDirName() {
    return this.selectedStageId.slice(this.selectedProject.id.length + 1);
  }

  uploadCharts() {
    const filenames = [];
    this.charts.forEach((chart, index) => {
      const filename = `chart_${index}`;
      this.uploadChart(filename, chart);
      filenames.push(filename);
    })
    const file = new File(
      [JSON.stringify(filenames, null, "\t")],
      `default.charts`,
      {type: "application/json"},);
    this.filesApi.uploadFile(LogAnalysisComponent.PATH_TO_CHARTS, file).toPromise().then(result => {
      console.log(result);
    });
  }

  private uploadChart(filename: string, chart: LogChart) {
    const file = new File(
      [JSON.stringify(chart, null, "\t")],
      `${filename}.${(LogAnalysisComponent.CHART_FILE_EXTENSION)}`,
      {type: "application/json"},);
    this.filesApi.uploadFile(LogAnalysisComponent.PATH_TO_CHARTS, file).toPromise().then(result => {
      console.log(result);
    });
  }
}
