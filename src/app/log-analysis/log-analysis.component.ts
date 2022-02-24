import {Component, Input, OnInit} from '@angular/core';
import {ExecutionGroupInfo, ProjectApiService, ProjectInfo, StageInfo, State} from "../api/project-api.service";
import {MatDialog} from '@angular/material/dialog';
import {LogAnalysisChartDialogComponent} from "../log-analysis-chart-dialog/log-analysis-chart-dialog.component";
import {LongLoadingDetector} from "../long-loading-detector";
import {FileInfo, FilesApiService} from "../api/files-api.service";
import {
  AnalysisDisplaySettings,
  ChartDataSet,
  ChartDialogData,
  CsvFileInfo,
  CsvFileStatus,
  LogChartDefinition,
  StageCsvInfo
} from "./log-chart-definition";
import {LogAnalysisSettingsDialogComponent} from "../log-analysis-settings-dialog/log-analysis-settings-dialog.component";
import {PipelineApiService, PipelineInfo} from "../api/pipeline-api.service";
import {BehaviorSubject} from "rxjs";

class LogChart {
  definition: LogChartDefinition;
  dataSubject: BehaviorSubject<ChartDataSet[]>;
  filename: string;

  constructor(id?: string) {
    this.definition = new LogChartDefinition();
    this.filename = id ?? LogChart.generateUniqueId();
    this.dataSubject = new BehaviorSubject<ChartDataSet[]>([]);
  }

  private static generateUniqueId() {
    return `${Date.now().toString().slice(5)}${Math.random().toString().slice(2)}.chart`;
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

  stageLabel(stage: StageInfo): string {
    const dateValue = stage.finishTime ?? stage.startTime;
    const dateString = new Date(dateValue).toLocaleString();

    const name = stage.id.slice(this.selectedProject.id.length + 1);

    return `${dateString} · ${name}`
  }

  loadStageCsvInfo(stageCsvInfo: StageCsvInfo, stage: StageInfo) {
    if (stage == null) {
      stage = this.latestStage;
    }
    stageCsvInfo.stage = stage;
    stageCsvInfo.csvFiles = [];
    console.log(`Selected stage ${stage.id}`);
    this.refreshAllCharts();
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
    this.loadStageCsvInfo(stageCsvInfo, this.latestStage);
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
      this.refreshChart(chart, stages, this.displaySettings)
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
      this.refreshChart(chart, this.stagesToDrawGraphsFor(), this.displaySettings);
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
      this.loadStageCsvInfo(this.stageToDisplay, stage)
    } else if (this.projectHistory) {
      this.loadStageCsvInfo(this.stageToDisplay, this.getLatestStage())
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

  private refreshChart(chart: LogChart, stageCsvInfos: StageCsvInfo[], displaySettings: AnalysisDisplaySettings) {
    const data = stageCsvInfos.map(stageCsvInfo => {
      const filename = chart.definition.file;
      const csvFile = stageCsvInfo.csvFiles.find(csvFile => csvFile.name == filename);

      if (csvFile?.status == CsvFileStatus.OK) {
        return LogChartDefinition.getDataSeries(chart.definition, csvFile.content, displaySettings)
      }

      if (csvFile == null) {
        const newCsvFile = {
          content: [],
          name: filename,
          status: CsvFileStatus.LOADING,
        }

        stageCsvInfo.csvFiles.push(newCsvFile);

        const directory = `${LogAnalysisComponent.PATH_TO_WORKSPACES}/${stageCsvInfo.stage.workspace}`;

        this.loadCsvFile(newCsvFile, directory, filename).then(() => {
          this.refreshChartsWithSource(filename);
        })
      }

      return [];
    });

    chart.dataSubject.next(data);
  }

  private loadCsvFile = (csvFile: CsvFileInfo, directory: string, filename: string) => {
    const filepath = `${directory}/${filename}`;
    console.log(`Loading file ${filename} from ${filepath}`);

    return this.filesApi.getFile(filepath).toPromise()
      .then(text => {
        csvFile.content = this.parseCsv(text);
        csvFile.status = CsvFileStatus.OK;
        return csvFile;
      })
      .catch(error => {
        console.warn(error);
        csvFile.status = CsvFileStatus.FAILED;
        return csvFile;
      });
  };

  private parseCsv(text: string) {
    const lines = text.split('\n');
    const content = [];
    lines.forEach(line => {
      if (line.trim().length != 0) { // ignore empty lines
        content.push(line.split(';'));
      }
    })
    return content;
  }

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

  private refreshChartsWithSource(filename: string) {
    const stages = this.stagesToDrawGraphsFor();
    this.charts.forEach(chart => {
      if (chart.definition.file == filename) {
        this.refreshChart(chart, stages, this.displaySettings)
      }
    })
  }
}
