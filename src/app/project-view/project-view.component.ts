import {AfterViewInit, Component, ElementRef, EventEmitter, Input, OnDestroy, OnInit, Output, ViewChild} from '@angular/core';
import {
  DeletionPolicy,
  EnvVariable,
  ExecutionGroupInfo,
  ImageInfo,
  LogEntry,
  LogSource,
  ParseError,
  ProjectApiService,
  ProjectInfo,
  RangedWithStepSize,
  StageDefinitionInfo,
  StageInfo,
  State,
  StateInfo,
  WorkspaceConfiguration,
  WorkspaceMode
} from '../api/project-api.service';
import {NotificationService} from '../notification.service';
import {MatDialog, MatTabGroup} from '@angular/material';
import {LongLoadingDetector} from '../long-loading-detector';
import {PipelineApiService, PipelineInfo, ResourceInfo} from '../api/pipeline-api.service';
import {StageExecutionSelectionComponent} from '../stage-execution-selection/stage-execution-selection.component';
import {GroupSettingsDialogComponent, GroupSettingsDialogData} from '../group-settings-dialog/group-settings-dialog.component';
import {DialogService} from '../dialog.service';
import {PipelineEditorComponent} from '../pipeline-editor/pipeline-editor.component';
import {ActivatedRoute, Router} from '@angular/router';
import {pipe, Subscription} from 'rxjs';
import {environment} from '../../environments/environment';


@Component({
  selector: 'app-project-view',
  templateUrl: './project-view.component.html',
  styleUrls: ['./project-view.component.css']
})
export class ProjectViewComponent implements OnInit, OnDestroy, AfterViewInit {


  constructor(public api: ProjectApiService, private notification: NotificationService,
              private pipelinesApi: PipelineApiService, private matDialog: MatDialog,
              private dialog: DialogService,
              private route: ActivatedRoute,
              private router: Router) {
  }

  @Input()
  public set project(value: ProjectInfo) {
    this.projectValue = value;
    this.logs = null;
    this.history = null;
    this.rawPipelineDefinition = this.rawPipelineDefinitionError = this.rawPipelineDefinitionSuccess = null;
    this.deletionPolicyLocal = null;
    this.deletionPolicyRemote = null;
    this.pollWatched(true);
    this.setupFiles();
    this.loadHistory();

    this.pipelinesApi.getPipelineDefinitions().then(result => {
      this.pipelines = result.filter(pipe(p => !p.hasActionMarker()));
      this.probablyProjectPipelineId = null;
      if (this.project && this.project.pipelineDefinition) {
        for (const pipeline of this.pipelines) {
          if (pipeline.name === this.project.pipelineDefinition.name) {
            this.probablyProjectPipelineId = pipeline.id;
            break;
          }
        }
      }
    });
    this.updateExecutionSelectionPipelines();
    this.api.getDeletionPolicy(this.project.id).then(policy => {
      this.deletionPolicyLocal = policy;
      this.deletionPolicyRemote = policy;
    });

    if (this.tabs) {
      this.selectTabIndex(this.selectedTabIndex);
    }

    this.api.getWorkspaceConfigurationMode(this.projectValue.id).then(mode => this.workspaceConfigurationMode = mode);
  }

  public get project(): ProjectInfo {
    return this.projectValue;
  }

  @Input()
  public set state(value: StateInfo) {
    this.update(value);
  }

  static ALWAYS_INCLUDE_FIRST_N_LINES = 100;
  static TRUNCATE_TO_MAX_LINES = 5000;
  static DEFAULT_VISIBLE_ITEM_COUNT_HISTORY = 10;

  tabIndexOverview = Tab.Overview;

  @ViewChild('tabGroup') tabs: MatTabGroup;
  @ViewChild('console') htmlConsole: ElementRef<HTMLElement>;
  @ViewChild('scrollBottomTarget') scrollBottomTarget: ElementRef<HTMLElement>;
  @ViewChild('executionSelection') executionSelection: StageExecutionSelectionComponent;

  private projectValue: ProjectInfo;
  probablyProjectPipelineId = null;

  @Output('state') private stateEmitter = new EventEmitter<State>();
  @Output('deleted') private deletedEmitter = new EventEmitter<boolean>();

  filesAdditionalRoot: string = null;
  filesNavigationTarget: string = null;

  stateValue?: State = null;
  history?: ExecutionGroupInfo[] = null;
  logs?: LogEntry[] = null;
  paused: boolean = null;
  pauseReason?: string = null;
  progress?: number;

  deletionPolicyLocal?: DeletionPolicy;
  deletionPolicyRemote?: DeletionPolicy;

