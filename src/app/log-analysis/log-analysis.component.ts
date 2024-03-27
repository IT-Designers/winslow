import {Component, Input, OnInit} from '@angular/core';
import {ExecutionGroupInfo, ProjectApiService, ProjectInfo, StageInfo} from '../api/project-api.service';
import {MatDialog} from '@angular/material/dialog';
import {LogAnalysisChartDialogComponent} from './log-analysis-chart-dialog/log-analysis-chart-dialog.component';
import {FilesApiService, IFileInfoExt} from '../api/files-api.service';
import {LogChart, LogChartDefinition} from './log-chart-definition';
import {LogAnalysisSettingsDialogComponent} from './log-analysis-settings-dialog/log-analysis-settings-dialog.component';
import {PipelineApiService, PipelineInfo} from '../api/pipeline-api.service';
import {getColor} from './colors';
import {CsvFilesService} from './csv-files.service';

export interface CsvFile {
  name: string;
  content: [number][];
}

interface Stage {
  id: string;
  executionGroup: ExecutionGroupInfo;
  csvFiles: CsvFile[]
}

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

@Component({
  selector: 'app-log-analysis',
  templateUrl: './log-analysis.component.html',
  styleUrls: ['./log-analysis.component.css']
})
export class LogAnalysisComponent implements OnInit {

  private static readonly PATH_TO_CHARTS = '/resources/.config/charts';

  probablyPipelineId = null;
  isLongLoading = true;

  projectHistory: ExecutionGroupInfo[] = [];
  latestStage: StageInfo = null;
  selectableStages: StageInfo[] = [];
  charts: LogChart[] = [];

  stageToDisplay: StageInfo;
  stagesToCompare: StageInfo[] = [];

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
      .finally(() => this.longLoading.clear(LogAnalysisComponent.LONG_LOADING_HISTORY_FLAG));

    this.longLoading.raise(LogAnalysisComponent.LONG_LOADING_PIPELINES_FLAG);
    const pipelinePromise = this.pipelineApi.getPipelineDefinitions()
      .then(pipelines => this.findProjectPipeline(pipelines))
      .finally(() => this.longLoading.clear(LogAnalysisComponent.LONG_LOADING_PIPELINES_FLAG));

    Promise.all([projectPromise, pipelinePromise]).then(
      () => this.loadCharts()
    );
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
    private csvFilesService: CsvFilesService,
  ) {
  }

  @Input() selectedStage: string;

  @Input() set project(project: ProjectInfo) {
    this.isLongLoading = true;
    this.projectInfo = project;
    this.resetStagesAndCharts();

    const projectPromise = this.projectApi.getProjectHistory(project.id)
      .then(projectHistory => this.loadStagesFromHistory(projectHistory))

    const pipelinePromise = this.pipelineApi.getPipelineDefinitions()
      .then(pipelines => this.findProjectPipeline(pipelines))

    Promise.all([projectPromise, pipelinePromise])
      .then(() => this.loadCharts())
      .finally(() => this.isLongLoading = false);
  }

  ngOnInit(): void {
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

  hasSelectableStages(): boolean {
    return this.selectableStages.length > 0
  }

  stageColor(step: number) {
    return getColor(step);
  }

  isLatestStage(stageInfo: StageInfo): boolean {
    return stageInfo === this.latestStage;
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

  private resetStagesAndCharts() {
    this.charts = [];
    this.selectableStages = [];
    this.stagesToCompare = [];

    this.refreshStages();
  }

  private getLatestStage(): StageInfo {
    return this.selectableStages.slice(-1)[0];
  }

  private loadStagesFromHistory(projectHistory) {
    this.projectHistory = projectHistory;
    this.selectableStages = this.getSelectableStages(projectHistory);
    this.latestStage = this.getLatestStage();
    if (this.hasSelectableStages()) {
      this.displayLatestStage();
    }
  }

  private getSelectableStages(projectHistory: ExecutionGroupInfo[]) {
    let stages: StageInfo[] = [];

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
    const filepath = this.pathToChartsDir();

    return this.filesApi.listFiles(filepath)
      .then(files => {
        return Promise.all(files.map(this.loadChart));
      })
      .then(charts => {
        this.charts = charts;
      })
      .catch(error => {
        alert('Failed to load charts');
        console.error(error);
      });
  }

  private loadChart = (file: IFileInfoExt) => {
    console.log(`Loading chart ${file.name}`);
    return this.filesApi.getFile(file.path).toPromise().then(text => {
      const definition = new LogChartDefinition();
      Object.assign(definition, JSON.parse(text));
      return new LogChart(this.csvFilesService, file.name, definition);
    });
  };

  private saveChart(filename: string, chart: LogChartDefinition) {
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

    return this.filesApi.listFiles(filepath)
      .then(files => {
        return Promise.all(files.map(this.loadChart));
      })
      .then(charts => {
        this.charts = charts;
        this.refreshAllCharts();
      })
      .catch(error => {
        alert('Failed to load charts');
        console.error(error);
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
