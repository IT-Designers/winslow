import {Component, Input, OnInit} from '@angular/core';
import {ProjectApiService} from '../../api/project-api.service';
import {MatDialog} from '@angular/material/dialog';
import {LogAnalysisChartDialogComponent} from './log-analysis-chart-dialog/log-analysis-chart-dialog.component';
import {FilesApiService} from '../../api/files-api.service';
import {AnalysisChart, ChartDefinition} from './chart-definition';
import {
  LogAnalysisSettingsDialogComponent
} from './log-analysis-settings-dialog/log-analysis-settings-dialog.component';
import {generateColor} from './colors';
import {CsvFilesService} from './csv-files.service';
import {ExecutionGroupInfo, FileInfo, ProjectInfo, StageInfo} from '../../api/winslow-api';
import {lastValueFrom} from "rxjs";

@Component({
  selector: 'app-project-analysis-tab',
  templateUrl: './project-analysis-tab.component.html',
  styleUrls: ['./project-analysis-tab.component.css']
})
export class ProjectAnalysisTabComponent implements OnInit {

  private static readonly PATH_TO_CHARTS = '/resources/.config/charts';

  isLongLoading = true;

  projectHistory: ExecutionGroupInfo[] = [];
  latestStage: StageInfo | null = null;
  selectableStages: StageInfo[] = [];
  charts: AnalysisChart[] = [];

  stageToDisplay: StageInfo | null = null;
  stagesToCompare: (StageInfo | null)[] = [];

  constructor(
    private dialog: MatDialog,
    private projectApi: ProjectApiService,
    private filesApi: FilesApiService,
    private csvFilesService: CsvFilesService,
  ) {
  }

  @Input() selectedStageId: string | undefined;

  @Input() set project(project: ProjectInfo) {
    this.isLongLoading = true;
    this._project = project;
    this.resetStagesAndCharts();

    this.projectApi.getProjectHistory(project.id)
      .then(projectHistory => this.loadStagesFromHistory(projectHistory))
      .then(() => this.loadCharts())
      .finally(() => this.isLongLoading = false);
  }

  get project(): ProjectInfo {
    return this._project;
  }

  private _project!: ProjectInfo;

  ngOnInit(): void {
  }

  stageLabel(stage: StageInfo): string {
    if (stage == null) {
      return 'Stage is null!';
    }

    const dateValue = stage.finishTime ?? stage.startTime ?? Date.now();
    const dateString = new Date(dateValue).toLocaleString();

    const name = stage.id.slice(this.project.id.length + 1);

    return `${dateString} · ${name}`;
  }

  stageColor(step: number) {
    return generateColor(step);
  }

  hasSelectableStages(): boolean {
    return this.selectableStages.length > 0;
  }

  isLatestStage(stageInfo: StageInfo | null): boolean {
    return stageInfo === this.latestStage;
  }

  displayStage(stage: StageInfo | null) {
    this.stageToDisplay = stage;
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
    const stages: (StageInfo | null)[] = [this.stageToDisplay, ...this.stagesToCompare];
    const filtered: StageInfo[] = stages.filter((stage): stage is StageInfo => stage != null) ;
    this.csvFilesService.setStages(filtered);
  }

  private resetStagesAndCharts() {
    this.charts = [];
    this.selectableStages = [];
    this.stagesToCompare = [];

    this.refreshStages();
  }

  private getLatestStage(): StageInfo {
    return this.selectableStages.slice(-1)[0];
  }

  private loadStagesFromHistory(projectHistory: ExecutionGroupInfo[]) {
    this.projectHistory = projectHistory;
    this.selectableStages = this.getSelectableStages(projectHistory);
    this.latestStage = this.getLatestStage();
    if (this.hasSelectableStages()) {
      const stageToDisplay = this.selectableStages.find(stage => stage.id == this.selectedStageId);
      if (stageToDisplay != undefined) {
        this.displayStage(stageToDisplay);
      } else {
        this.displayStage(this.latestStage);
      }
    }
  }

  private getSelectableStages(projectHistory: ExecutionGroupInfo[]) {
    const stages: StageInfo[] = [];

    projectHistory.forEach(executionGroup => {

      if (executionGroup.configureOnly) {
        return;
      }

      if (executionGroup.stages.length === 0) {
        return;
      }

      if (executionGroup.workspaceConfiguration.sharedWithinGroup) {
        stages.push(executionGroup.stages[0]);
      } else {
        stages.push(...executionGroup.stages);
      }
    });

    return stages.filter(stage => stage.workspace);
  }

  createChart() {
    const chart = new AnalysisChart(this.csvFilesService);
    this.charts.push(chart);
    this.openEditChartDialog(chart);
  }

  removeChart(chartIndex: number) {
    const chart = this.charts[chartIndex];
    if (!chart) {
      throw new Error('Chart index is out of range!');
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

  openEditChartDialog(chart: AnalysisChart) {
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
    const filepath = this.pathToChartsDir();

    return this.filesApi.listFiles(filepath)
      .then(files => {
        return Promise.all(files.map(file => this.loadChart(file)));
      })
      .then(charts => {
        this.charts = charts;
      })
      .catch(error => {
        alert('Failed to load charts');
        console.error(error);
      });
  }

  private loadChart(file: FileInfo) {
    console.log(`Loading chart ${file.name}`);
    return lastValueFrom(this.filesApi.getFile(file.path)).then(text => {
      const definition = new ChartDefinition(JSON.parse(text));
      return new AnalysisChart(this.csvFilesService, file.name, definition);
    });
  }

  private saveChart(filename: string, chart: ChartDefinition) {
    const file = new File([JSON.stringify(chart, null, 2)], filename, {type: 'application/json'});
    lastValueFrom(this.filesApi.uploadFile(this.pathToChartsDir(), file))
      .then(() => console.log(`Uploaded chart ${filename}`));
  }

  private pathToChartsDir() {
    return `${ProjectAnalysisTabComponent.PATH_TO_CHARTS}/${this.project.pipelineDefinition.id}`;
  }

  private deleteChart(chart: AnalysisChart) {
    const filepath = this.pathToChartsDir();
    this.filesApi.delete(`${filepath}/${chart.filename}`).catch(error => {
      alert('Failed to delete chart');
      console.error(error);
    });
  }
}
