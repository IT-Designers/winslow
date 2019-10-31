import {Component, ElementRef, EventEmitter, Input, OnDestroy, OnInit, Output, ViewChild} from '@angular/core';
import {
  Action,
  HistoryEntry,
  ImageInfo,
  LogEntry,
  LogSource,
  Project,
  ProjectApiService,
  State,
  StateInfo
} from '../api/project-api.service';
import {NotificationService} from '../notification.service';
import {MatDialog, MatTabGroup} from '@angular/material';
import {LongLoadingDetector} from '../long-loading-detector';
import {PipelineApiService, PipelineInfo, StageInfo} from '../api/pipeline-api.service';
import {StageExecutionSelectionComponent} from '../stage-execution-selection/stage-execution-selection.component';
import {GroupSettingsDialogComponent, GroupSettingsDialogData} from '../group-settings-dialog/group-settings-dialog.component';
import {DialogService} from '../dialog.service';


@Component({
  selector: 'app-project-view',
  templateUrl: './project-view.component.html',
  styleUrls: ['./project-view.component.css']
})
export class ProjectViewComponent implements OnInit, OnDestroy {

  @ViewChild('tabGroup', {static: false}) tabs: MatTabGroup;
  @ViewChild('console', {static: false}) htmlConsole: ElementRef<HTMLElement>;
  @ViewChild('scrollBottomTarget', {static: false}) scrollBottomTarget: ElementRef<HTMLElement>;
  @ViewChild('executionSelection', {static: false}) executionSelection: StageExecutionSelectionComponent;

  private projectValue: Project;
  @Output('state') private stateEmitter = new EventEmitter<State>();
  @Output('deleted') private deletedEmitter = new EventEmitter<boolean>();

  filesAdditionalRoot: string = null;
  filesNavigationTarget: string = null;

  stateValue?: State = null;
  history?: HistoryEntry[] = null;
  logs?: LogEntry[] = null;
  paused: boolean = null;
  pauseReason?: string = null;
  progress?: number;

  watchHistory = false;
  watchPaused = false;
  watchLogs = false;
  watchLogsInterval: any = null;
  watchLogsId?: string = null;
  watchLatestLogs = true;

  loadLogsOnceAnyway = false;

  longLoading = new LongLoadingDetector();

  stickConsole = true;
  consoleIsLoading = false;
  scrollCallback;

  pipelines: PipelineInfo[];

  selectedPipeline: PipelineInfo = null;
  selectedStage: StageInfo = null;
  defaultEnvVars: Map<string, string> = null;


  constructor(public api: ProjectApiService, private notification: NotificationService,
              private pipelinesApi: PipelineApiService, private matDialog: MatDialog,
              private dialog: DialogService) {
  }

  private static deepClone(obj: any): any {
    return JSON.parse(JSON.stringify(obj));
  }

  ngOnInit(): void {
    this.setupFiles();
    this.scrollCallback = () => this.onWindowScroll();
    window.addEventListener('scroll', this.scrollCallback, true);
    this.pipelinesApi.getPipelineDefinitions().then(result => {
      this.pipelines = result;
      this.executionSelection.pipelines = result;
      this.project = this.project;
    });
  }

  ngOnDestroy(): void {
    window.removeEventListener('scroll', this.scrollCallback, true);
  }

  @Input()
  public set project(value: Project) {
    this.projectValue = value;
    if (this.pipelines != null) {
      this.projectValue.pipelineDefinition.id = this.getProjectPipelineId();
      this.executionSelection.defaultPipelineId = this.projectValue.pipelineDefinition.id;
    }
    this.logs = null;
    this.history = null;
    this.pollWatched(true);
    this.setupFiles();
  }

  public get project(): Project {
    return this.projectValue;
  }

  @Input()
  public set state(value: StateInfo) {
    this.update(value);
  }

  update(info: StateInfo) {
    this.stateValue = info.actualState();
    this.pauseReason = info.pauseReason;
    this.progress = info.stageProgress;

    this.paused = this.stateValue === State.Paused || this.pauseReason != null;

    this.stateEmitter.emit(this.stateValue);
    this.pollWatched();
  }

