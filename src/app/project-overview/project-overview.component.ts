import {Component, EventEmitter, Input, OnDestroy, Output} from '@angular/core';
import {Action, ExecutionGroupInfo, ProjectApiService, ProjectInfo, StageInfo, State, StatsInfo} from '../api/project-api.service';
import {DialogService} from '../dialog.service';
import {MatDialog} from '@angular/material/dialog';
import {
  ProjectDiskUsageDialogComponent,
  ProjectDiskUsageDialogData
} from '../project-disk-usage-dialog/project-disk-usage-dialog.component';
import {PipelineApiService, PipelineInfo} from '../api/pipeline-api.service';
import {pipe, Subscription} from 'rxjs';

@Component({
  selector: 'app-project-overview',
  templateUrl: './project-overview.component.html',
  styleUrls: ['./project-overview.component.css']
})
export class ProjectOverviewComponent implements OnDestroy {

  private static readonly UPDATE_INTERVAL = 1_000;
  private static readonly GRAPH_ENTRIES = 180;

  @Output() openFiles = new EventEmitter<ProjectInfo>();
  @Output() openLogs = new EventEmitter<ProjectInfo>();
  @Output() clickUseAsBlueprint = new EventEmitter<[ExecutionGroupInfo, StageInfo?]>();
  @Output() clickDeleteEnqueued = new EventEmitter<ExecutionGroupInfo>();
  @Output() clickResumeSingle = new EventEmitter<ExecutionGroupInfo>();
  @Output() clickResume = new EventEmitter<ExecutionGroupInfo>();
  @Output() clickPause = new EventEmitter<ExecutionGroupInfo>();

  schemeCpu = {domain: ['#DD4444']};
  schemeMemory = {domain: ['#44DD44']};

  stateValue: State = null;
  stateFinished: boolean;
  stateRunning: boolean;
  statePaused: boolean;

  lastSuccessfulStatsUpdate = 0;
  seriesInitialized = false;

  mostRecent: ExecutionGroupInfo = null;
  projectValue: ProjectInfo;
  memory: any[] = [];
  memoryMax = 1;
  cpu: any[] = [];
  cpuMax = 100;
  subscription: Subscription = null;

  enqueued: ExecutionGroupInfo[] = [];
  pipelineActions: PipelineInfo[] = [];

  constructor(private api: ProjectApiService,
              private dialog: DialogService,
              private createDialog: MatDialog,
              private  pipelines: PipelineApiService) {
  }

  private static maxOfSeriesOr(series: any[], minimum: number, upperMax: number) {
    let max = minimum;
    for (const entry of series) {
      if (entry.value > max) {
        max = entry.value;
      }
    }
    return Math.max(max, Math.min(max * 1.4, upperMax));
  }

  private static limitSeriesTo(container: any, length: number) {
    if (container.series.length >= length) {
      container.series.splice(0, container.series.length - length + 1);
    }
  }

  @Input()
  set project(value: ProjectInfo) {
    this.projectValue = value;
    this.unsubscribe();
    this.subscribe();
    if (value != null) {
      this.pipelines
        .getPipelineDefinitions()
        .then(def => {
          this.pipelineActions = def.filter(pipe(p => p.hasActionMarkerFor(this.projectValue.pipelineDefinition.name)));
        });
    }
    this.enqueued = [];
    this.initSeries();
  }

  @Input()
  set history(history: ExecutionGroupInfo[]) {
    this.mostRecent = null;
    this.enqueued = [];
    if (history && history.length > 0) {
      this.mostRecent = history[0];
      for (const entry of history) {
        // because it is ordered, it starts with the enqueued ones
        // and then continues with the old ones
        if (entry.enqueueIndex != null) {
          this.enqueued.push(entry);
        }
      }
    }
  }

  @Input()
  set state(state: State) {
    this.stateValue = state;
    this.stateFinished = State.Failed === state || State.Succeeded === state;
    this.stateRunning = State.Running === state;
    this.statePaused = State.Paused === state || State.Enqueued === state;
  }