  watchHistory = false;
  watchPaused = false;
  watchLogs = false;
  watchLogsInterval: any = null;
  watchLogsId?: string = null;
  watchLatestLogs = true;
  watchDefinition = false;

  loadLogsOnceAnyway = false;
  loadHistoryAnyway = false;

  longLoading = new LongLoadingDetector();

  stickConsole = true;
  consoleIsLoading = false;
  scrollCallback;

  pipelines: PipelineInfo[];

  selectedPipeline: PipelineInfo = null;
  selectedStage: StageDefinitionInfo = null;
  environmentVariables: Map<string, EnvVariable> = null;
  defaultEnvironmentVariables: Map<string, string> = null;
  rangedEnvironmentVariables: Map<string, RangedWithStepSize> = null;
  workspaceConfigurationMode: WorkspaceMode = null;

  rawPipelineDefinition: string = null;
  rawPipelineDefinitionError: string = null;
  rawPipelineDefinitionSuccess: string = null;

  paramsSubscription: Subscription = null;
  selectedTabIndex: number = Tab.Overview;
  maxHistoryItemsToDisplay = ProjectViewComponent.DEFAULT_VISIBLE_ITEM_COUNT_HISTORY;
  workspaceMode: WorkspaceMode = null;

  private static deepClone(obj: any): any {
    return JSON.parse(JSON.stringify(obj));
  }

  updateTabSelection(tab: string) {
    for (let i = 0; i < 10; ++i) {
      if (Tab[i] && Tab[i].toLowerCase() === tab) {
        this.selectedTabIndex = i;
        this.onSelectedTabChanged(this.selectedTabIndex);
        break;
      }
    }
  }

  ngOnInit(): void {
    this.setupFiles();
    this.scrollCallback = () => this.onWindowScroll();
    window.addEventListener('scroll', this.scrollCallback, true);

    this.paramsSubscription = this.route.children[0].params.subscribe(params => {
      if (params.tab != null) {
        this.updateTabSelection(params.tab);
      }
    });
  }

  ngAfterViewInit() {
    this.updateExecutionSelectionPipelines();
  }

  ngOnDestroy(): void {
    window.removeEventListener('scroll', this.scrollCallback, true);
    if (this.paramsSubscription) {
      this.paramsSubscription.unsubscribe();
      this.paramsSubscription = null;
    }
  }

  private updateExecutionSelectionPipelines() {
    if (this.executionSelection != null && this.project != null) {
      this.executionSelection.pipelines = [this.project.pipelineDefinition];
      this.executionSelection.defaultPipelineId = this.project.pipelineDefinition.id;
    }
  }

  update(info: StateInfo) {
    if (!info) {
      return;
    }

    this.stateValue = info.actualState();
    this.pauseReason = info.pauseReason;
    this.progress = info.stageProgress;

    this.paused = this.stateValue === State.Paused || this.pauseReason != null;

    this.stateEmitter.emit(this.stateValue);
    this.pollWatched();
  }

  isEnqueued(state = this.stateValue): boolean {
    return State.Enqueued === state;
  }

  isRunning(state = this.stateValue): boolean {
    return State.Running === state;
  }