  isConfigure(action: Action): boolean {
    return Action.Configure === action;
  }

  isEnqueued(state = this.stateValue): boolean {
    return State.Enqueued === state;
  }

  isRunning(state = this.stateValue): boolean {
    return State.Running === state;
  }

  pollWatched(forceUpdateOnWatched = false): void {
    if (this.watchHistory && (this.isRunning() || forceUpdateOnWatched)) {
      this.loadHistory();
    }
    if (this.watchPaused && (this.isRunning() || forceUpdateOnWatched)) {
      this.loadPaused();
    }
    if (this.watchLogs && (this.isRunning() || this.loadLogsOnceAnyway || forceUpdateOnWatched)) {
      if (!this.watchLogsInterval) {
        this.watchLogsInterval = setInterval(() => this.loadLogs(), 1000);
      }
      this.loadLogs();
    } else if (this.watchLogsInterval) {
      clearInterval(this.watchLogsInterval);
      this.watchLogsInterval = null;
    }
  }

  loadLogs() {
    if (this.consoleIsLoading) {
      return;
    }
    this.consoleIsLoading = true;
    this.longLoading.increase();
    return this.requestLogs()
      .then(logs => {
        if (this.logs == null) {
          this.logs = [];
        }
        this.loadLogsOnceAnyway = this.isRunning();
        if (logs.length > 0 && this.logs.length > 0 && logs[0].stageId !== this.logs[0].stageId) {
          this.logs = null;
          return this.loadLogs();
        } else {
          logs.forEach(entry => this.logs.push(entry));
          if (logs.length > 0) {
            // execute it after the DOM update
            setTimeout(() => this.scrollConsoleToBottom());
          }
        }
      })
      .finally(() => {
        this.longLoading.decrease();
        this.consoleIsLoading = false;
      });
  }

  requestLogs() {
    if (this.watchLatestLogs) {
      const skipLines = this.logs != null ? this.logs.length : 0;
      const expectingStageId = this.logs != null && this.logs.length > 0 ? this.logs[0].stageId : null;
      return this.api.getLatestLogs(this.project.id, skipLines, expectingStageId);
    } else {
      this.logs = [];
      return this.api.getLog(this.project.id, this.watchLogsId);
    }
  }

  loadHistory() {
    this.longLoading.increase();
    return this.api
      .getProjectHistory(this.project.id)
      .then(history => {
        history = history.reverse();
        return this.api.getProjectEnqueued(this.project.id)
          .then(enqueued => {
            // remember state before adding to other history entires
            for (let i = 0; i < enqueued.length; ++i) {
              enqueued[i].enqueueIndex = i;
              enqueued[i].enqueueControlSize = enqueued.length;
            }

            const latest = enqueued.reverse();
            history.forEach(h => latest.push(h));
            if (this.history === null || this.history.length !== latest.length || JSON.stringify(this.history) !== JSON.stringify(latest)) {
              this.history = latest;
            }
          });
      })
      .finally(() => this.longLoading.decrease());
  }

  loadPaused(): void {
    this.longLoading.increase();
    this.api.getProjectPaused(this.project.id)
      .then(paused => this.paused = paused)
      .finally(() => this.longLoading.decrease());
  }


  toDate(time: number) {
    if (time) {
      return new Date(time).toLocaleString();
    } else {
      return '';
    }
  }

  enqueue(pipeline: PipelineInfo, stage: StageInfo, env: any, image: ImageInfo) {
    if (pipeline.name === this.project.pipelineDefinition.name) {
      let index = null;
      for (let i = 0; i < pipeline.stages.length; ++i) {
        if (pipeline.stages[i].name === stage.name) {
          index = i;
          break;
        }
      }
      if (index !== null) {
        this.dialog.openLoadingIndicator(
          this.api.enqueue(this.project.id, index, env, image),
          `Submitting selections`
        );
      }
    } else {
      this.dialog.error('Changing the Pipeline is not yet supported!');
    }
  }