  private initSeries() {
    this.seriesInitialized = true;

    const now = new Date();
    const zeroes = [];

    for (let i = ProjectOverviewComponent.GRAPH_ENTRIES; i >= 0; --i) {
      zeroes.push({
        name: new Date(now.getTime() - (i * ProjectOverviewComponent.UPDATE_INTERVAL)),
        value: 0
      });
    }

    this.memory = [{
      name: 'Used',
      series: zeroes.map(z => z),
    }];
    this.cpu = [{
      name: 'Consumed',
      series: zeroes.map(z => z),
    }];
  }

  ngOnDestroy() {
    this.unsubscribe();
  }

  @Input()
  set visible(value: boolean) {
    if (value && !this.seriesInitialized) {
      this.initSeries();
    }
  }

  onStatsUpdate(stats: StatsInfo) {
    this.updateCpu(stats);
    this.updateMemory(stats);
  }

  private updateCpu(stats: StatsInfo) {
    this.cpu[0].series.push({
      name: new Date(),
      value: stats.cpuUsed/*.toLocaleString('en-US') -- ngx seems to be borked here?*/
    });

    this.cpuMax = ProjectOverviewComponent.maxOfSeriesOr(this.cpu[0].series, 100, stats.cpuMaximum);
    this.cpu = this.cpu.map(c => {
      ProjectOverviewComponent.limitSeriesTo(c, ProjectOverviewComponent.GRAPH_ENTRIES);
      return c;
    });
  }


  private updateMemory(stats: StatsInfo) {
    this.memory[0].series.push({
      name: new Date(),
      value: this.bytesToGigabyte(stats.memoryAllocated)/*.toLocaleString('en-US') -- ngx seems to be borked here?*/
    });

    this.memoryMax = ProjectOverviewComponent.maxOfSeriesOr(this.memory[0].series, 0.1, stats.memoryMaximum);
    this.memory = this.memory.map(m => {
      ProjectOverviewComponent.limitSeriesTo(m, ProjectOverviewComponent.GRAPH_ENTRIES);
      return m;
    });
  }

  bytesToGigabyte(bytes: number) {
    return bytes / (1024 * 1024 * 1024);
  }

  pause() {
    if (this.projectValue) {
      this.dialog.openLoadingIndicator(
        this.api
          .pause(this.projectValue.id)
          .then(r => this.statePaused = true),
        `Pausing pipeline...`
      );
    }
  }

  resume() {
    if (this.projectValue) {
      this.dialog.openLoadingIndicator(
        this.api
          .resume(this.projectValue.id)
          .then(r => this.statePaused = false),
        `Resuming pipeline...`
      );
    }
  }

  emitOpenFiles() {
    if (this.projectValue) {
      this.openFiles.emit(this.projectValue);
    }
  }

  emitOpenLogs() {
    if (this.projectValue) {
      this.openLogs.emit(this.projectValue);
    }
  }

  isConfigure(action: Action) {
    return Action.Configure === action;
  }

  isEnqueued(state: State) {
    return State.Enqueued === state;
  }

  isRunning(state: State) {
    return State.Running === state;
  }

  openProjectDiskUsageDialog() {
    this.createDialog
      .open(ProjectDiskUsageDialogComponent, {
        data: {
          projects: [this.projectValue],
        } as ProjectDiskUsageDialogData
      });
  }

  enqueueAction(value: string) {
    this.dialog.openLoadingIndicator(
      this.api.action(this.projectValue.id, value),
      `Submitting action...`
    );
  }


  actionBackgroundColor(tag: string) {
    tag = tag.trim();
    let sum = tag.length;
    for (let i = 0; i < tag.length; ++i) {
      sum += (i + 1) * tag.charCodeAt(i) * 1337;
    }

    const min = 192;
    const max = 256 - min;

    const red = ((sum / 7) % max) + min;
    const green = ((sum / 5) % max) + min;
    const blue = ((sum / 3) % max) + min;
    return `rgba(${red}, ${green}, ${blue}, 0.45)`;
  }

  private subscribe() {
    this.unsubscribe();
    this.subscription = this.api.watchProjectStats(this.projectValue.id, stats => {
      this.onStatsUpdate(stats);
    });
  }

  private unsubscribe() {
    if (this.subscription) {
      this.subscription.unsubscribe();
      this.subscription = null;
    }
  }
}