  pollWatched(forceUpdateOnWatched = false): void {
    if (this.watchHistory && (this.isRunning() || this.loadHistoryAnyway || forceUpdateOnWatched)) {
      this.loadHistoryAnyway = false;
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
    } else if (this.watchDefinition && this.rawPipelineDefinition == null) {
      this.loadRawPipelineDefinition();
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
          if (this.logs.length > ProjectViewComponent.TRUNCATE_TO_MAX_LINES) {
            this.logs.splice(
              ProjectViewComponent.ALWAYS_INCLUDE_FIRST_N_LINES,
              this.logs.length - ProjectViewComponent.TRUNCATE_TO_MAX_LINES
            );
          }
          if (logs.length > 0) {
            this.scrollConsoleToBottom();
            // execute it also after the DOM update
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
    if (this.watchLatestLogs || this.isStageRunning(this.watchLogsId)) {
      const skipLines = this.logs != null && this.logs.length > 0 ? this.logs[this.logs.length - 1].line : 0;
      const expectingStageId = this.logs != null && this.logs.length > 0 ? this.logs[0].stageId : null;
      return this.api.getLatestLogs(this.project.id, skipLines, expectingStageId, this.watchLogsId);
    } else {
      if (this.logs != null && this.logs.length > 0 && this.logs[0].stageId === this.watchLogsId) {
        return Promise.reject('already loaded');
      } else {
        this.logs = [];
        return this.api.getLog(this.project.id, this.watchLogsId);
      }
    }
  }

  isStageRunning(stageId: string) {
    if (this.history != null) {
      for (const entry of this.history) {
        // TODO double check!
        if (stageId.startsWith(entry.id)) {
          for (const stage of entry.stages) {
            if (stage.id === stageId) {
              return stage.state === State.Running;
            }
          }
        }
      }
    }
    return false;
  }

  loadHistory() {
    this.longLoading.increase();
    return this.api
      .getProjectHistory(this.project.id)
      .then(history => {
        return this.updateHistory(history);
      })
      .catch(e => {
        this.dialog.error(e);
        console.log(e);
      })
      .finally(() => this.longLoading.decrease());
  }

  private updateHistory(history: ExecutionGroupInfo[]) {
    history = history.reverse();
    return this.api.getProjectEnqueued(this.project.id)
      .then(enqueued => {

        this.loadHistoryAnyway = (enqueued.length > 0 && this.stateValue !== State.Paused)
          || (history.length > 0 && history[0].hasRunningStages());

        const latest = enqueued.reverse();
        history.forEach(h => latest.push(h));
        if (this.history === null || this.history.length !== latest.length || JSON.stringify(this.history) !== JSON.stringify(latest)) {
          this.history = latest;
        }
      });
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

  enqueue(
    pipeline: PipelineInfo,
    stageDefinitionInfo: StageDefinitionInfo,
    env: any,
    rangedEnv: any,
    image: ImageInfo,
    requiredResources?: ResourceInfo,
    workspaceConfiguration?: WorkspaceConfiguration
  ) {
    if (pipeline.name === this.project.pipelineDefinition.name) {
      let index = null;
      for (let i = 0; i < pipeline.stages.length; ++i) {
        if (pipeline.stages[i].name === stageDefinitionInfo.name) {
          index = i;
          break;
        }
      }
      if (index !== null) {
        this.dialog.openLoadingIndicator(
          this.api.enqueue(this.project.id, index, env, rangedEnv, image, requiredResources, workspaceConfiguration),
          `Submitting selections`
        );
      }
    } else {
      this.dialog.error('Changing the Pipeline is not yet supported!');
    }
  }

  configure(pipeline: PipelineInfo, stage: StageDefinitionInfo, env: any, image: ImageInfo, requiredResources?: ResourceInfo) {
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
          this.api.configureGroup(this.project.id, index, [this.project.id], env, image, requiredResources),
          `Submitting selections`
        );
      }
    } else {
      this.dialog.error('Changing the Pipeline is not yet supported!');
    }
  }

  configureGroup(pipeline: PipelineInfo, stage: StageDefinitionInfo, env: any, image: ImageInfo) {
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
              .then(selectedProjects => {
                if (selectedProjects) {
                  return this.dialog.openLoadingIndicator(
                    this.api.configureGroup(this.project.id, i, selectedProjects, env, image)
                      .then(configureResult => {
                        const failed = [];
                        for (let n = 0; n < configureResult.length && n < selectedProjects.length; ++n) {
                          if (!configureResult[n]) {
                            failed.push(selectedProjects[n]);
                          }
                        }
                        if (failed.length === 0) {
                          return Promise.resolve();
                        } else {
                          return Promise.reject('The operation failed for at least one project: ' + (failed.join(', ')));
                        }
                      }),
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
          }
        })
        .catch(err => {
          this.paused = before;
          return Promise.reject(err);
        })
    );
  }

  startLoading() {
    this.selectTabIndex(this.selectedTabIndex);
  }

  stopLoading() {
    this.onSelectedTabChanged(null);
  }

  selectTabIndex(index: number) {
    this.router.navigate([Tab[index].toLowerCase()], {
      relativeTo: this.route,
    });
  }

  onSelectedTabChanged(index: number) {
    if (this.tabs && index != null) {
      this.tabs.selectedIndex = index;
    }

    this.watchPaused = this.conditionally(Tab.Control === index, () => this.loadPaused());
    this.watchHistory = this.conditionally(Tab.History === index || Tab.Overview === index, () => {
      this.maxHistoryItemsToDisplay = ProjectViewComponent.DEFAULT_VISIBLE_ITEM_COUNT_HISTORY;
      this.loadHistory();
    });
    this.watchLogs = this.conditionally(Tab.Logs === index, () => this.loadLogs());
    this.watchDefinition = this.conditionally(Tab.PipelineDefinition === index, () => this.loadRawPipelineDefinition());
  }

  conditionally(condition: boolean, fn): boolean {
    if (condition) {
      fn();
    }
    return condition;
  }