  enqueueGroup(pipeline: PipelineInfo, stage: StageInfo, env: any, image: ImageInfo) {
    for (let i = 0; i < pipeline.stages.length; ++i) {
      if (stage.name === pipeline.stages[i].name) {
        return this.api.listProjects()
          .then(projects => {
            return this.matDialog
              .open(GroupSettingsDialogComponent, {
                data: {
                  projects,
                  availableTags: this.api.cachedTags,
                } as GroupSettingsDialogData
              })
              .afterClosed()
              .toPromise()
              .then(result => {
                if (result) {
                  return this.dialog.openLoadingIndicator(
                    this.api.configureGroup(result, pipeline.id, i, env, image),
                    `Applying settings on all selected projects`,
                  );
                }
              });
          });
      }
    }
  }

  updateRequestPause(pause: boolean, singleStageOnly?: boolean) {
    const before = this.paused;
    this.paused = pause;
    this.dialog.openLoadingIndicator(
      this.api
        .resume(this.project.id, pause, singleStageOnly)
        .then(result => {
          if (!this.paused) {
            this.stateEmitter.emit(this.stateValue = State.Running);
            this.pauseReason = null;
            this.openLogs(null, true);
          }
        })
        .catch(err => {
          this.paused = before;
          return Promise.reject(err);
        })
    );
  }

  startLoading() {
    this.onSelectedTabChanged(this.tabs.selectedIndex);
  }

  stopLoading() {
    this.onSelectedTabChanged(null);
  }

  onSelectedTabChanged(index: number) {
    this.watchPaused = this.conditionally(0 === index, () => this.loadPaused());
    this.watchHistory = this.conditionally(1 === index, () => this.loadHistory());
    this.watchLogs = this.conditionally(3 === index, () => this.loadLogs());
  }

  conditionally(condition: boolean, fn): boolean {
    if (condition) {
      fn();
    }
    return condition;
  }

  isLongLoading() {
    return this.longLoading.isLongLoading();
  }

  openFolder(project: Project, entry: HistoryEntry) {
    this.tabs.selectedIndex = 2;
    this.setupFiles(project);
    this.filesNavigationTarget = `/workspaces/${project.id}/${entry.workspace}/`;
  }

  private setupFiles(project = this.projectValue) {
    if (this.projectValue != null) {
      this.filesAdditionalRoot = `${project.name};workspaces/${project.id}`;
    }
  }


  openLogs(entry?: HistoryEntry, watchLatestLogs = false) {
    this.tabs.selectedIndex = 3;
    this.logs = null;
    this.watchLogs = true;
    if (entry != null) {
      this.watchLogsId = entry.stageId;
      this.watchLatestLogs = false;
    }
    if (watchLatestLogs) {
      this.watchLatestLogs = true;
    }
  }

  showLatestLogs(force: boolean) {
    if (!this.watchLatestLogs || force) {
      this.logs = null;
      this.watchLogs = true;
      this.watchLogsId = null;
      this.watchLatestLogs = true;
      this.loadLogsOnceAnyway = true;
      this.loadLogs();
    }
  }

  sourceIsManagement(source: LogSource) {
    return source === LogSource.MANAGEMENT_EVENT;
  }

  forceReloadLogs() {
    this.logs = [];
    this.loadLogs();
  }

  setName(name: string) {
    this.dialog.openLoadingIndicator(
      this.api
        .setName(this.project.id, name)
        .then(result => {
          this.project.name = name;
        }),
      `Updating name`
    );
  }

  delete() {
    this.dialog.openAreYouSure(
      `Project being deleted: ${this.project.name}`,
      () => this.api.delete(this.project.id).then(result => {
        this.deletedEmitter.emit(true);
      })
    );
  }

  onConsoleScroll($event: Event) {
    const element = $event.target as HTMLDivElement;
    this.stickConsole = (element.scrollHeight - element.clientHeight) <= element.scrollTop;
  }

