import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../environments/environment';
import {RxStompService} from '@stomp/ng2-stompjs';
import {SubscriptionHandler} from './subscription-handler';
import {Subscription} from 'rxjs';
import {Message} from '@stomp/stompjs';
import {ChangeEvent} from './api.service';
import {
  AuthTokenInfo,
  EnvVariable,
  ExecutionGroupInfo,
  ImageInfo, Link,
  LogEntryInfo,
  PipelineDefinitionInfo,
  ProjectInfo,
  RangedList,
  RangedValue,
  RangeWithStepSize,
  ResourceInfo,
  ResourceLimitation,
  StageAndGatewayDefinitionInfo,
  StageDefinitionInfoUnion,
  StageInfo,
  StageWorkerDefinitionInfo,
  StageXOrGatewayDefinitionInfo,
  State,
  StateInfo,
  StatsInfo,
  WorkspaceConfiguration,
  WorkspaceMode
} from './winslow-api';
import {loadPipelineDefinition} from './pipeline-api.service';


@Injectable({
  providedIn: 'root'
})
export class ProjectApiService {

  static LOGS_LATEST = 'latest';

  public cachedTags: string[] = [];
  private readonly projectSubscriptionHandler: SubscriptionHandler<string, ProjectInfo>;
  private readonly publicProjectSubscriptionHandler: SubscriptionHandler<string, ProjectInfo>;
  private readonly ownProjectSubscriptionHandler: SubscriptionHandler<string, ProjectInfo>;
  private readonly projectStateSubscriptionHandler: SubscriptionHandler<string, StateInfo>;
  private readonly publicProjectStateSubscriptionHandler: SubscriptionHandler<string, StateInfo>;
  private readonly ownProjectStateSubscriptionHandler: SubscriptionHandler<string, StateInfo>;

  private static getUrl(more?: string) {
    return `${environment.apiLocation}projects${more != null ? `/${more}` : ''}`;
  }

  private static fixExecutionGroupInfoArray(groups: ExecutionGroupInfo[]): ExecutionGroupInfo[] {
    return groups.map(origin => loadExecutionGroupInfo(origin));
  }

  static toMap(entry) {
    return entry != null ? new Map(Object.keys(entry).map(key => [key, entry[key]])) : new Map();
  }


  constructor(private client: HttpClient, private rxStompService: RxStompService) {
    this.projectSubscriptionHandler = new SubscriptionHandler<string, ProjectInfo>(
      rxStompService,
      '/projects',
      p => loadProjectInfo(p)
    );

    this.publicProjectSubscriptionHandler = new SubscriptionHandler<string, ProjectInfo>(
      rxStompService,
      '/projects/public',
      p => loadProjectInfo(p)
    );

    this.ownProjectSubscriptionHandler = new SubscriptionHandler<string, ProjectInfo>(
      rxStompService,
      '/projects/own',
      p => loadProjectInfo(p)
    );

    this.projectSubscriptionHandler.subscribe((id, value) => {
      if (value != null) {
        this.cacheTags(value.tags);
      }
    });

    this.publicProjectSubscriptionHandler.subscribe((id, value) => {
      if (value != null) {
        this.cacheTags(value.tags);
      }
    });

    this.ownProjectSubscriptionHandler.subscribe((id, value) => {
      if (value != null) {
        this.cacheTags(value.tags);
      }
    });


    this.projectStateSubscriptionHandler = new SubscriptionHandler<string, StateInfo>(
      rxStompService,
      '/projects/states',
      s => new StateInfo(s)
    );

    this.publicProjectStateSubscriptionHandler = new SubscriptionHandler<string, StateInfo>(
      rxStompService,
      '/projects/public',
      s => new StateInfo(s)
    );

    this.ownProjectStateSubscriptionHandler = new SubscriptionHandler<string, StateInfo>(
      rxStompService,
      '/projects/own',
      s => new StateInfo(s)
    );
  }

  private cacheTags(tags: string[]) {
    this.cachedTags = [...this.cachedTags];
    tags.forEach(tag => {
      if (this.cachedTags.indexOf(tag) < 0) {
        this.cachedTags.push(tag);
      }
    });
    this.cachedTags = this.cachedTags.sort();
  }

  public watchProjectStats(projectId: string, listener: (update: StatsInfo) => void): Subscription {
    return this.rxStompService.watch(`/projects/${projectId}/stats`).subscribe((message: Message) => {
      const events: ChangeEvent<string, StatsInfo>[] = JSON.parse(message.body);
      events.forEach(event => {
        if (event.identifier === projectId && event.value) {
          listener(new StatsInfo(event.value));
        }
      });
    });
  }

