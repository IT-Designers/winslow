import {Component, EventEmitter, Input, OnDestroy, OnInit, Output} from '@angular/core';
import {Action, HistoryEntry, ProjectApiService, ProjectInfo, State, StatsInfo} from '../api/project-api.service';
import {DialogService} from '../dialog.service';
import {MatDialog} from '@angular/material';
import {
  ProjectDiskUsageDialogComponent,
  ProjectDiskUsageDialogData
} from '../project-disk-usage-dialog/project-disk-usage-dialog.component';

@Component({
  selector: 'app-project-overview',
  templateUrl: './project-overview.component.html',
  styleUrls: ['./project-overview.component.css']
})
export class ProjectOverviewComponent implements OnInit, OnDestroy {

  private static readonly UPDATE_INTERVAL = 1_000;
  private static readonly GRAPH_ENTRIES = 180;

  @Output() openFiles = new EventEmitter<ProjectInfo>();
  @Output() openLogs = new EventEmitter<ProjectInfo>();
  @Output() clickUseAsBlueprint = new EventEmitter<HistoryEntry>();
  @Output() clickDeleteEnqueued = new EventEmitter<HistoryEntry>();
  @Output() clickResumeSingle = new EventEmitter<HistoryEntry>();
  @Output() clickResume = new EventEmitter<HistoryEntry>();
  @Output() clickPause = new EventEmitter<HistoryEntry>();

  schemeCpu = {domain: ['#DD4444']};
  schemeMemory = {domain: ['#44DD44']};

  stateValue: State = null;
  stateFinished: boolean;
  stateRunning: boolean;
  statePaused: boolean;

  lastSuccessfulStatsUpdate = 0;
  seriesInitialized = false;

  mostRecent: HistoryEntry = null;
  projectValue: ProjectInfo;
  memory: any[] = [];
  memoryMax = 1;
  cpu: any[] = [];
  cpuMax = 100;
  poll = null;

  enqueued: HistoryEntry[] = [];

  constructor(private api: ProjectApiService,
              private dialog: DialogService,
              private createDialog: MatDialog) {
    this.poll = setInterval(() => {
      const updateNonetheless = new Date().getTime() - this.lastSuccessfulStatsUpdate < 5_000;
      if (this.projectValue && (State.Running === this.stateValue || updateNonetheless)) {
        this.api.getStats(this.projectValue.id)
          .then(stats => {
            if (stats) {
              this.lastSuccessfulStatsUpdate = new Date().getTime();
            } else if (updateNonetheless) {
              stats = new StatsInfo();
            }
            if (stats) {
              this.onStatsUpdate(stats);
            }
          });
      }
    }, ProjectOverviewComponent.UPDATE_INTERVAL);
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
    this.enqueued = [];
    this.initSeries();
  }

  @Input()
  set history(history: HistoryEntry[]) {
    this.mostRecent = null;
    this.enqueued = [];
    if (history && history.length > 0) {
      this.mostRecent = history[0];
      for (const entry of history) {
        // because it is ordered, it starts with the enqueued ones
        // and then continues with the old ones
        if (State.Enqueued !== entry.state && State.Running !== entry.state) {
          break;
        } else {
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


  ngOnInit() {
  }

  ngOnDestroy() {
    if (this.poll) {
      clearInterval(this.poll);
    }
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

  kill() {
    if (this.projectValue) {
      this.dialog.openAreYouSure(
        `Kill currently running stage of project ${this.projectValue.name}`,
        () => this.api.killStage(this.projectValue.id)
      );
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
}