  onWindowScroll() {
    if (this.htmlConsole != null) {
      const rangeSize = 50;
      let element = this.htmlConsole.nativeElement;
      let offset = element.offsetHeight - window.innerHeight + rangeSize;

      while (element) {
        offset += element.offsetTop;
        element = element.offsetParent as HTMLElement;
      }

      this.stickConsole = offset <= window.scrollY + rangeSize && offset >= window.scrollY - rangeSize;
    }
  }

  scrollConsoleToBottom(overwrite = false) {
    if (this.stickConsole || overwrite) {
      this.stickConsole = true;
      setTimeout(() => this.htmlConsole.nativeElement.scrollTop = 9_999_999_999);
      setTimeout(() => this.scrollToBottomTarget());
    }
  }

  killCurrentStage() {
    this.dialog.openAreYouSure(
      `Kill currently running stage of project ${this.project.name}`,
      () => this.api.killStage(this.project.id)
    );
  }

  prepareEnqueue(entry: HistoryEntry) {
    const stageInfo = new StageInfo();
    stageInfo.image = ProjectViewComponent.deepClone(entry.imageInfo);
    stageInfo.name = entry.stageName;
    stageInfo.requiredEnvVariables = entry.env && entry.env.keys ? [...entry.env.keys()] : [];

    this.executionSelection.image = stageInfo.image;
    this.executionSelection.selectedStage = stageInfo;
    this.defaultEnvVars = entry.env;
    this.tabs.selectedIndex = 0;
  }

  private scrollToBottomTarget(smooth = true) {
    this.scrollBottomTarget.nativeElement.scrollIntoView({
      behavior: smooth ? 'smooth' : 'auto',
      block: 'end'
    });
  }

  scrollConsoleToBottomTimeout(checked: boolean) {
    setTimeout(() => {
      if (checked) {
        this.scrollConsoleToBottom(checked);
      }
    });
  }

  cancelEnqueuedStage(index: number, controlSize: number) {
    this.dialog.openAreYouSure(
      `Remove enqueued stage from project ${this.project.name}`,
      () => this.api.deleteEnqueued(this.project.id, index, controlSize)
    );
  }

  setTags(tags: string[]) {
    return this.dialog.openLoadingIndicator(
      this.api
        .setTags(this.project.id, tags)
        .then(result => {
          this.project.tags = tags;
        }),
      'Updating tags'
    );
  }

  onSelectedPipelineChanged(info: PipelineInfo) {
    this.selectedPipeline = info;
  }

  onSelectedStageChanged(info: StageInfo) {
    this.selectedStage = info;
    if (this.selectedPipeline != null && this.selectedStage != null) {
      if (this.selectedPipeline.name === this.project.pipelineDefinition.name) {
        let index = null;
        for (let i = 0; i < this.selectedPipeline.stages.length; ++i) {
          if (this.selectedPipeline.stages[i].name === this.selectedStage.name) {
            index = i;
            break;
          }
        }

        this.dialog.openLoadingIndicator(
          this.api
            .getEnvironment(this.project.id, index)
            .then(result => {
              this.defaultEnvVars = result;
            }),
          `Loading environment variables`,
          false
        );
      }
    }
  }

  getProjectPipelineId(): string {
    if (this.pipelines != null) {
      for (const pipeline of this.pipelines) {
        if (this.project.pipelineDefinition.name === pipeline.name) {
          return pipeline.id;
        }
      }
    }
    return null;
  }

  setPipeline(pipelineId: string) {
    for (const pipeline of this.pipelines) {
      if (pipelineId === pipeline.id) {
        this.dialog.openLoadingIndicator(
          this.api
            .setPipelineDefinition(this.project.id, pipelineId)
            .then(successful => {
              if (successful) {
                this.project.pipelineDefinition = pipeline;
                this.executionSelection.defaultPipelineId = pipelineId;
                return Promise.resolve();
              } else {
                return Promise.reject();
              }
            }),
          `Submitting Pipeline selection`,
          true
        );
        break;
      }
    }
  }
}