  loadRawPipelineDefinition() {
    this.dialog.openLoadingIndicator(
      this.api.getProjectRawPipelineDefinition(this.project.id)
        .then(result => this.rawPipelineDefinition = result),
      `Loading Pipeline Definition`,
      false
    );
  }

  isLongLoading() {
    return this.longLoading.isLongLoading();
  }

  openFolder(project: ProjectInfo, group: ExecutionGroupInfo) {
    if (group != null && group.stages != null && group.stages.length > 0) {
      const stage = group.stages[group.stages.length - 1];
      this.tabs.selectedIndex = Tab.Files;
      this.setupFiles(project);
      this.filesNavigationTarget = `/workspaces/${stage.workspace}/`;
    }
  }

  openWorkspace(project: ProjectInfo, stage: StageInfo) {
    this.tabs.selectedIndex = Tab.Files;
    this.setupFiles(project);
    this.filesNavigationTarget = `/workspaces/${stage.workspace}/`;
  }

  openTensorboard(project: ProjectInfo, entry: StageInfo) {
    window.open(`${environment.apiLocation}tensorboard/${project.id}/${entry.id}/start`, '_blank');
  }

  private setupFiles(project = this.projectValue) {
    if (this.projectValue != null) {
      this.filesAdditionalRoot = `${project.name};workspaces/${project.id}`;
    }
  }


  openLogs(entry?: StageInfo, watchLatestLogs = false) {
    this.tabs.selectedIndex = Tab.Logs;
    this.logs = null;
    this.watchLogs = true;
    if (entry != null) {
      this.watchLogsId = entry.id;
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

  scrollConsoleToTop() {
    this.stickConsole = false;
    this.htmlConsole.nativeElement.scrollTop = 0;
  }

  scrollConsoleToBottom(overwrite = false) {
    if (this.stickConsole || overwrite) {
      this.stickConsole = true;
      setTimeout(() => {
        if (this.htmlConsole) {
          this.htmlConsole.nativeElement.scrollTop = 9_999_999_999;
        }
      });
      setTimeout(() => this.scrollToBottomTarget());
    }
  }

  killStage(stageId: string) {
    this.dialog.openAreYouSure(
      `Kill  running stage ${stageId}`,
      () => this.api.killStage(this.project.id, stageId).then()
    );
  }

  killAllStages() {
    this.dialog.openAreYouSure(
      `Kill all running stages of project ${this.project.name}`,
      () => this.api.killStage(this.project.id, null).then()
    );
  }

  useAsBlueprint(group: ExecutionGroupInfo, entry?: StageInfo) {
    this.executionSelection.image = group.stageDefinition.image;
    this.executionSelection.resources = group.stageDefinition.requiredResources;
    this.executionSelection.selectedStage = group.stageDefinition;
    this.executionSelection.workspaceConfiguration = group.workspaceConfiguration;
    this.environmentVariables = new Map();
    this.defaultEnvironmentVariables = entry != null ? entry.env : new Map();
    this.rangedEnvironmentVariables = entry == null && group.rangedValues != null ? new Map(group.rangedValues) : new Map();
    this.tabs.selectedIndex = Tab.Control;
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

  cancelEnqueuedStage(groupId: string) {
    this.dialog.openAreYouSure(
      `Remove enqueued stage from project ${this.project.name}`,
      () => this.api.deleteEnqueued(this.project.id, groupId).then()
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

  onSelectedStageChanged(info: StageDefinitionInfo) {
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
              this.environmentVariables = result;
            }),
          `Loading environment variables`,
          false
        );
      }
    }
  }

