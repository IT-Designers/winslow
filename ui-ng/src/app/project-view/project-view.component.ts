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
import {DeletionPolicy, ProjectApiService,} from '../api/project-api.service';
import {NotificationService} from '../notification.service';
import {MatDialog} from '@angular/material/dialog';
import {MatTabGroup} from '@angular/material/tabs';
import {LongLoadingDetector} from '../long-loading-detector';
import {PipelineApiService} from '../api/pipeline-api.service';
import {StageExecutionSelectionComponent} from '../stage-execution-selection/stage-execution-selection.component';
import {
  GroupSettingsDialogComponent,
  GroupSettingsDialogData
} from '../group-settings-dialog/group-settings-dialog.component';
import {DialogService} from '../dialog.service';
import {PipelineEditorComponent} from '../pipeline-editor/pipeline-editor.component';
import {ActivatedRoute, Router} from '@angular/router';
import {pipe, Subscription} from 'rxjs';
import {environment} from '../../environments/environment';
import {GroupApiService, GroupInfo} from '../api/group-api.service';
import {
  AuthTokenInfo,
  EnvVariable,
  ExecutionGroupInfo,
  ImageInfo, Link,
  PipelineDefinitionInfo,
  ProjectInfo,
  RangedValue,
  ResourceInfo,
  ResourceLimitation,
  StageDefinitionInfo,
  StageInfo,
  StageWorkerDefinitionInfo,
  State,
  StateInfo,
  WorkspaceConfiguration,
  WorkspaceMode
} from '../api/winslow-api';
import {UserApiService} from '../api/user-api.service';


@Component({
  selector: 'app-project-view',
  templateUrl: './project-view.component.html',
  styleUrls: ['./project-view.component.css']
})
export class ProjectViewComponent implements OnInit, OnDestroy, OnChanges {

  constructor(
    public api: ProjectApiService,
    private pipelinesApi: PipelineApiService,
    private matDialog: MatDialog,
    private dialog: DialogService,
    private route: ActivatedRoute,
    private router: Router,
  ) {
    this.setHistoryListHeight(window.innerHeight);
  }

  @Input()
  public set project(value: ProjectInfo) {
    const changed = this.projectValue?.id !== value?.id;
    this.projectValue = value;

    if (changed) {
      this.rawPipelineDefinition = this.rawPipelineDefinitionError = this.rawPipelineDefinitionSuccess = null;

      this.setupFiles();

      if (this.tabs) {
        this.selectTabIndex(this.selectedTabIndex);
      }

      this.resubscribe(value.id);
    }

    this.pipelinesApi.getPipelineDefinitions().then(result => {
      this.pipelines = result;
      this.probablyProjectPipelineId = null;
      if (this.project && this.project.pipelineDefinition) {
        this.probablyProjectPipelineId = this.api.findProjectPipeline(this.project, this.pipelines);
      }
    });

  }

