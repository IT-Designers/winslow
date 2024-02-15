import {Component, Input, OnInit} from '@angular/core';
import {MatDialog} from '@angular/material/dialog';
import {LogAnalysisChartDialogComponent} from './log-analysis-chart-dialog/log-analysis-chart-dialog.component';
import {AnalysisChart} from './chart-definition';
import {
  LogAnalysisSettingsDialogComponent
} from './log-analysis-settings-dialog/log-analysis-settings-dialog.component';
import {generateColor} from './colors';
import {CsvFilesService} from './csv-files.service';
import {ChartDefinition, ProjectInfo, StageInfo} from '../../api/winslow-api';
import {PipelineApiService} from "../../api/pipeline-api.service";
import {DialogService} from "../../dialog.service";
import {ExecutionGroupInfoHelper, ProjectApiService} from "../../api/project-api.service";

@Component({
  selector: 'app-project-analysis-tab',
  templateUrl: './project-analysis-tab.component.html',
  styleUrls: ['./project-analysis-tab.component.css']
})
export class ProjectAnalysisTabComponent implements OnInit {

  private static readonly PATH_TO_CHARTS = '/resources/.config/charts';

  isLongLoading = true;

  projectHistory: ExecutionGroupInfoHelper[] = [];
  latestStage: StageInfo | null = null;
  selectableStages: StageInfo[] = [];
  charts: AnalysisChart[] = [];

  stageToDisplay: StageInfo | null = null;
  stagesToCompare: (StageInfo | null)[] = [];

  constructor(
    private matDialog: MatDialog,
    private customDialog: DialogService,
    private pipelineApi: PipelineApiService,
    private projectApi: ProjectApiService,
    private csvFilesService: CsvFilesService,
  ) {
  }

  @Input() selectedStageId: string | undefined;

  @Input() set project(project: ProjectInfo) {
    this._project = project;

    this.resetStagesAndCharts();
    this.charts = project
      .pipelineDefinition
      .charts
      .map(definition => new AnalysisChart(this.csvFilesService, definition));

    this.isLongLoading = true;
    this.projectApi.getProjectHistory(project.id)
      .then(projectHistory => this.loadStagesFromHistory(projectHistory))
      .then(_ => this.isLongLoading = false);
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
    const filtered: StageInfo[] = stages.filter((stage): stage is StageInfo => stage != null);
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

  private loadStagesFromHistory(projectHistory: ExecutionGroupInfoHelper[]) {
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

  private getSelectableStages(projectHistory: ExecutionGroupInfoHelper[]) {
    const stages: StageInfo[] = [];

    projectHistory.forEach(executionGroup => {

      if (executionGroup.executionGroupInfo.configureOnly) {
        return;
      }

      if (executionGroup.executionGroupInfo.stages.length === 0) {
        return;
      }

      if (executionGroup.executionGroupInfo.workspaceConfiguration.sharedWithinGroup) {
        stages.push(executionGroup.executionGroupInfo.stages[0]);
      } else {
        stages.push(...executionGroup.executionGroupInfo.stages);
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
      this.customDialog.error('Cannot delete chart: Index is out of range.');
    }
    this.charts.splice(chartIndex, 1);
    this.saveCharts();
  }

  saveCharts() {
    this.project.pipelineDefinition.charts = this.charts.map(chart => chart.definition$.getValue());
    this.pipelineApi.setPipelineDefinition(this.project.pipelineDefinition).catch(_ => {
      this.customDialog.error("Failed to save charts.");
    })
  }

  openEditChartDialog(chart: AnalysisChart) {
    const dialogRef = this.matDialog.open(LogAnalysisChartDialogComponent, {
      data: chart.definition$.getValue(),
    });

    dialogRef.afterClosed().subscribe((definition: ChartDefinition | null) => {
      if (definition != null) {
        chart.definition$.next(definition);
        this.saveCharts();
      }
    });
  }

  openGlobalSettingsDialog() {
    this.matDialog.open(LogAnalysisSettingsDialogComponent);
  }

}
