import {Component, Input, OnInit} from '@angular/core';
import {ExecutionGroupInfo, ProjectApiService, ProjectInfo, State} from "../api/project-api.service";
import {MatDialog} from '@angular/material/dialog';
import {LogAnalysisChartDialogComponent} from "../log-analysis-chart-dialog/log-analysis-chart-dialog.component";
import {LongLoadingDetector} from "../long-loading-detector";
import {FileInfo, FilesApiService} from "../api/files-api.service";
import {ChartDataSeries, ChartDialogData, CsvFile, LogChartDefinition, StageCsvInfo} from "./log-chart-definition";

class LogChart {
  definition: LogChartDefinition;
  data: ChartDataSeries[];
  filename: string;

  constructor(id?: string) {
    this.definition = new LogChartDefinition();
    this.filename = id ?? `${Date.now().toString().slice(5)}${Math.random().toString().slice(2)}.chart`;
    console.log(this.filename);
  }

  refreshDisplay(stages: StageCsvInfo[]) {
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

  private static readonly PATH_TO_CHARTS = '/resources/.config/charts';
  private static readonly PATH_TO_WORKSPACES = '/workspaces';

  longLoading = new LongLoadingDetector();

  selectedProject: ProjectInfo = null;
  latestExecutionGroup: ExecutionGroupInfo = null;
  projectHistory: ExecutionGroupInfo[] = [];
  filteredHistory: ExecutionGroupInfo[] = [];
  charts: LogChart[] = [];

  stageToDisplay: StageCsvInfo = {
    id: null,
    executionGroup: null,
    csvFiles: [],
  }

  stagesToCompare: StageCsvInfo[] = [];

  @Input()
  set project(project: ProjectInfo) {
    this.selectedProject = project;

    this.longLoading.raise(LogAnalysisComponent.LONG_LOADING_HISTORY_FLAG);

    this.projectApi.getProjectHistory(this.selectedProject.id).then(projectHistory => {
      this.projectHistory = projectHistory;
      this.filteredHistory = this.filterHistory(projectHistory);
      this.latestExecutionGroup = this.getLatestExecutionGroup();

      this.autoSelectStage();

      this.loadCharts();
      this.longLoading.clear(LogAnalysisComponent.LONG_LOADING_HISTORY_FLAG);
    });
  }

  private filterHistory(projectHistory: ExecutionGroupInfo[]) {
    let filteredHistory = [];
    projectHistory.forEach(entry => {

      if (entry.configureOnly) {
        return;
      }

      const state = entry.getMostRelevantState();
      if (state == State.Failed || state == State.Skipped) {
        return;
      }

      filteredHistory.push(entry);
    })

    return filteredHistory;
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

  historyEntryLabel(executionGroup: ExecutionGroupInfo): string {
    const date = new Date(executionGroup.getMostRecentStartOrFinishTime()).toLocaleString();
    const name = executionGroup.stageDefinition.name;
    return `${date} · ${name}`
  }

  updateStage(stage: StageCsvInfo, executionGroup: ExecutionGroupInfo) {
    if (executionGroup == null) {
      executionGroup = this.latestExecutionGroup;
    }
    stage.executionGroup = executionGroup;
    console.log(`Selected execution group ${executionGroup.id}`);
    this.loadCsvFiles(stage);
  }

  isLatestStage(stage: StageCsvInfo): boolean {
    return stage.executionGroup == this.latestExecutionGroup;
  }

  getLatestExecutionGroup(): ExecutionGroupInfo {
    return this.filteredHistory.slice(-1)[0];
  }

  addStageToCompare() {
    let stage: StageCsvInfo = {
      csvFiles: [],
      executionGroup: undefined,
      id: ""
    }
    this.updateStage(stage, this.latestExecutionGroup);
    this.stagesToCompare.push(stage);
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
      chart.refreshDisplay(stages)
    })
  }

  openEditChartDialog(chart: LogChart) {
    const dialogData: ChartDialogData = {
      chartDefinition: chart.definition,
      stages: this.stagesToDrawGraphsFor(),
    }

    const dialogRef = this.dialog.open(LogAnalysisChartDialogComponent, {
      data: dialogData,
    });

    dialogRef.afterClosed().subscribe(_ => {
      chart.refreshDisplay(this.stagesToDrawGraphsFor());
      this.saveCharts();
    })
  }

  private stagesToDrawGraphsFor() {
    return [this.stageToDisplay, ...this.stagesToCompare];
  }

  private autoSelectStage() {
    if (this.stageToDisplay.id && this.projectHistory) {
      const executionGroup = this.projectHistory.find(entry => entry.id == this.stageToDisplay.id)
      this.updateStage(this.stageToDisplay, executionGroup)
    } else if (this.projectHistory) {
      this.updateStage(this.stageToDisplay, this.getLatestExecutionGroup())
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

  private loadCsvFiles(stage: StageCsvInfo) {
    this.longLoading.raise(LogAnalysisComponent.LONG_LOADING_CSV_FLAG);

    const filepath = `${LogAnalysisComponent.PATH_TO_WORKSPACES}/${stage.executionGroup.stages[0].workspace}`

    this.filesApi.listFiles(filepath)
      .then(files => {
        return Promise.all(files.map(this.loadCsvFile));
      })
      .then(csvFiles => {
        stage.csvFiles = csvFiles;
        this.refreshAllCharts();
      })
      .catch(error => {
        alert("Failed to load csv for stage " + stage.id);
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
    return `${LogAnalysisComponent.PATH_TO_CHARTS}/${this.selectedProject.id}`;
  }

  private deleteChart(chart: LogChart) {
    let filepath = this.pathToChartsDir();
    this.filesApi.delete(`${filepath}/${chart.filename}`).catch(error => {
      alert("Failed to delete chart");
      console.error(error);
    });
  }
}