  public get project(): ProjectInfo {
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

  projectValue: ProjectInfo;
  probablyProjectPipelineId = null;

  @Output('state') stateEmitter = new EventEmitter<State>();
  @Output('deleted') deletedEmitter = new EventEmitter<boolean>();

  filesAdditionalRoot: string = null;
  filesNavigationTarget: string = null;

  stageIdToDisplayLogsFor: string = null;
  stateValue?: State = null;

  history: ExecutionGroupInfo[] = [];
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
  defaultEnvironmentVariables: Record<string, string> = null;
  rangedEnvironmentVariables: Record<string, RangedValue> = null;

  rawPipelineDefinition: string = null;
  rawPipelineDefinitionError: string = null;
  rawPipelineDefinitionSuccess: string = null;

  paramsSubscription: Subscription = null;
  selectedTabIndex: number = Tab.Overview;
  resourceLimit: ResourceLimitation = null;

  historyListHeight: any;
  selectedHistoryEntry: ExecutionGroupInfo = null;
  selectedHistoryEntryNumber: number;
  selectedHistoryEntryIndex = 0;
  selectedHistoryEntryStage: StageInfo;

  projectGroups: Link[];
  showGroupList = false;
  groupListBtnText = 'Expand';
  groupListBtnIcon = 'expand_more';

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


  setHistoryEntry(entry: ExecutionGroupInfo, index: number) {
    this.selectedHistoryEntry = entry;
    this.selectedHistoryEntryNumber = this.tryParseStageNumber(entry.id, this.history.length - index);
    this.selectedHistoryEntryIndex = index;

    if (entry.stages.length === 1) {
      this.selectedHistoryEntryStage = entry.stages[0];
    } else if (entry.stages.length < 1) {
      this.selectedHistoryEntryStage = new StageInfo({
        env: {},
        envInternal: {},
        envPipeline: {},
        envSystem: {},
        id: '',
        result: {}
      });
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
    this.sortGroups();
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

  update(info: StateInfo) {
    if (!info) {
      return;
    }

    this.stateValue = info.state;
    this.pauseReason = info.pauseReason;
    this.progress = info.stageProgress;

    this.paused = this.stateValue === 'PAUSED' || this.pauseReason != null;

    this.stateEmitter.emit(this.stateValue);
  }

  isEnqueued(state = this.stateValue): boolean {
    return state === 'ENQUEUED';
  }

  isRunning(state = this.stateValue): boolean {
    return state === 'RUNNING';
  }

  updateRequestPause(pause: boolean, singleStageOnly?: boolean) {
    const before = this.paused;
    this.paused = pause;
    this.dialog.openLoadingIndicator(
      this.api
        .resume(this.project.id, pause, singleStageOnly)
        .then(result => {
          if (!this.paused) {
            this.stateEmitter.emit(this.stateValue = 'RUNNING');
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
      this.pipelinesApi.getRawPipelineDefinition(this.project.pipelineDefinition.id)
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
    console.log('useAsBlueprint ' + (group.stageDefinition instanceof StageWorkerDefinitionInfo));
    if (group.stageDefinition instanceof StageWorkerDefinitionInfo) {
      this.executionSelection.image = group.stageDefinition.image;
      this.executionSelection.resources = new ResourceInfo({
        cpus: group.stageDefinition.requiredResources.cpus,
        gpus: group.stageDefinition.requiredResources.gpu.count,
        megabytesOfRam: group.stageDefinition.requiredResources.megabytesOfRam
      });
      this.executionSelection.selectedStage = group.stageDefinition;
      this.executionSelection.workspaceConfiguration = group.workspaceConfiguration;
      this.executionSelection.comment = group.comment;
      this.environmentVariables = new Map();
      this.defaultEnvironmentVariables = entry != null ? entry.env : group.stageDefinition.environment;
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

  /*Project Groups methods*/
  sortGroups() {
    this.project.groups.sort((a, b) => {
      if (a.role < b.role) {
        return 1;
      } else if (a.role === b.role) {
        if (a.name.toUpperCase() > b.name.toUpperCase()) {
          return 1;
        } else {
          return -1;
        }
      }
    });
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
            this.rawPipelineDefinitionError = '' + result;
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
      this.pipelinesApi.setRawPipelineDefinition(this.project.pipelineDefinition.id, raw)
        .catch(e => {
          editor.parseError = [e];
          return Promise.reject('Failed to parse input, see marked area(s) for more details');
        })
        .then(r => {
          editor.parseError = [];
          return this.pipelinesApi
            .getPipelineDefinition(this.project.pipelineDefinition.id)
            .then(definition => {
              this.setProjectPipeline(definition);
            });
        }),
      `Saving Pipeline Definition`,
      true
    );
  }

  updatePipelineDefinitionWithObject(pipeline: PipelineDefinitionInfo) {
    this.dialog.openLoadingIndicator(
      this.pipelinesApi.setPipelineDefinition(pipeline)
        .then((result) => {
            this.setProjectPipeline(result);
          }
        ),
      'Updating Pipeline with new definition'
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
          .then((result: ProjectInfo[] | null) => {
            if (result) {
              const promises = [];
              for (const project of result) {
                promises.push(this.pipelinesApi.setRawPipelineDefinition(
                  project.pipelineDefinition.id,
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
