import {
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
import {ProjectApiService,} from '../api/project-api.service';
import {MatTabGroup} from '@angular/material/tabs';
import {LongLoadingDetector} from '../long-loading-detector';
import {PipelineApiService} from '../api/pipeline-api.service';
import {StageExecutionSelectionComponent} from '../stage-execution-selection/stage-execution-selection.component';
import {DialogService} from '../dialog.service';
import {PipelineEditorComponent} from '../pipeline-editor/pipeline-editor.component';
import {ActivatedRoute, Router} from '@angular/router';
import {Subscription} from 'rxjs';
import {environment} from '../../environments/environment';
import {
  EnvVariable,
  ExecutionGroupInfo, ParseError,
  PipelineDefinitionInfo,
  ProjectInfo,
  RangedValue,
  ResourceInfo,
  StageInfo,
  StageWorkerDefinitionInfo,
  State,
  StateInfo,
} from '../api/winslow-api';

@Component({
  selector: 'app-project-view',
  templateUrl: './project-view.component.html',
  styleUrls: ['./project-view.component.css']
})
export class ProjectViewComponent implements OnInit, OnDestroy, OnChanges {

  constructor(
    public api: ProjectApiService,
    private pipelinesApi: PipelineApiService,
    private dialog: DialogService,
    private route: ActivatedRoute,
    private router: Router,
  ) {
    this.setHistoryListHeight(window.innerHeight);
  }

  @Input()
  public set project(projectInfo: ProjectInfo) {
    const changed = this.projectValue?.id !== projectInfo?.id;
    this.projectValue = projectInfo;

    if (changed) {
      this.rawPipelineDefinition = undefined;
      this.rawPipelineDefinitionError = undefined;
      this.rawPipelineDefinitionSuccess = undefined;

      this.filesAdditionalRoot = `${projectInfo.name};workspaces/${projectInfo.id}`;

      if (this.tabGroup != null) {
        this.updateRouteToMatchTab();
      }

      this.resubscribe(projectInfo.id);
    }
  }

  public get project(): ProjectInfo {
    return this.projectValue;
  }

  @Input()
  public set state(state: StateInfo | undefined) {
    if (!state) {
      return;
    }

    this.stateValue = state.state;
    this.pauseReason = state.pauseReason;
    this.progress = state.stageProgress;

    this.paused = this.stateValue === 'PAUSED' || this.pauseReason != null;

    this.stateEmitter.emit(this.stateValue);
  }

  overviewTabIndex = ProjectViewTab.Overview;
  selectedTabIndex = ProjectViewTab.Overview;

  @ViewChild('tabGroup') tabGroup!: MatTabGroup;
  @ViewChild('executionSelection') executionSelection!: StageExecutionSelectionComponent;

  projectValue!: ProjectInfo;

  @Output('state') stateEmitter = new EventEmitter<State>();
  @Output('deleted') projectDeletedEmitter = new EventEmitter<string>();

  filesAdditionalRoot!: string;
  filesNavigationTarget!: string;

  stageIdToDisplayLogsFor?: string;
  stateValue?: State;

  history: ExecutionGroupInfo[] = [];
  subscribedProjectId?: string;
  historySubscription?: Subscription;
  historyEnqueued = 0;
  historyEnqueuedSubscription?: Subscription;
  historyExecuting = 0;
  historyExecutingSubscription?: Subscription;
  historyCanLoadMoreEntries = true;

  paused?: boolean;
  pauseReason?: string;
  progress?: number;

  longLoading = new LongLoadingDetector();

  environmentVariables?: Map<string, EnvVariable>;
  defaultEnvironmentVariables?: Record<string, string>;
  rangedEnvironmentVariables?: Record<string, RangedValue>;

  rawPipelineDefinition?: string;
  rawPipelineDefinitionError?: string;
  rawPipelineDefinitionSuccess?: string;

  paramsSubscription?: Subscription;

  historyListHeight: any;
  selectedHistoryEntry?: ExecutionGroupInfo;
  selectedHistoryEntryNumber?: number;
  selectedHistoryEntryIndex = 0;
  selectedHistoryEntryStage?: StageInfo;

  // load more entries, when user is scrolling to the bottom
  // on project history list
  @HostListener('scroll', ['$event'])
  onScroll(event: any) {
    if (event.target.offsetHeight + event.target.scrollTop >= event.target.scrollHeight) {
      this.loadMoreHistoryEntries(10);
    }
  }

  @HostListener('window:resize', ['$event'])
  getScreenSize(_event?: Event) {
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

  ngOnInit(): void {

    this.paramsSubscription = this.route.children[0].params.subscribe(params => {
      if (!params.tab) {
        return;
      }

      const tabIndex = this.indexFromTabName(params.tab);
      if (tabIndex == null) {
        console.warn(`Attempted to switch to ProjectViewTab '${params.tab}', however no such tab is defined.`);
        return;
      }

      this.selectedTabIndex = tabIndex;

      if (ProjectViewTab.PipelineDefinition === tabIndex) {
        this.dialog.openLoadingIndicator(
          this.pipelinesApi.getRawPipelineDefinition(this.project.pipelineDefinition.id)
            .then(result => this.rawPipelineDefinition = result),
          `Loading Pipeline Definition`,
          false
        );
      }

    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (let propName in changes) {
      let change = changes[propName];

      // reset selectedHistory if another project will be selected
      if (change?.currentValue?.id != change?.previousValue?.id) {
        this.selectedHistoryEntry = undefined;
        this.selectedHistoryEntryNumber = undefined;
        this.selectedHistoryEntryIndex = 0;
        this.selectedHistoryEntryStage = undefined;
      }
    }
    this.sortGroups();
  }

  ngOnDestroy(): void {
    this.paramsSubscription?.unsubscribe();
    this.unsubscribe();
  }

  updateRouteToMatchTab() {
    this.router.navigate([ProjectViewTab[this.selectedTabIndex].toLowerCase()], {
      relativeTo: this.route,
    });
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
    this.subscribedProjectId = undefined;
    this.unsubscribeHistory();
  }

  private subscribe(projectId: string) {
    this.subscribedProjectId = projectId;
    this.subscribeHistory(projectId);
  }

  private unsubscribeHistory() {
    if (this.historySubscription != undefined) {
      this.historySubscription.unsubscribe();
      this.historySubscription = undefined;
    }

    if (this.historyExecutingSubscription != undefined) {
      this.historyExecutingSubscription.unsubscribe();
      this.historyExecutingSubscription = undefined;
    }

    if (this.historyEnqueuedSubscription != undefined) {
      this.historyEnqueuedSubscription.unsubscribe();
      this.historyEnqueuedSubscription = undefined;
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
      this.history.splice(offset, 0, ...executions.reverse());
    });
  }

  updateRequestPause(pause: boolean, singleStageOnly?: boolean) {
    const before = this.paused;
    this.paused = pause;
    this.dialog.openLoadingIndicator(
      this.api
        .resume(this.project.id, pause, singleStageOnly)
        .then(_result => {
          if (!this.paused) {
            this.stateEmitter.emit(this.stateValue = 'RUNNING');
            this.pauseReason = undefined;
          }
        })
        .catch(err => {
          this.paused = before;
          return Promise.reject(err);
        })
    );
  }

  isLongLoading() {
    return this.longLoading.isLongLoading();
  }

  openWorkspace(stage: StageInfo) {
    this.tabGroup.selectedIndex = ProjectViewTab.Files;
    this.filesNavigationTarget = `/workspaces/${stage.workspace}/`;
  }

  openTensorboard(project: ProjectInfo, entry: StageInfo) {
    window.open(`${environment.apiLocation}tensorboard/${project.id}/${entry.id}/start`, '_blank');
  }

  openLogs(entry?: StageInfo,) {
    this.stageIdToDisplayLogsFor = entry?.id;
    this.tabGroup.selectedIndex = ProjectViewTab.Logs;
  }

  openAnalysis(entry?: StageInfo) {
    this.stageIdToDisplayLogsFor = entry?.id;
    this.tabGroup.selectedIndex = ProjectViewTab.Analysis;
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
      () => this.api.killStage(this.project.id).then()
    );
  }

  useAsBlueprint(group?: ExecutionGroupInfo, entry?: StageInfo) {
    if (group == undefined) {
      this.dialog.error("Cannot use execution group as blueprint: No group selected.");
      return;
    }

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
      this.executionSelection.comment = group.comment ?? '';
      this.environmentVariables = new Map();
      this.defaultEnvironmentVariables = entry != null ? entry.env : group.stageDefinition.environment;
      this.rangedEnvironmentVariables = entry == null && group.rangedValues != null ? group.rangedValues : {};
      this.rangedEnvironmentVariables = this.rangedEnvironmentVariables ?? {};
      this.tabGroup.selectedIndex = ProjectViewTab.Control;
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
      return -1;
    });
  }

  private setProjectPipeline(pipeline: PipelineDefinitionInfo) {
    const projectInfo = this.project;
    projectInfo.pipelineDefinition = pipeline;
    this.project = new ProjectInfo(projectInfo); // rerender tabs with new pipeline //todo make this an observable
  }

  checkPipelineDefinition(raw: string) {
    this.dialog.openLoadingIndicator(
      this.pipelinesApi.checkPipelineDefinition(raw)
        .then(result => {
          if (result instanceof ParseError) {
            this.rawPipelineDefinitionSuccess = undefined;
            this.rawPipelineDefinitionError = '' + result.message; //TODO Datatype same as in pipeline-details
          } else {
            this.rawPipelineDefinitionSuccess = 'Looks good!';
            this.rawPipelineDefinitionError = undefined;
          }
        }),
      `Checking Pipeline Definition`,
      false
    );
  }

  updatePipelineDefinition(raw: string, editor: PipelineEditorComponent) {
    this.dialog.openLoadingIndicator(
      this.pipelinesApi
        .setRawPipelineDefinition(this.project.pipelineDefinition.id, raw)
        .catch(e => {
          editor.parseError = [e];
          return Promise.reject('Failed to parse input, see marked area(s) for more details');
        })
        .then(() => {
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

  trackHistory(_index: number, value: ExecutionGroupInfo): string {
    return value.id;
  }

  private indexFromTabName(name: string): number | null {
    const lower = name.toLowerCase();
    for (const [key, value] of Object.entries(ProjectViewTab)) {
      if (key.toLowerCase() == lower && typeof value == "number") {
        return value;
      }
    }
    return null;
  }
}

enum ProjectViewTab {
  Overview,
  Control,
  PipelineControl,
  History,
  Files,
  Logs,
  Analysis,
  PipelineDefinition,
  PipelineView,
  Settings
}
