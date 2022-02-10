import {Component, Input, OnInit} from '@angular/core';
import {ExecutionGroupInfo, ProjectApiService, ProjectInfo, StageInfo, State} from "../api/project-api.service";
import {MatDialog} from '@angular/material/dialog';
import {LogAnalysisChartDialogComponent} from "../log-analysis-chart-dialog/log-analysis-chart-dialog.component";
import {LongLoadingDetector} from "../long-loading-detector";
import {FileInfo, FilesApiService} from "../api/files-api.service";
import {
  AnalysisDisplaySettings,
  ChartDataSeries,
  ChartDialogData,
  CsvFile,
  LogChartDefinition,
  StageCsvInfo
} from "./log-chart-definition";
import {LogAnalysisSettingsDialogComponent} from "../log-analysis-settings-dialog/log-analysis-settings-dialog.component";
import {PipelineApiService, PipelineInfo} from "../api/pipeline-api.service";
import {BehaviorSubject} from "rxjs";

class LogChart {
  definition: LogChartDefinition;
  dataSubject: BehaviorSubject<ChartDataSeries[]>;
  filename: string;

  constructor(id?: string) {
    this.definition = new LogChartDefinition();
    this.filename = id ?? LogChart.generateUniqueId();
    this.dataSubject = new BehaviorSubject<ChartDataSeries[]>([]);
  }

  refreshDisplay(stages: StageCsvInfo[], displaySettings: AnalysisDisplaySettings) {
    let data = stages.map(stage => LogChartDefinition.getDataSeries(this.definition, stage.csvFiles, displaySettings));
    this.dataSubject.next(data);
  }

  private static generateUniqueId() {
    return `${Date.now().toString().slice(5)}${Math.random().toString().slice(2)}.chart`;
  }
}