  private watchProjectExecutionGroupInfo(
    projectId: string,
    specialization: string,
    listener: (update: ExecutionGroupInfo[]) => void): Subscription {
    return this.rxStompService.watch(`/projects/${projectId}/${specialization}`).subscribe((message: Message) => {
      const events: ChangeEvent<string, ExecutionGroupInfo[]>[] = JSON.parse(message.body);
      events.forEach(event => {
        if (event.identifier === projectId) {
          listener(event.value ? ProjectApiService.fixExecutionGroupInfoArray(event.value) : []);
        }
      });
    });
  }

  public watchProjectHistory(projectId: string, listener: (update: ExecutionGroupInfo[]) => void): Subscription {
    return this.watchProjectExecutionGroupInfo(projectId, 'history', listener);
  }

  public watchProjectExecutions(projectId: string, listener: (update: ExecutionGroupInfo[]) => void): Subscription {
    return this.watchProjectExecutionGroupInfo(projectId, 'executing', listener);
  }

  public watchProjectEnqueued(projectId: string, listener: (update: ExecutionGroupInfo[]) => void): Subscription {
    return this.watchProjectExecutionGroupInfo(projectId, 'enqueued', groups => {
      if (groups != null) {
        for (let i = 0; i < groups.length; ++i) {
          groups[i].enqueueIndex = i;
        }
      }
      listener(groups);
    });
  }

  public watchLogs(
    projectId: string,
    listener: (logs: LogEntryInfo[]) => void,
    stageId: string = ProjectApiService.LOGS_LATEST): Subscription {
    return this.rxStompService.watch(`/projects/${projectId}/logs/${stageId}`).subscribe(message => {
      const events: ChangeEvent<string, LogEntryInfo[]>[] = JSON.parse(message.body);
      events.forEach(event => {
        if (event.value) {
          listener(event.value.map(e => new LogEntryInfo(e)));
        }
      });
    });
  }

  public getProjectSubscriptionHandler(): SubscriptionHandler<string, ProjectInfo> {
    return this.projectSubscriptionHandler;
  }

  public getPublicProjectSubscriptionHandler(): SubscriptionHandler<string, ProjectInfo> {
    return this.publicProjectSubscriptionHandler;
  }

  public getOwnProjectSubscriptionHandler(): SubscriptionHandler<string, ProjectInfo> {
    return this.ownProjectSubscriptionHandler;
  }

  public getProjectStateSubscriptionHandler(): SubscriptionHandler<string, StateInfo> {
    return this.projectStateSubscriptionHandler;
  }

  public getPublicProjectStateSubscriptionHandler(): SubscriptionHandler<string, StateInfo> {
    return this.publicProjectStateSubscriptionHandler;
  }

  public getOwnProjectStateSubscriptionHandler(): SubscriptionHandler<string, StateInfo> {
    return this.ownProjectStateSubscriptionHandler;
  }

  createProject(name: string, pipeline: string, tags?: string[]): Promise<ProjectInfo> {
    return this.client
      .post<ProjectInfo>(
        ProjectApiService.getUrl(null),
        {
          name,
          pipeline: pipeline,
          tags
        })
      .toPromise()
      .then(result => result ?? loadProjectInfo(result));
  }

  listProjects(): Promise<ProjectInfo[]> {
    return Promise.all([...this.projectSubscriptionHandler.getCached()]);
  }


  getProjectPartialHistory(projectId: string, olderThanGroupId: string, count: number): Promise<ExecutionGroupInfo[]> {
    return this.client.get<ExecutionGroupInfo[]>(ProjectApiService.getUrl(`${projectId}/history/reversed/${olderThanGroupId}/${count}`))
      .toPromise()
      .then(ProjectApiService.fixExecutionGroupInfoArray);
  }

  getProjectHistory(projectId: string): Promise<ExecutionGroupInfo[]> {
    return this.client.get<ExecutionGroupInfo[]>(ProjectApiService.getUrl(`${projectId}/history`))
      .toPromise()
      .then(ProjectApiService.fixExecutionGroupInfoArray);
  }

  deleteEnqueued(projectId: string, groupId: string): Promise<boolean> {
    return this.client.delete<boolean>(ProjectApiService.getUrl(`${projectId}/enqueued/${groupId}`)).toPromise();
  }

  action(projectId: string, actionId: string): Promise<void> {
    return this.client.post<void>(
      ProjectApiService.getUrl(`${projectId}/action`),
      actionId
    ).toPromise();
  }

