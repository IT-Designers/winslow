import {Component, Input, OnInit} from '@angular/core';
import {ExecutionGroupInfo, ProjectApiService, ProjectInfo} from "../api/project-api.service";
import {MatDialog} from '@angular/material/dialog';
import {LogAnalysisChartDialogComponent} from "../log-analysis-chart-dialog/log-analysis-chart-dialog.component";
import {LongLoadingDetector} from "../long-loading-detector";
import {FileInfo, FilesApiService} from "../api/files-api.service";
import {ChartData, ChartDialogData, CsvFile, LogChartDefinition} from "./log-chart-definition";

type Stage = {
  id: string;
  executionGroup: ExecutionGroupInfo;
  csvFiles: CsvFile[]
}

class LogChartInstance {
  definition: LogChartDefinition;
  data: ChartData;

  constructor() {
    this.definition = new LogChartDefinition();
  }

  updateData(stages: Stage[]) {
    this.data = [];
    stages.forEach(stage => {
      this.data.push(LogChartDefinition.getDataSeries(this.definition, stage.csvFiles))
    })
  }
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

  longLoading = new LongLoadingDetector();

  selectedProject: ProjectInfo = null;
  latestExecutionGroup: ExecutionGroupInfo = null;
  projectHistory: ExecutionGroupInfo[] = [];
  filteredHistory: ExecutionGroupInfo[] = [];
  charts: LogChartInstance[] = [];

  stageToDisplay: Stage = {
    id: null,
    executionGroup: null,
    csvFiles: [],
  }

  stagesToCompare: Stage[] = [];

  @Input()
  set project(project: ProjectInfo) {
    this.selectedProject = project;

    this.longLoading.raise(LogAnalysisComponent.LONG_LOADING_HISTORY_FLAG);

    this.projectApi.getProjectHistory(this.selectedProject.id).then(projectHistory => {
      this.projectHistory = projectHistory;
      this.filteredHistory = projectHistory.filter(entry => !entry.configureOnly);
      this.latestExecutionGroup = this.getLatestExecutionGroup();

      this.autoSelectStage();

      this.loadCharts();
      this.longLoading.clear(LogAnalysisComponent.LONG_LOADING_HISTORY_FLAG);
    });
  }

  @Input()
  set selectedStage(id: string) {
    this.stageToDisplay.id = id;

    this.autoSelectStage();
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

  isLatestStage(stage: Stage): boolean {
    return stage.executionGroup == this.latestExecutionGroup;
  }

  autoSelectStage() {
    if (this.stageToDisplay.id && this.projectHistory) {
      const executionGroup = this.projectHistory.find(entry => entry.id == this.stageToDisplay.id)
      this.updateStage(this.stageToDisplay, executionGroup)
    } else if (this.projectHistory) {
      this.updateStage(this.stageToDisplay, this.getLatestExecutionGroup())
    }
  }

  updateStage(stage: Stage, executionGroup: ExecutionGroupInfo) {
    if (executionGroup == null) {
      executionGroup = this.latestExecutionGroup;
    }
    stage.executionGroup = executionGroup;
    console.log(`Selected execution group ${executionGroup.id}`);
    this.loadCsv(stage);
  }

  getLatestExecutionGroup(): ExecutionGroupInfo {
    return this.filteredHistory.slice(-1)[0];
  }

  addStageToCompare() {
    let stage: Stage = {
      csvFiles: [],
      executionGroup: undefined,
      id: ""
    }
    this.updateStage(stage, this.latestExecutionGroup);
    this.stagesToCompare.push(stage);
  }

  removeStageToCompare(stageIndex: number) {
    this.stagesToCompare.splice(stageIndex, 1);
  }

  addChart(chart: LogChartInstance = new LogChartInstance) {
    this.charts.push(chart);
    this.openEditChartDialog(chart.definition);
  }

  removeChart(chartIndex: number) {
    this.charts.splice(chartIndex, 1);
    this.uploadCharts();
  }

  openEditChartDialog(chart: LogChartDefinition) {
    const dialogData: ChartDialogData = {
      chart: chart,
      csvFiles: this.stageToDisplay.csvFiles,
    }

    const dialogRef = this.dialog.open(LogAnalysisChartDialogComponent, {
      data: dialogData,
    });

    dialogRef.afterClosed().subscribe(result => {
      console.log(result);
      this.uploadCharts();
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
      .then(chartDefinitions => LogAnalysisComponent.getChartInstances(chartDefinitions))
      .then(chartInstances => this.charts = chartInstances)
      .finally(() => {
        this.updateAllChartData();
        this.longLoading.clear(LogAnalysisComponent.LONG_LOADING_CHARTS_FLAG);
      })
  }

  private static getChartInstances(chartDefinitions: LogChartDefinition[]) {
    return chartDefinitions.map(chartDefinition => {
      let chartInstance = new LogChartInstance()
      chartInstance.definition = chartDefinition;
      return chartInstance;
    });
  }

  private static getChartFilenames(filename: string, filesApi: FilesApiService): Promise<string[]> {
    const filepath = `${LogAnalysisComponent.PATH_TO_CHARTS}/${filename}.${LogAnalysisComponent.REGISTRY_FILE_EXTENSION}`;
    return filesApi.getFile(filepath).toPromise().then(result => {
      return JSON.parse(result)
    }) as Promise<string[]>;
  }

  private static getChartFiles(filenames, filesApi: FilesApiService): Promise<LogChartDefinition[]> {
    const promises: Promise<LogChartDefinition>[] = [];
    for (const filename of filenames as string[]) {
      const filepath = `${LogAnalysisComponent.PATH_TO_CHARTS}/${filename}`;
      promises.push(filesApi.getFile(filepath).toPromise().then(text => LogAnalysisComponent.parseChart(text)));
    }
    return Promise.all(promises);
  }

  private static parseChart(text: string) {
    const chart = new LogChartDefinition();
    Object.assign(chart, JSON.parse(text));
    return chart;
  }

  private loadCsv(stage: Stage) {
    this.longLoading.raise(LogAnalysisComponent.LONG_LOADING_CSV_FLAG);
    const workspaceDir = LogAnalysisComponent.workspaceDir(stage);
    stage.csvFiles = [];

    LogAnalysisComponent.getCsvFilenames(workspaceDir, this.filesApi)
      .then(files => LogAnalysisComponent.getCsvFiles(files, this.filesApi))
      .then(csvFiles => stage.csvFiles = csvFiles)
      .finally(() => {
        this.updateAllChartData();
        this.longLoading.clear(LogAnalysisComponent.LONG_LOADING_CSV_FLAG);
      })
  }

  private static getCsvFilenames(workspaceDir: string, filesApi: FilesApiService) {
    const filepath = `${this.PATH_TO_WORKSPACES}/${workspaceDir}`
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
      this.uploadChart(filename, chart.definition);
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

  private uploadChart(filename: string, chart: LogChartDefinition) {
    const file = new File(
      [JSON.stringify(chart, null, "\t")], filename, {type: "application/json"},);
    this.filesApi.uploadFile(LogAnalysisComponent.PATH_TO_CHARTS, file).toPromise().then(result => {
      console.log(result);
    });
  }

  private static workspaceDir(stage: Stage) {
    return stage.executionGroup.stages[0].workspace;
  }

  updateAllChartData() {
    const stages = [this.stageToDisplay, ...this.stagesToCompare];
    this.charts.forEach(chart => {
      chart.updateData(stages)
    })
  }
}
