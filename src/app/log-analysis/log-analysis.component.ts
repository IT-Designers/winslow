import {Component, Input, OnInit} from '@angular/core';
import {ExecutionGroupInfo, ProjectApiService, ProjectInfo, StageInfo} from '../api/project-api.service';
import {MatDialog} from '@angular/material/dialog';
import {LogAnalysisChartDialogComponent} from './log-analysis-chart-dialog/log-analysis-chart-dialog.component';
import {LongLoadingDetector} from '../long-loading-detector';
import {FileInfo, FilesApiService} from '../api/files-api.service';
import {LogChart, LogChartDefinition} from './log-chart-definition';
import {LogAnalysisSettingsDialogComponent} from './log-analysis-settings-dialog/log-analysis-settings-dialog.component';
import {PipelineApiService, PipelineInfo} from '../api/pipeline-api.service';
import {getColor} from './colors';
import {CsvFilesService} from './csv-files.service';

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

  longLoading = new LongLoadingDetector();
  hasSelectableStages = true;
  probablyPipelineId = null;

  projectHistory: ExecutionGroupInfo[] = [];
  latestStage: StageInfo = null;
  selectableStages: StageInfo[] = [];
  charts: LogChart[] = [];

  stageToDisplay: StageInfo;
  stagesToCompare: StageInfo[] = [];

  private projectInfo: ProjectInfo;

  constructor(
    private dialog: MatDialog,
    private projectApi: ProjectApiService,
    private pipelineApi: PipelineApiService,
    private filesApi: FilesApiService,
    private csvFilesService: CsvFilesService,
  ) {
  }

  @Input() selectedStage: string;

  @Input() set project(project: ProjectInfo) {
    this.projectInfo = project;

    this.longLoading.raise(LogAnalysisComponent.LONG_LOADING_HISTORY_FLAG);
    const projectPromise = this.projectApi.getProjectHistory(project.id)
      .then(projectHistory => this.loadStagesFromHistory(projectHistory))
      .finally(() => this.longLoading.clear(LogAnalysisComponent.LONG_LOADING_HISTORY_FLAG));

    this.longLoading.raise(LogAnalysisComponent.LONG_LOADING_PIPELINES_FLAG);
    const pipelinePromise = this.pipelineApi.getPipelineDefinitions()
      .then(pipelines => this.findProjectPipeline(pipelines))
      .finally(() => this.longLoading.clear(LogAnalysisComponent.LONG_LOADING_PIPELINES_FLAG));

    Promise.all([projectPromise, pipelinePromise]).then(
      () => this.loadCharts()
    );
  }

  ngOnInit(): void {
  }

  isLongLoading(): boolean {
    return this.longLoading.isLongLoading();
  }

  stageLabel(stage: StageInfo): string {
    if (stage == null) {
      return 'Stage is null!';
    }

    const dateValue = stage.finishTime ?? stage.startTime;
    const dateString = new Date(dateValue).toLocaleString();

    const name = stage.id.slice(this.projectInfo.id.length + 1);

    return `${dateString} · ${name}`;
  }

  stageColor(step: number) {
    return getColor(step);
  }

  isLatestStage(stageInfo: StageInfo): boolean {
    return stageInfo == this.latestStage;
  }

  displayLatestStage() {
    this.stageToDisplay = this.latestStage;
    this.refreshStages();
  }

  addStageToCompare() {
    this.stagesToCompare.push(this.latestStage);

    this.refreshStages();
  }

  compareWithLatestStage(index: number) {
    this.stagesToCompare[index] = this.latestStage;
    this.refreshStages();
  }

  removeStageToCompare(stageIndex: number) {
    this.stagesToCompare.splice(stageIndex, 1);

    this.refreshStages();
  }

  refreshStages() {
    const stages = [this.stageToDisplay, ...this.stagesToCompare];
    this.csvFilesService.setStages(stages);
  }

  private getLatestStage(): StageInfo {
    return this.selectableStages.slice(-1)[0];
  }

  private loadStagesFromHistory(projectHistory) {
    this.projectHistory = projectHistory;
    this.selectableStages = this.getSelectableStages(projectHistory);
    this.latestStage = this.getLatestStage();
    this.hasSelectableStages = this.selectableStages.length > 0;
    if (this.hasSelectableStages) {
      this.displayLatestStage();
    }
  }

  private getSelectableStages(projectHistory: ExecutionGroupInfo[]) {
    let stages: StageInfo[] = [];

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
    });

    return stages;
  }

  createChart() {
    const chart = new LogChart(this.csvFilesService);
    this.charts.push(chart);
    this.openEditChartDialog(chart);
  }

  removeChart(chartIndex: number) {
    let chart = this.charts[chartIndex];
    if (!chart) {
      throw 'Chart index is out of range!';
    }
    this.deleteChart(chart);
    this.charts.splice(chartIndex, 1);
  }

  saveCharts() {
    this.charts.forEach(chart => {
      const filename = chart.filename;
      this.saveChart(filename, chart.definition$.getValue());
    });
  }

  openEditChartDialog(chart: LogChart) {
    const dialogRef = this.dialog.open(LogAnalysisChartDialogComponent, {
      data: chart.definition$.getValue(),
    });

    dialogRef.afterClosed().subscribe(definition => {
      if (definition != null) {
        chart.definition$.next(definition);
        this.saveCharts();
      }
    });
  }

  openGlobalSettingsDialog() {
    this.dialog.open(LogAnalysisSettingsDialogComponent);
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
        alert('Failed to load charts');
        console.error(error);
      })
      .finally(() => {
        this.longLoading.clear(LogAnalysisComponent.LONG_LOADING_CHARTS_FLAG);
      });
  }

  private loadChart = (file: FileInfo) => {
    console.log(`Loading chart ${file.name}`);
    return this.filesApi.getFile(file.path).toPromise().then(text => {
      const definition = new LogChartDefinition();
      Object.assign(definition, JSON.parse(text));
      return new LogChart(this.csvFilesService, file.name, definition);
    });
  };

  private saveChart(filename: string, chart: LogChartDefinition) {
    const file = new File([JSON.stringify(chart, null, 2)], filename, {type: 'application/json'});
    this.filesApi.uploadFile(this.pathToChartsDir(), file).toPromise()
      .then(() => console.log(`Uploaded chart ${filename}`));
  }

  private pathToChartsDir() {
    return `${LogAnalysisComponent.PATH_TO_CHARTS}/${this.probablyPipelineId ?? this.projectInfo.id}`;
  }

  private deleteChart(chart: LogChart) {
    let filepath = this.pathToChartsDir();
    this.filesApi.delete(`${filepath}/${chart.filename}`).catch(error => {
      alert('Failed to delete chart');
      console.error(error);
    });
  }

  private findProjectPipeline(pipelines: PipelineInfo[]) {
    const project = this.projectInfo;
    this.probablyPipelineId = this.projectApi.findProjectPipeline(project, pipelines)
  }
}