export interface CsvFile {
  name: string;
  content: [number][];
}

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
  private static readonly LONG_LOADING_PIPELINES_FLAG = 'pipelines';

  private static readonly PATH_TO_CHARTS = '/resources/.config/charts';
  private static readonly PATH_TO_WORKSPACES = '/workspaces';

  longLoading = new LongLoadingDetector();
  hasSelectableStages = true;

  probablyPipelineId = null;

  selectedProject: ProjectInfo = null;
  projectHistory: ExecutionGroupInfo[] = [];
  latestStage: StageInfo = null;
  selectableStages: StageInfo[] = [];
  charts: LogChart[] = [];

  stageToDisplay: Stage = {
    id: null,
    executionGroup: null,
    csvFiles: [],
  }

  stagesToCompare: Stage[] = [];

  stageToDisplay: StageCsvInfo = {
    id: null,
    stage: null,
    csvFiles: [],
  }

  stagesToCompare: StageCsvInfo[] = [];

  displaySettings: AnalysisDisplaySettings = {
    enableEntryLimit: false,
    entryLimit: 50
  }

  @Input()
  set project(project: ProjectInfo) {
    this.selectedProject = project;

    this.longLoading.raise(LogAnalysisComponent.LONG_LOADING_HISTORY_FLAG);
    const projectPromise = this.projectApi.getProjectHistory(this.selectedProject.id)
      .then(projectHistory => this.loadStagesFromHistory(projectHistory))
      .finally(() => this.longLoading.clear(LogAnalysisComponent.LONG_LOADING_HISTORY_FLAG))

    this.longLoading.raise(LogAnalysisComponent.LONG_LOADING_PIPELINES_FLAG);
    const pipelinePromise = this.pipelineApi.getPipelineDefinitions()
      .then(pipelines => this.findProjectPipeline(pipelines))
      .finally(() => this.longLoading.clear(LogAnalysisComponent.LONG_LOADING_PIPELINES_FLAG))

    Promise.all([projectPromise, pipelinePromise]).then(
      () => this.loadCharts()
    )
  }

  @Input()
  set selectedStage(id: string) {
    if (id == null) {
      return;
    }

    this.stageToDisplay.id = id;

    this.autoSelectStage();
  }

  constructor(
    private dialog: MatDialog,
    private projectApi: ProjectApiService,
    private pipelineApi: PipelineApiService,
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

  selectStage(stageId: string) {
    if (stageId == null) {
      stageId = this.latestStageId;
    }
    this.selectedStageId = stageId;
  }

  lineId(index, log): string {
    return log.stageId + log.line;
  }

  isLongLoading(): boolean {
    return this.longLoading.isLongLoading();
  }

  stageLabel(stage: StageInfo): string {
    const dateValue = stage.finishTime ?? stage.startTime;
    const dateString = new Date(dateValue).toLocaleString();

    const name = stage.id.slice(this.selectedProject.id.length + 1);

    return `${dateString} · ${name}`
  }

  updateStageCsvInfo(stageCsvInfo: StageCsvInfo, stage: StageInfo) {
    if (stage == null) {
      stage = this.latestStage;
    }
    stageCsvInfo.stage = stage;
    console.log(`Selected stage ${stage.id}`);
    this.loadCsvFiles(stageCsvInfo);
  }

  isLatestStage(stageCsvInfo: StageCsvInfo): boolean {
    return stageCsvInfo.stage == this.latestStage;
  }

  getLatestStage(): StageInfo {
    return this.selectableStages.slice(-1)[0];
  }

  addStageToCompare() {
    let stageCsvInfo: StageCsvInfo = {
      csvFiles: [],
      stage: undefined,
      id: ""
    }
    this.updateStageCsvInfo(stageCsvInfo, this.latestStage);
    this.stagesToCompare.push(stageCsvInfo);
  }

  removeStageToCompare(stageIndex: number) {
    this.stagesToCompare.splice(stageIndex, 1);
    this.refreshAllCharts();
  }

  createChart() {
    const chart = new LogChart();
    this.charts.push(chart);
    this.openEditChartDialog(chart);
  }

  removeChart(chartIndex: number) {
    let chart = this.charts[chartIndex];
    if (!chart) {
      throw "Chart index is out of range!";
    }
    this.deleteChart(chart);
    this.charts.splice(chartIndex, 1);
  }

  refreshAllCharts() {
    const stages = this.stagesToDrawGraphsFor();
    this.charts.forEach(chart => {
      chart.refreshDisplay(stages, this.displaySettings)
    })
  }

  openEditChartDialog(chart: LogChart) {
    const dialogData: ChartDialogData = {
      chartDefinition: chart.definition,
      dataSource: chart.dataSubject,
      stages: this.stagesToDrawGraphsFor(),
    }

    const dialogRef = this.dialog.open(LogAnalysisChartDialogComponent, {
      data: dialogData,
    });

    dialogRef.afterClosed().subscribe(_ => {
      chart.refreshDisplay(this.stagesToDrawGraphsFor(), this.displaySettings);
      this.saveCharts();
    })
  }

  private loadStagesFromHistory(projectHistory) {
    this.projectHistory = projectHistory;
    this.selectableStages = this.getSelectableStages(projectHistory);
    this.latestStage = this.getLatestStage();

    this.hasSelectableStages = this.selectableStages.length > 0;

    if (this.hasSelectableStages) {
      this.autoSelectStage();
    }
  }

  private getSelectableStages(projectHistory: ExecutionGroupInfo[]) {
    let stages: StageInfo[] = []

    projectHistory.forEach(executionGroup => {

      if (executionGroup.configureOnly) {
        return;
      }

      const state = executionGroup.getMostRelevantState();
      if (state == State.Failed || state == State.Skipped) {
        return;
      }

      if (executionGroup.workspaceConfiguration.sharedWithinGroup) {
        stages.push(executionGroup.stages[0]);
      } else {
        stages.push(...executionGroup.stages);
      }
    })

    return stages;
  }

  private stagesToDrawGraphsFor() {
    return [this.stageToDisplay, ...this.stagesToCompare];
  }

  private autoSelectStage() {
    this.stagesToCompare = [];
    if (this.stageToDisplay.id && this.projectHistory) {
      const stage = this.selectableStages.find(entry => entry.id == this.stageToDisplay.id)
      this.updateStageCsvInfo(this.stageToDisplay, stage)
    } else if (this.projectHistory) {
      this.updateStageCsvInfo(this.stageToDisplay, this.getLatestStage())
    }
  }

  private loadCharts() {
    this.longLoading.raise(LogAnalysisComponent.LONG_LOADING_CHARTS_FLAG);

    const filepath = this.pathToChartsDir();

    this.filesApi.listFiles(filepath)
      .then(files => {
        return Promise.all(files.map(this.loadChart));
      })
      .then(charts => {
        this.charts = charts;
        this.refreshAllCharts();
      })
      .catch(error => {
        alert("Failed to load charts");
        console.error(error);
      })
      .finally(() => {
        this.longLoading.clear(LogAnalysisComponent.LONG_LOADING_CHARTS_FLAG);
      })
  }

  private loadChart = (file: FileInfo) => {
    console.log(`Loading chart ${file.name}`);
    return this.filesApi.getFile(file.path).toPromise().then(text => {
      const chart = new LogChart(file.name);
      Object.assign(chart.definition, JSON.parse(text));
      return chart;
    });
  };

  private loadCsvFiles(stageCsvInfo: StageCsvInfo) {
    this.longLoading.raise(LogAnalysisComponent.LONG_LOADING_CSV_FLAG);

    const filepath = `${LogAnalysisComponent.PATH_TO_WORKSPACES}/${stageCsvInfo.stage.workspace}`

    this.filesApi.listFiles(filepath)
      .then(files => {
        return Promise.all(files.map(this.loadCsvFile));
      })
      .then(csvFiles => {
        stageCsvInfo.csvFiles = csvFiles;
        this.refreshAllCharts();
      })
      .catch(error => {
        alert("Failed to load csv for stage " + stageCsvInfo.id);
        console.error(error);
      })
      .finally(() => {
        this.longLoading.clear(LogAnalysisComponent.LONG_LOADING_CSV_FLAG);
      })
  }

  private loadCsvFile = (file: FileInfo) => {
    console.log(`Loading file ${file.name}`);
    return this.filesApi.getFile(file.path).toPromise().then(text => {
      const lines = text.split('\n');
      const csvFile: CsvFile = {
        name: file.name,
        content: [],
      };

      lines.forEach(line => {
        if (line.trim().length != 0) { // ignore empty lines
          csvFile.content.push(line.split(';'));
        }
      })

      return csvFile;
    });
  };

  saveCharts() {
    this.charts.forEach(chart => {
      const filename = chart.filename;
      this.saveChart(filename, chart.definition);
    })
  }

  private saveChart(filename: string, chart: LogChartDefinition) {
    const file = new File(
      [JSON.stringify(chart, null, "\t")], filename, {type: "application/json"},);
    this.filesApi.uploadFile(this.pathToChartsDir(), file).toPromise()
      .then(() => console.log(`Uploaded chart ${filename}`))
  }

  private pathToChartsDir() {
    return `${LogAnalysisComponent.PATH_TO_CHARTS}/${this.probablyPipelineId ?? this.selectedProject.id}`;
  }

  private deleteChart(chart: LogChart) {
    let filepath = this.pathToChartsDir();
    this.filesApi.delete(`${filepath}/${chart.filename}`).catch(error => {
      alert("Failed to delete chart");
      console.error(error);
    });
  }

  openDisplaySettingsDialog() {
    const dialogData = this.displaySettings;

    const dialogRef = this.dialog.open(LogAnalysisSettingsDialogComponent, {
      data: dialogData,
    });

    dialogRef.afterClosed().subscribe(_ => {
      this.refreshAllCharts();
    })
  }

  private findProjectPipeline(pipelines: PipelineInfo[]) {
    const project = this.selectedProject;
    this.probablyPipelineId = this.projectApi.findProjectPipeline(project, pipelines)
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
