import {Component, Input, OnInit} from '@angular/core';
import {ExecutionGroupInfo, ProjectApiService, ProjectInfo} from "../api/project-api.service";
import {MatDialog} from '@angular/material/dialog';
import {LogAnalysisChartDialogComponent} from "../log-analysis-chart-dialog/log-analysis-chart-dialog.component";
import {LongLoadingDetector} from "../long-loading-detector";
import {FileInfo, FilesApiService} from "../api/files-api.service";
import {ChartData, ChartDialogData, CsvFile, LogChartDefinition} from "./log-chart-definition";

interface Stage {
  id: string;
  executionGroup: ExecutionGroupInfo;
  csvFiles: CsvFile[]
}

class LogChart {
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
  private static readonly PATH_TO_CHARTS = '/resources/.config/charts';
  private static readonly PATH_TO_WORKSPACES = '/workspaces';

  longLoading = new LongLoadingDetector();

  selectedProject: ProjectInfo = null;
  latestExecutionGroup: ExecutionGroupInfo = null;
  projectHistory: ExecutionGroupInfo[] = [];
  filteredHistory: ExecutionGroupInfo[] = [];
  charts: LogChart[] = [];

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

  historyEntryLabel(executionGroup: ExecutionGroupInfo): string {
    const date = new Date(executionGroup.getMostRecentStartOrFinishTime()).toLocaleString();
    const name = executionGroup.stageDefinition.name;
    return `${date} · ${name}`
  }

  updateStage(stage: Stage, executionGroup: ExecutionGroupInfo) {
    if (executionGroup == null) {
      executionGroup = this.latestExecutionGroup;
    }
    stage.executionGroup = executionGroup;
    console.log(`Selected execution group ${executionGroup.id}`);
    this.loadCsvFiles(stage);
  }

  isLatestStage(stage: Stage): boolean {
    return stage.executionGroup == this.latestExecutionGroup;
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

  addChart(chart: LogChart = new LogChart) {
    this.charts.push(chart);
    this.openEditChartDialog(chart.definition);
  }

  removeChart(chartIndex: number) {
    this.charts.splice(chartIndex, 1);
    this.saveCharts();
  }

  updateAllChartData() {
    const stages = [this.stageToDisplay, ...this.stagesToCompare];
    this.charts.forEach(chart => {
      chart.updateData(stages)
    })
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
      this.saveCharts();
    })
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

    const filepath = `${LogAnalysisComponent.PATH_TO_CHARTS}/${this.selectedProject.id}`;

    this.filesApi.listFiles(filepath)
      .then(files => {
        return Promise.all(files.map(this.loadChart));
      })
      .then(charts => {
        this.charts = charts;
        this.updateAllChartData();
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
    return this.filesApi.getFile(file.path).toPromise().then(text => {
      const chart = new LogChart();
      Object.assign(chart.definition, JSON.parse(text));
      return chart;
    });
  };

  private loadCsvFiles(stage: Stage) {
    this.longLoading.raise(LogAnalysisComponent.LONG_LOADING_CSV_FLAG);

    const filepath = `${LogAnalysisComponent.PATH_TO_WORKSPACES}/${stage.executionGroup.stages[0].workspace}`

    this.filesApi.listFiles(filepath)
      .then(files => {
        return Promise.all(files.map(this.loadCsvFile));
      })
      .then(csvFiles => {
        stage.csvFiles = csvFiles;
        this.updateAllChartData();
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
    const filenames = [];
    this.charts.forEach((chart, index) => {
      const filename = `${this.selectedProject.pipelineDefinition.id}.${index}.${(LogAnalysisComponent.CHART_FILE_EXTENSION)}`;
      this.uploadChart(filename, chart.definition);
      filenames.push(filename);
    })
  }

  private uploadChart(filename: string, chart: LogChartDefinition) {
    const file = new File(
      [JSON.stringify(chart, null, "\t")], filename, {type: "application/json"},);
    this.filesApi.uploadFile(LogAnalysisComponent.PATH_TO_CHARTS, file).toPromise()
      .then(() => console.log(`Uploaded chart ${filename}`))
  }
}