  enqueue(
    projectId: string,
    nextStageId: string,
    env: any,
    rangedEnv?: Map<string, RangeWithStepSize>,
    image: ImageInfo = null,
    requiredResources: ResourceInfo = null,
    workspaceConfiguration: WorkspaceConfiguration = null,
    comment: string = null,
    runSingle: boolean = false,
    resume?: boolean,
  ): Promise<void> {
    return this.client.post<void>(
      ProjectApiService.getUrl(`${projectId}/enqueued`),
      {
        id: nextStageId,
        env,
        rangedEnv,
        image,
        requiredResources,
        workspaceConfiguration,
        comment,
        runSingle,
        resume
      }
    ).toPromise();
  }

  pause(projectId: string): Promise<boolean> {
    return this.resume(projectId, true);
  }

  resume(projectId: string, paused: boolean = false, singleStageOnly = false): Promise<boolean> {
    return this.client.put<boolean>(
      ProjectApiService.getUrl(`${projectId}/paused`),
      {
        paused,
        strategy: singleStageOnly ? 'once' : null
      }
    ).toPromise();
  }

  getLogRawUrl(projectId: string, stageId: string): string {
    return ProjectApiService.getUrl(`${projectId}/raw-logs/${stageId}`);
  }

  getEnvironment(projectId: string, stageIndex: number): Promise<Map<string, EnvVariable>> {
    return this.client
      .get<object>(ProjectApiService.getUrl(`${projectId}/${stageIndex}/environment`))
      .toPromise()
      .then(response => {
        const map = new Map();
        for (const [key, value] of Object.entries(response)) {
          map.set(key, new EnvVariable(value));
        }
        return map;
      });
  }

  setName(projectId: string, name: string): Promise<void> {
    return this
      .client
      .put<void>(ProjectApiService.getUrl(`${projectId}/name`), name)
      .toPromise();
  }

  setTags(projectId: string, tags: string[]): Promise<string[]> {
    return this
      .client
      .put<string[]>(ProjectApiService.getUrl(`${projectId}/tags`), tags)
      .toPromise()
      .then(result => {
        this.cacheTags(tags);
        return result;
      });
  }

  addOrUpdateGroup(projectId: string, groupLink: Link): Promise<void> {
    return this
      .client
      .post<void>(ProjectApiService.getUrl(`${projectId}/groups`), groupLink)
      .toPromise();
  }

  removeGroup(projectId: string, groupname: string): Promise<object> {
    return this
      .client
      .delete(ProjectApiService.getUrl(`${projectId}/groups/${groupname}`))
      .toPromise();
  }

  stopStage(projectId: string, pause: boolean, stageId: string = null): Promise<boolean> {
    return this
      .client
      .put<boolean>(
        ProjectApiService.getUrl(`${projectId}/stop${stageId != null ? '/' + stageId : ''}`),
        pause
      )
      .toPromise();
  }

  killStage(projectId: string, stageId: string = null): Promise<boolean> {
    return this
      .client
      .put<boolean>(ProjectApiService.getUrl(
          `${projectId}/kill${stageId != null ? '/' + stageId : ''}`),
        {}
      )
      .toPromise();
  }

  setPipelineDefinition(projectId: string, pipelineId: string): Promise<PipelineDefinitionInfo> {
    return this
      .client
      .put<PipelineDefinitionInfo>(ProjectApiService.getUrl(`${projectId}/pipeline/${pipelineId}`), {})
      .toPromise();
  }

  delete(projectId: string): Promise<string> {
    return this.client
      .delete<string>(ProjectApiService.getUrl(`${projectId}`))
      .toPromise();
  }

  getDeletionPolicy(projectId: string): Promise<DeletionPolicy> {
    return this.client.get<DeletionPolicy>(ProjectApiService.getUrl(`${projectId}/deletion-policy`)).toPromise();
  }

  resetDeletionPolicy(projectId: string): Promise<any> {
    return this.client.delete<any>(ProjectApiService.getUrl(`${projectId}/deletion-policy`)).toPromise();
  }

  updateDeletionPolicy(projectId: string, policy: DeletionPolicy): Promise<DeletionPolicy> {
    return this.client.put<DeletionPolicy>(ProjectApiService.getUrl(`${projectId}/deletion-policy`), policy).toPromise();
  }

  updatePublicAccess(projectId: string, publicAccess: boolean): Promise<boolean> {
    return this.client.put<boolean>(ProjectApiService.getUrl(`${projectId}/public`), publicAccess).toPromise();
  }

