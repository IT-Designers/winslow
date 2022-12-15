import {
  AfterViewInit,
  Component,
  EventEmitter,
  HostListener,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  Output,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import {
  AuthTokenInfo,
  DeletionPolicy,
  EnvVariable,
  ExecutionGroupInfoExt,
  ProjectInfoExt, ResourceLimitationExt, StageInfoExt, WorkspaceConfigurationExt,
  ParseError,
  ProjectApiService,
} from '../api/project-api.service';
import {NotificationService} from '../notification.service';
import {MatDialog} from '@angular/material/dialog';
import {MatTabGroup} from '@angular/material/tabs';
import {LongLoadingDetector} from '../long-loading-detector';
import {PipelineApiService} from '../api/pipeline-api.service';
import {StageExecutionSelectionComponent} from '../stage-execution-selection/stage-execution-selection.component';
import {GroupSettingsDialogComponent, GroupSettingsDialogData} from '../group-settings-dialog/group-settings-dialog.component';
import {DialogService} from '../dialog.service';
import {PipelineEditorComponent} from '../pipeline-editor/pipeline-editor.component';
import {ActivatedRoute, Router} from '@angular/router';
import {pipe, Subscription} from 'rxjs';
import {environment} from '../../environments/environment';
import {
  ImageInfo,
  PipelineDefinitionInfo,
  IRangedValue,
  ResourceInfo,
  StageDefinitionInfo,
  StageInfo,
  State, WorkspaceMode, StateInfo, ExecutionGroupInfo, StageWorkerDefinitionInfo
} from '../api/winslow-api';


@Component({
  selector: 'app-project-view',
  templateUrl: './project-view.component.html',
  styleUrls: ['./project-view.component.css']
})
export class ProjectViewComponent implements OnInit, OnDestroy, OnChanges, AfterViewInit {


  constructor(public api: ProjectApiService, private notification: NotificationService,
              private pipelinesApi: PipelineApiService, private matDialog: MatDialog,
              private dialog: DialogService,
              private route: ActivatedRoute,
              private router: Router) {
    this.setHistoryListHeight(window.innerHeight);
  }

  @Input()
  public set project(value: ProjectInfoExt) {
    const changed = this.projectValue?.id !== value?.id;
    this.projectValue = value;

    if (changed) {
      this.rawPipelineDefinition = this.rawPipelineDefinitionError = this.rawPipelineDefinitionSuccess = null;

      this.deletionPolicyLocal = null;
      this.deletionPolicyRemote = null;
      this.api.getDeletionPolicy(this.project.id).then(policy => {
        this.deletionPolicyLocal = policy;
        this.deletionPolicyRemote = policy;
      });

      this.setupFiles();

      if (this.tabs) {
        this.selectTabIndex(this.selectedTabIndex);
      }

      this.api.getWorkspaceConfigurationMode(this.projectValue.id).then(mode => this.workspaceConfigurationMode = mode);
      this.api.getResourceLimitation(this.projectValue.id).then(limit => this.resourceLimit = limit);
      this.api.getAuthTokens(this.projectValue.id).then(tokens => this.authTokens = tokens);
      this.resubscribe(value.id);
    }

    this.pipelinesApi.getPipelineDefinitions().then(result => {
      this.pipelines = result.filter(pipe(p => !p.hasActionMarker()));
      this.probablyProjectPipelineId = null;
      if (this.project && this.project.pipelineDefinition) {
        this.probablyProjectPipelineId = this.api.findProjectPipeline(this.project, this.pipelines);
      }
    });
    this.updateExecutionSelectionPipelines();

  }

  public get project(): ProjectInfoExt {
    return this.projectValue;
  }

  @Input()
  public set state(value: StateInfo) {
    this.update(value);
  }

  static TRUNCATE_TO_MAX_LINES = 5000;


  tabIndexOverview = Tab.Overview;

  @ViewChild('tabGroup') tabs: MatTabGroup;
  @ViewChild('executionSelection') executionSelection: StageExecutionSelectionComponent;

  projectValue: ProjectInfoExt;
  probablyProjectPipelineId = null;

  @Output('state') private stateEmitter = new EventEmitter<State>();
  @Output('deleted') private deletedEmitter = new EventEmitter<boolean>();

  filesAdditionalRoot: string = null;
  filesNavigationTarget: string = null;

  stageIdToDisplayLogsFor: string = null;
  stateValue?: State = null;

  history: ExecutionGroupInfoExt[] = [];
  subscribedProjectId: string = null;
  historySubscription: Subscription = null;
  historyEnqueued = 0;
  historyEnqueuedSubscription: Subscription = null;
  historyExecuting = 0;
  historyExecutingSubscription: Subscription = null;
  historyCanLoadMoreEntries = true;

  paused: boolean = null;
  pauseReason?: string = null;
  progress?: number;

  deletionPolicyLocal?: DeletionPolicy;
  deletionPolicyRemote?: DeletionPolicy;

  longLoading = new LongLoadingDetector();

  pipelines: PipelineDefinitionInfo[];

  selectedPipeline: PipelineDefinitionInfo = null;
  selectedStage: StageDefinitionInfo = null;
  environmentVariables: Map<string, EnvVariable> = null;
  //defaultEnvironmentVariables: Map<string, string> = null;
  defaultEnvironmentVariables: { [p: string]: string } = null;
  //rangedEnvironmentVariables: Map<string, RangedValue> = null;
  rangedEnvironmentVariables: { [p: string]: IRangedValue } = null;
  workspaceConfigurationMode: WorkspaceMode = null;

  rawPipelineDefinition: string = null;
  rawPipelineDefinitionError: string = null;
  rawPipelineDefinitionSuccess: string = null;

  paramsSubscription: Subscription = null;
  selectedTabIndex: number = Tab.Overview;
  workspaceMode: WorkspaceMode = null;
  resourceLimit: ResourceLimitationExt = null;
  authTokens: AuthTokenInfo[] = null;

  historyListHeight: any;
  selectedHistoryEntry: ExecutionGroupInfoExt = null;
  selectedHistoryEntryNumber: number;
  selectedHistoryEntryIndex = 0;
  selectedHistoryEntryStage: StageInfo;

  // load more entries, when user is scrolling to the bottom
  // on project history list
  @HostListener('scroll', ['$event'])
  onScroll(event: any) {
    if (event.target.offsetHeight + event.target.scrollTop >= event.target.scrollHeight) {
      this.loadMoreHistoryEntries(10);
    }
  }


  @HostListener('window:resize', ['$event'])
  getScreenSize(event?) {
    this.setHistoryListHeight(window.innerHeight);
  }

  setHistoryListHeight(height: number) {
    this.historyListHeight = 0.295 * (height - 136);
  }


  setHistoryEntry(entry: ExecutionGroupInfoExt, index: number) {
    this.selectedHistoryEntry = entry;
    this.selectedHistoryEntryNumber = this.tryParseStageNumber(entry.id, this.history.length - index);
    this.selectedHistoryEntryIndex = index;

    if (entry.stages.length === 1) {
      this.selectedHistoryEntryStage = entry.stages[0];
    } else if (entry.stages.length < 1) {
      this.selectedHistoryEntryStage = new StageInfoExt();
    }
  }


  setHistoryEntryStage(stage: StageInfo) {
    this.selectedHistoryEntryStage = stage;
  }

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
    this.paramsSubscription = this.route.children[0].params.subscribe(params => {
      if (params.tab != null) {
        this.updateTabSelection(params.tab);
      }
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (let propName in changes) {
      let change = changes[propName];

      // reset selectedHistory if another project will be selected
      if (change?.currentValue?.id != change?.previousValue?.id) {
        this.selectedHistoryEntry = null;
        this.selectedHistoryEntryNumber = null;
        this.selectedHistoryEntryIndex = 0;
        this.selectedHistoryEntryStage = null;
      }
    }
  }

  ngAfterViewInit() {
    this.updateExecutionSelectionPipelines();
  }

  ngOnDestroy(): void {
    if (this.paramsSubscription) {
      this.paramsSubscription.unsubscribe();
      this.paramsSubscription = null;
    }
    this.unsubscribe();
  }

  private resubscribe(projectId: string) {
    if (this.subscribedProjectId !== projectId) {
      this.unsubscribe();
      this.subscribe(projectId);
    } else {
      console.warn('resubscribe request on same project id ' + projectId);
    }
  }

  private unsubscribe() {
    this.subscribedProjectId = null;
    this.unsubscribeHistory();
  }

  private subscribe(projectId: string) {
    this.subscribedProjectId = projectId;
    this.subscribeHistory(projectId);
  }

  private unsubscribeHistory() {
    if (this.historySubscription != null) {
      this.historySubscription.unsubscribe();
      this.historySubscription = null;
    }

    if (this.historyExecutingSubscription != null) {
      this.historyExecutingSubscription.unsubscribe();
      this.historyExecutingSubscription = null;
    }

    if (this.historyEnqueuedSubscription != null) {
      this.historyEnqueuedSubscription.unsubscribe();
      this.historyEnqueuedSubscription = null;
    }
  }

  private subscribeHistory(projectId: string) {
    this.history = [];
    this.historyEnqueued = 0;
    this.historyExecuting = 0;
    this.historyCanLoadMoreEntries = true;

    this.historyEnqueuedSubscription = this.api.watchProjectEnqueued(projectId, executions => {
      const offset = 0;
      const length = this.historyEnqueued;
      this.history.splice(offset, length, ...executions.reverse());
      this.historyEnqueued = executions.length;
    });

    this.historyExecutingSubscription = this.api.watchProjectExecutions(projectId, executions => {
      const offset = this.historyEnqueued;
      const length = this.historyExecuting;
      this.history.splice(offset, length, ...executions.reverse());
      this.historyExecuting = executions.length;
    });

    this.historySubscription = this.api.watchProjectHistory(projectId, executions => {
      const offset = this.historyEnqueued + this.historyExecuting;
      const length = this.history.length - offset;
      this.history.splice(offset, 0, ...executions.reverse());
    });
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

    this.stateValue = info.state;
    this.pauseReason = info.pauseReason;
    this.progress = info.stageProgress;

    this.paused = this.stateValue === 'Paused' || this.pauseReason != null;

    this.stateEmitter.emit(this.stateValue);
  }

  isEnqueued(state = this.stateValue): boolean {
    return state === 'Enqueued';
  }

  isRunning(state = this.stateValue): boolean {
    return state === 'Running';
  }

  enqueue(
    pipeline: PipelineDefinitionInfo,
    stageDefinitionInfo: StageDefinitionInfo,
    env: any,
    rangedEnv: any,
    image: ImageInfo,
    requiredResources?: ResourceInfo,
    workspaceConfiguration?: WorkspaceConfigurationExt,
    comment?: string,
    runSingle?: boolean,
    resume?: boolean,
  ) {
    if (pipeline.name === this.project.pipelineDefinition.name) {
      this.dialog.openLoadingIndicator(
        this.api.enqueue(
          this.project.id,
          stageDefinitionInfo.id,
          env,
          rangedEnv,
          image,
          requiredResources,
          workspaceConfiguration,
          comment,
          runSingle,
          resume),
        `Submitting selections`
      );
    } else {
      this.dialog.error('Changing the Pipeline is not yet supported!');
    }
  }

  configure(pipeline: PipelineDefinitionInfo, stage: StageDefinitionInfo, env: any, image: ImageInfo, requiredResources?: ResourceInfo) {
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

  configureGroup(pipeline: PipelineDefinitionInfo, stage: StageDefinitionInfo, env: any, image: ImageInfo) {
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
            this.stateEmitter.emit(this.stateValue = 'Running');
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

    this.conditionally(
      Tab.PipelineDefinition === index,
      () => this.loadRawPipelineDefinition(),
      () => this.rawPipelineDefinition = null
    );
  }

  conditionally(condition: boolean, fn, fnAlt = null): boolean {
    if (condition) {
      fn();
    } else if (fnAlt != null) {
      fnAlt();
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

  openFolder(project: ProjectInfoExt, group: ExecutionGroupInfoExt) {
    if (group != null && group.stages != null && group.stages.length > 0) {
      const stage = group.stages[group.stages.length - 1];
      this.tabs.selectedIndex = Tab.Files;
      this.setupFiles(project);
      this.filesNavigationTarget = `/workspaces/${stage.workspace}/`;
    }
  }

  openWorkspace(project: ProjectInfoExt, stage: StageInfo) {
    this.tabs.selectedIndex = Tab.Files;
    this.setupFiles(project);
    this.filesNavigationTarget = `/workspaces/${stage.workspace}/`;
  }

  openTensorboard(project: ProjectInfoExt, entry: StageInfo) {
    window.open(`${environment.apiLocation}tensorboard/${project.id}/${entry.id}/start`, '_blank');
  }

  private setupFiles(project = this.projectValue) {
    if (project != null) {
      this.filesAdditionalRoot = `${project.name};workspaces/${project.id}`;
    }
  }


  openLogs(entry?: StageInfo, watchLatestLogs = false) {
    this.stageIdToDisplayLogsFor = entry?.id;
    this.tabs.selectedIndex = Tab.Logs;
  }

  openAnalysis(entry?: StageInfo, watchLatestLogs = false) {
    this.stageIdToDisplayLogsFor = entry?.id;
    this.tabs.selectedIndex = Tab.Analysis;
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

  useAsBlueprint(group: ExecutionGroupInfoExt, entry?: StageInfo) {
    console.log('useAsBlueprint ' + (group.stageDefinition instanceof StageWorkerDefinitionInfo));
    if (group.stageDefinition instanceof StageWorkerDefinitionInfo) {
      this.executionSelection.image = group.stageDefinition.image;
      // TODO this.executionSelection.resources = group.stageDefinition.requiredResources;
      this.executionSelection.selectedStage = group.stageDefinition;
      this.executionSelection.workspaceConfiguration = group.workspaceConfiguration;
      this.executionSelection.comment = group.comment;
      this.environmentVariables = new Map();
      // TODO this.defaultEnvironmentVariables = entry != null ? entry.env : group.stageDefinition.env;
      this.rangedEnvironmentVariables = entry == null && group.rangedValues != null ? group.rangedValues : {};
      this.rangedEnvironmentVariables = this.rangedEnvironmentVariables ?? {};
      this.tabs.selectedIndex = Tab.Control;
    }
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

  onSelectedPipelineChanged(info: PipelineDefinitionInfo) {
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

  private setProjectPipeline(pipeline: PipelineDefinitionInfo) {
    const p = this.project;
    const pid = p.pipelineDefinition.id;
    p.pipelineDefinition = ProjectViewComponent.deepClone(pipeline);
    p.pipelineDefinition.id = pid;
    this.project = p;
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

  updateDeletionPolicy(set: boolean, limitStr: string, keep: boolean, always: boolean) {
    let promise = null;
    if (set) {
      const policy = new DeletionPolicy();
      policy.numberOfWorkspacesOfSucceededStagesToKeep = Number(limitStr) > 0 ? Number(limitStr) : null;
      policy.keepWorkspaceOfFailedStage = keep;
      policy.alwaysKeepMostRecentWorkspace = always;
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

  loadMoreHistoryEntries(count: number = 1) {
    const projectId = this.projectValue.id;
    const groupId = this.history[this.history.length - 1].id;
    this.historyCanLoadMoreEntries = false;
    // this.dialog.openLoadingIndicator(
    //   this.api.getProjectPartialHistory(
    //     projectId,
    //     groupId,
    //     count
    //   ).then(entries => {
    //     if (this.projectValue.id === projectId) {
    //       if (entries != null && entries.length > 0) {
    //         this.history.push(...entries);
    //         this.historyCanLoadMoreEntries = entries.length >= count;
    //       } else {
    //         this.historyCanLoadMoreEntries = false;
    //       }
    //     }
    //   }, err => {
    //     console.error(err);
    //   }),
    //   `Digging out old history entries`,
    //   true,
    //   true
    // );
    this.api.getProjectPartialHistory(
      projectId,
      groupId,
      count
    ).then(entries => {
      if (this.projectValue.id === projectId) {
        if (entries != null && entries.length > 0) {
          this.history.push(...entries);
          this.historyCanLoadMoreEntries = entries.length >= count;
        } else {
          this.historyCanLoadMoreEntries = false;
        }
      }
    }, err => {
      console.error(err);
    });
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
      () => this.api.pruneHistory(this.projectValue.id).then()
    );
  }

  tryParseStageNumber(stageId: string, alt: number): number {
    return this.api.tryParseGroupNumber(stageId, alt);
  }

  trackHistory(index: number, value: ExecutionGroupInfo): string {
    return value.id;
  }

  workspaceModes(): WorkspaceMode[] {
    const modes: WorkspaceMode[] = ['STANDALONE', 'INCREMENTAL', 'CONTINUATION'];
    return modes;
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

  setResourceLimitation(limit?: ResourceLimitationExt) {
    this.dialog.openLoadingIndicator(
      this.api.setResourceLimitation(this.projectValue.id, limit)
        .then(l => {
          this.resourceLimit = l;
        }),
      `Updating resource limitation`
    );
  }

  createAuthToken(name: string) {
    this.dialog.openLoadingIndicator(
      this.api.createAuthToken(this.projectValue.id, name)
        .then(l => {
          this.authTokens.push(l);
          // weird
          setTimeout(
            () => this.dialog.info(l.secret, 'The secret value is'),
            100
          );
          // this.dialog.info(`The secret for the new Auth-Token is: <br><pre>${l.secret}</pre>`);
        }),
      `Creating a new authentication token`
    );
  }

  deleteAuthToken(id: string) {
    this.dialog.openLoadingIndicator(
      this.api.deleteAuthToken(this.projectValue.id, id)
        .then(l => {
          for (let i = 0; i < this.authTokens.length; ++i) {
            if (this.authTokens[i].id === id) {
              this.authTokens.splice(i, 1);
              break;
            }
          }
        }),
      `Creating a new authentication token`
    );
  }
}


export enum Tab {
  Overview,
  Control,
  History,
  Files,
  Logs,
  Analysis,
  PipelineDefinition,
  PipelineView,
  Settings
}