  setPipeline(pipelineId: string) {
    for (const pipeline of this.pipelines) {
      if (pipelineId === pipeline.id) {
        this.dialog.openLoadingIndicator(
          this.api
            .setPipelineDefinition(this.project.id, pipelineId)
            .then(successful => {
              if (successful) {
                this.setProjectPipeline(pipeline);
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

  private setProjectPipeline(pipeline: PipelineInfo) {
    const p = this.project;
    const pid = p.pipelineDefinition.id;
    p.pipelineDefinition = ProjectViewComponent.deepClone(pipeline);
    p.pipelineDefinition.id = pid;
    this.project = p;
  }

  downloadUrl() {
    if (this.logs != null && this.logs.length > 0) {
      return this.api.getLogRawUrl(this.project.id, this.logs[0].stageId);
    } else {
      return '';
    }
  }

  checkPipelineDefinition(raw: string) {
    this.dialog.openLoadingIndicator(
      this.pipelinesApi.checkPipelineDefinition(raw)
        .then(result => {
          if (result != null) {
            this.rawPipelineDefinitionSuccess = null;
            this.rawPipelineDefinitionError = result;
          } else {
            this.rawPipelineDefinitionSuccess = 'Looks good!';
            this.rawPipelineDefinitionError = null;
          }
        }),
      `Checking Pipeline Definition`,
      false
    );
  }

  updatePipelineDefinition(raw: string, editor: PipelineEditorComponent) {
    this.dialog.openLoadingIndicator(
      this.api.setProjectRawPipelineDefinition(this.project.id, raw)
        .catch(e => {
          if (ParseError.canShadow(e)) {
            editor.parseError = [e];
            return Promise.reject('Failed to parse input, see marked area(s) for more details');
          } else {
            editor.parseError = [];
            return Promise.reject(e);
          }
        })
        .then(r => {
          editor.parseError = [];
          return this.api
            .getProjectPipelineDefinition(this.project.id)
            .then(definition => {
              this.setProjectPipeline(definition);
            });
        }),
      `Saving Pipeline Definition`,
      true
    );
  }

  updatePipelineDefinitionOnOthers(raw: string) {
    this.api.listProjects()
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
              const promises = [];
              for (const projectId of result) {
                promises.push(this.api.setProjectRawPipelineDefinition(
                  projectId,
                  raw
                ).catch(e => 'At least one update failed: ' + e));
              }
              this.dialog.openLoadingIndicator(
                Promise.all(promises),
                `Updating Projects`,
                true
              );
            }
          });
      });
  }

  maybeResetDeletionPolicy(reset: boolean) {
    const reApplyCurrentState = () => {
      const before = JSON.parse(JSON.stringify(this.deletionPolicyLocal));
      this.deletionPolicyLocal = null;
      if (before != null) {
        setTimeout(() => {
          this.deletionPolicyLocal = before;
        });
      }
    };
    if (reset) {
      this.deletionPolicyLocal = null;
    } else {
      if (this.deletionPolicyLocal === null) {
        this.dialog.openLoadingIndicator(
          this.api.getDefaultDeletionPolicy(this.project.id)
            .then(result => this.deletionPolicyLocal = result)
            .catch(e => {
              reApplyCurrentState();
              return Promise.reject(e);
            }),
          'Loading default policy',
          false
        );
      }
    }
  }

  updateDeletionPolicy(set: boolean, limitStr: string, keep: boolean) {
    let promise = null;
    if (set) {
      const policy = new DeletionPolicy();
      policy.numberOfWorkspacesOfSucceededStagesToKeep = Number(limitStr) > 0 ? Number(limitStr) : null;
      policy.keepWorkspaceOfFailedStage = keep;
      promise = this.api.updateDeletionPolicy(this.project.id, policy)
        .then(result => {
          this.deletionPolicyLocal = result;
          this.deletionPolicyRemote = result;
        });
    } else {
      promise = this.api.resetDeletionPolicy(this.project.id)
        .then(r => {
          this.deletionPolicyLocal = null;
          this.deletionPolicyRemote = null;
        });
    }
    this.dialog.openLoadingIndicator(
      promise,
      'Updating Deletion Policy'
    );
  }

  incrementMaxHistoryItemsToDisplay(range: number = 1) {
    this.maxHistoryItemsToDisplay += range;
  }

  updatePublicAccess(checked: boolean) {
    this.dialog.openLoadingIndicator(
      this.api.updatePublicAccess(this.projectValue.id, checked)
        .then(v => this.projectValue.publicAccess = v),
      `Updating public access property`
    );
  }

  pruneHistory() {
    this.dialog.openAreYouSure(
      `Delete all failed stages of this project`,
      () => this.api.pruneHistory(this.projectValue.id)
        .then(updatedHistory => {
          return this.updateHistory(updatedHistory);
        }),
    );
  }

  tryParseStageNumber(stageId: string, alt: number): number {
    return this.api.tryParseGroupNumber(stageId, alt);
  }

  workspaceModes(): WorkspaceMode[] {
    return [WorkspaceMode.STANDALONE, WorkspaceMode.INCREMENTAL, WorkspaceMode.CONTINUATION];
  }

  setWorkspaceMode(value: WorkspaceMode) {
    this.dialog.openLoadingIndicator(
      this.api.setWorkspaceConfigurationMode(this.projectValue.id, value)
        .then(mode => {
          this.workspaceMode = mode;
        }),
      `Updating workspace configuration mode`,
    );
  }
}


export enum Tab {
  Overview,
  Control,
  History,
  Files,
  Logs,
  PipelineDefinition,
  Settings
}
