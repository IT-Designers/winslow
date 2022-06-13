import {Component, Input, OnInit} from '@angular/core';
import {ExecutionGroupInfo, ProjectApiService, ProjectInfo, StageInfo} from "../api/project-api.service";
import {MatDialog} from '@angular/material/dialog';
import {LogAnalysisChartDialogComponent} from "../log-analysis-chart-dialog/log-analysis-chart-dialog.component";
import {LongLoadingDetector} from "../long-loading-detector";
import {FileInfo, FilesApiService} from "../api/files-api.service";
import {
  ChartDialogData, CsvFileController,
  LogChart,
  LogChartDefinition,
  StageCsvInfo
} from "./log-chart-definition";
import {LogAnalysisSettingsDialogComponent} from "../log-analysis-settings-dialog/log-analysis-settings-dialog.component";
import {PipelineApiService, PipelineInfo} from "../api/pipeline-api.service";

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

  private readonly csvFileController : CsvFileController;

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

  @Input()
  set project(project: ProjectInfo) {
    this.selectedProject = project

    this.longLoading.raise(LogAnalysisComponent.LONG_LOADING_HISTORY_FLAG)
    const projectPromise = this.projectApi.getProjectHistory(this.selectedProject.id)
      .then(projectHistory => this.loadStagesFromHistory(projectHistory))
      .finally(() => this.longLoading.clear(LogAnalysisComponent.LONG_LOADING_HISTORY_FLAG))

    this.longLoading.raise(LogAnalysisComponent.LONG_LOADING_PIPELINES_FLAG)
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
    this.csvFileController = new CsvFileController(filesApi);
  }

  ngOnInit(): void {
  }

  isLongLoading(): boolean {
    return this.longLoading.isLongLoading();
  }

  stageLabel(stage: StageInfo): string {
    if (stage == null) {
      return "Stage is null!";
    }

    const dateValue = stage.finishTime ?? stage.startTime;
    const dateString = new Date(dateValue).toLocaleString();

    const name = stage.id.slice(this.selectedProject.id.length + 1);

    return `${dateString} · ${name}`
  }

  isLatestStage(stageCsvInfo: StageCsvInfo): boolean {
    return stageCsvInfo.stage == this.latestStage;
  }

  getLatestStage(): StageInfo {
    return this.selectableStages.slice(-1)[0];
  }

  private refreshStages() {
    const stages = [this.stageToDisplay, ...this.stagesToCompare];
    this.csvFileController.stages$.next(stages);
  }

  updateStage(stageCsvInfo: StageCsvInfo, stage: StageInfo) {
    if (stage == null) {
      stage = this.latestStage;
    }
    stageCsvInfo.id = stage.id;
    stageCsvInfo.stage = stage;
    stageCsvInfo.csvFiles = [];

    console.log(`Selected stage ${stage.id}`);

    this.refreshStages()
  }

  addStageToCompare() {
    let stageCsvInfo: StageCsvInfo = {
      csvFiles: [],
      stage: undefined,
      id: ""
    }
    this.updateStage(stageCsvInfo, this.latestStage);
    this.stagesToCompare.push(stageCsvInfo);

    this.refreshStages()
  }

  removeStageToCompare(stageIndex: number) {
    this.stagesToCompare.splice(stageIndex, 1);

    this.refreshStages()
  }

  createChart() {
    const chart = new LogChart(this.csvFileController);
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

  openEditChartDialog(chart: LogChart) {
    const dialogData: ChartDialogData = {
      definition: chart.definition$.getValue(),
      chart: chart,
      csvFileController: this.csvFileController,
    }

    const dialogRef = this.dialog.open(LogAnalysisChartDialogComponent, {
      data: dialogData,
    });

    dialogRef.afterClosed().subscribe(definition => {
      if (definition != null) {
        chart.definition$.next(definition);
        this.saveCharts();
      }
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

      if (executionGroup.stages.length == 0) {
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

  private autoSelectStage() {
    this.stagesToCompare = [];
    if (this.stageToDisplay.id && this.projectHistory) {
      const stage = this.selectableStages.find(entry => entry.id == this.stageToDisplay.id)
      this.updateStage(this.stageToDisplay, stage)
    } else if (this.projectHistory) {
      this.updateStage(this.stageToDisplay, this.getLatestStage())
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
      const chart = new LogChart(this.csvFileController, file.name);
      const definition = new LogChartDefinition();
      Object.assign(definition, JSON.parse(text));
      chart.definition$.next(definition);
      return chart;
    });
  };

  saveCharts() {
    this.charts.forEach(chart => {
      const filename = chart.filename;
      this.saveChart(filename, chart.definition$.getValue());
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
    const dialogData = LogChart.overrides;

    const dialogRef = this.dialog.open(LogAnalysisSettingsDialogComponent, {
      data: dialogData,
    });

    dialogRef.afterClosed().subscribe(_ => {
    })
  }

  private findProjectPipeline(pipelines: PipelineInfo[]) {
    const project = this.selectedProject;
    this.probablyPipelineId = this.projectApi.findProjectPipeline(project, pipelines)
  }
}