  getDefaultDeletionPolicy(projectId: string): Promise<DeletionPolicy> {
    return this
      .client
      .get<DeletionPolicy>(ProjectApiService.getUrl(`${projectId}/deletion-policy/default`))
      .toPromise();
  }

  pruneHistory(projectId: string): Promise<ExecutionGroupInfo[]> {
    return this
      .client
      .post<ExecutionGroupInfo[]>(ProjectApiService.getUrl(`${projectId}/history/prune`), {})
      .toPromise()
      .then(ProjectApiService.fixExecutionGroupInfoArray);
  }

  getWorkspaceConfigurationMode(projectId: string): Promise<WorkspaceMode> {
    return this
      .client
      .get<WorkspaceMode>(ProjectApiService.getUrl(`${projectId}/workspace-configuration-mode`))
      .toPromise();
  }

  setWorkspaceConfigurationMode(projectId: string, mode: WorkspaceMode): Promise<WorkspaceMode> {
    return this
      .client
      .put<WorkspaceMode>(ProjectApiService.getUrl(`${projectId}/workspace-configuration-mode`), mode)
      .toPromise();
  }


  getResourceLimitation(projectId: string): Promise<ResourceLimitation> {
    return this
      .client
      .get<ResourceLimitation>(ProjectApiService.getUrl(`${projectId}/resource-limitation`))
      .toPromise();
  }

  setResourceLimitation(projectId: string, limit: ResourceLimitation): Promise<ResourceLimitation> {
    return this
      .client
      .put<ResourceLimitation>(ProjectApiService.getUrl(`${projectId}/resource-limitation`), limit)
      .toPromise();
  }

  getAuthTokens(projectId: string): Promise<AuthTokenInfo[]> {
    return this
      .client
      .get<AuthTokenInfo[]>(ProjectApiService.getUrl(`${projectId}/auth-tokens`))
      .toPromise()
      .then(e => e.map(o => new AuthTokenInfo(o)));
  }

  createAuthToken(projectId: string, name: string): Promise<AuthTokenInfo> {
    return this
      .client
      .post<AuthTokenInfo>(ProjectApiService.getUrl(`${projectId}/auth-tokens?name=${name}`), new FormData())
      .toPromise()
      .then(e => new AuthTokenInfo(e));
  }

  deleteAuthToken(projectId: string, id: string): Promise<boolean> {
    return this
      .client
      .delete<boolean>(ProjectApiService.getUrl(`${projectId}/auth-tokens/${id}`))
      .toPromise();
  }


  tryParseGroupNumber(groupId: string, alt: number): number {
    if (groupId != null) {
      const split = groupId.split('_');
      if (split.length >= 2) {
        const parsed = Number(split[1]);
        if (!Number.isNaN(parsed)) {
          return parsed;
        }
      }
    }
    return alt;
  }

  tryParseStageNumber(stageId: string, alt: number): number {
    if (stageId != null) {
      const split = stageId.split('_');
      if (split.length >= 3) {
        const parsed = Number(split[split.length - 1]);
        if (!Number.isNaN(parsed)) {
          return parsed;
        }
      }
    }
    return alt;
  }

  findProjectPipeline(project: ProjectInfo, pipelines: PipelineDefinitionInfo[]) {
    for (const pipeline of pipelines) {
      if (pipeline.name === project.pipelineDefinition.name) {
        return pipeline.id;
      }
    }
    return null;
  }
}

export function loadProjectInfo(origin: ProjectInfo): ProjectInfo {
  return new ProjectInfo({
    ...origin,
    pipelineDefinition: loadPipelineDefinition(origin.pipelineDefinition)
  });
}


export class ProjectGroup {
  name: string;
  projects: ProjectInfo[];
}

export function createRangeWithStepSize(min: number, max: number, stepSize: number): RangeWithStepSize {
  const stp = Math.abs(stepSize);
  const dist = (max - min);
  return new RangeWithStepSize({
    '@type': 'DiscreteSteps',
    min,
    max,
    stepCount: Math.ceil(dist / stp) + 1,
    stepSize
  });
}

export function createRangedList(values: string[]): RangedList {
  return new RangedList({
    '@type': 'List',
    stepCount: values.length,
    values
  });
}

export function loadExecutionGroupInfo(origin: ExecutionGroupInfo): ExecutionGroupInfo {
  return new ExecutionGroupInfo({
    ...origin,
    stages: origin.stages.map(stage => loadStageInfo(stage)),
    stageDefinition: loadStageDefinition(origin.stageDefinition)
  });
}

declare module './winslow-api' {
  interface ExecutionGroupInfo {
    enqueueIndex?: number;

    rangedValuesKeys(): string[];

    hasStagesState(state: State): boolean;

    getMostRecentStage(): StageInfo;

    getMostRecentStartOrFinishTime(): number;

    getMostRelevantState(projectState?: State): State;

    isMostRecentStateRunning(): boolean;

    hasRunningStages(): boolean;

    getGroupSize(): number;
  }
}

ExecutionGroupInfo.prototype.rangedValuesKeys = function(): string[] {
  return Object.keys(this.rangedValues);
};

ExecutionGroupInfo.prototype.hasStagesState = function(state: State): boolean {
  for (const stage of this.stages) {
    if (stage.state === state) {
      return true;
    }
  }
  return false;
};

ExecutionGroupInfo.prototype.getMostRecentStage = function(): StageInfo {
  for (const stage of [...this.stages].reverse()) {
    if (stage.finishTime != null) {
      return stage;
    } else if (stage.startTime != null) {
      return stage;
    }
  }
  return null;
};

ExecutionGroupInfo.prototype.getMostRecentStartOrFinishTime = function(): number {
  const stage = this.getMostRecentStage();
  if (stage == null) {
    return null;
  } else if (stage.startTime != null) {
    return stage.startTime;
  } else if (stage?.finishTime != null) {
    return stage.finishTime;
  } else {
    return null;
  }
};

ExecutionGroupInfo.prototype.getMostRelevantState = function(projectState: State = null): State {
  const states: Array<State> = ['RUNNING', 'PREPARING', 'FAILED'];
  for (const state of states) {
    if (this.hasStagesState(state)) {
      return state;
    }
  }
  if (this.enqueued) {
    return 'ENQUEUED';
  } else if (this.active) {
    const alternative = projectState === 'PAUSED' ? 'PAUSED' : 'PREPARING';
    return this.getMostRecentStage()?.state ?? alternative;
  } else {
    return this.getMostRecentStage()?.state ?? 'SKIPPED';
  }
};

ExecutionGroupInfo.prototype.isMostRecentStateRunning = function(): boolean {
  return this.getMostRelevantState() === 'RUNNING';
};

ExecutionGroupInfo.prototype.hasRunningStages = function(): boolean {
  for (const stage of this.stages) {
    if (stage.state === 'RUNNING') {
      return true;
    }
  }
  return false;
};

ExecutionGroupInfo.prototype.getGroupSize = function(): number {
  const rvKeys = Object.keys(this.rangedValues);

  if (rvKeys.length > 0) {
    let size = 0;
    for (const entry of Object.entries(this.rangedValues)) {
      size += (entry[1] as RangedValue).stepCount;
    }
    return size;
  } else {
    return 1;
  }
};


export function loadStageInfo(stage: StageInfo): StageInfo {
  return new StageInfo(stage);
}

export function loadStageDefinition(stage: StageDefinitionInfoUnion): StageDefinitionInfoUnion {
  const type = stage['@type'];
  if (type === 'Worker') {
    return new StageWorkerDefinitionInfo(stage as StageWorkerDefinitionInfo);
  } else if (type === 'XorGateway') {
    return new StageXOrGatewayDefinitionInfo(stage as StageXOrGatewayDefinitionInfo);
  } else if (type === 'AndGateway') {
    return new StageAndGatewayDefinitionInfo(stage as StageAndGatewayDefinitionInfo);
  } else {
    // TODO
    console.error(`Unexpected StageDefinitionInfo type ${type}`);
    return stage;
  }
}

export class DeletionPolicy {
  keepWorkspaceOfFailedStage: boolean;
  numberOfWorkspacesOfSucceededStagesToKeep?: number;
  alwaysKeepMostRecentWorkspace?: boolean;
}

export function createWorkspaceConfiguration(
  mode: WorkspaceMode = 'INCREMENTAL',
  value: string = null,
  sharedWithinGroup: boolean = false,
  nestedWithinGroup: boolean = true): WorkspaceConfiguration {
  return new WorkspaceConfiguration({
    mode, value, sharedWithinGroup, nestedWithinGroup
  });
}


export function loadResourceLimitation(origin: ResourceLimitation): ResourceLimitation {
  return new ResourceLimitation({
    ...origin
  });
}

export function createResourceLimitation(cpu: number = null, mem: number = null, gpu: number = null): ResourceLimitation {
  return new ResourceLimitation({cpu, mem, gpu});
}

export function similarResourceLimitation(a?: ResourceLimitation, b?: ResourceLimitation) {
  if (a != null && b != null) {
    return a.cpu === b.cpu && a.mem === b.mem && a.gpu === b.gpu;
  } else {
    return a == null && b == null;
  }
}
