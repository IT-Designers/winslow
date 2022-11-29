import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../environments/environment';
import {map} from 'rxjs/operators';
import {RxStompService} from '@stomp/ng2-stompjs';
import {SubscriptionHandler} from './subscription-handler';
import {Subscription} from 'rxjs';
import {Message} from '@stomp/stompjs';
import {ChangeEvent} from './api.service';
import {
  IExecutionGroupInfo,
  IPipelineInfo,
  IProjectInfo, IResourceInfo, IResourceLimitation, IStageDefinitionInfo,
  IStageInfo,
  IState,
  IStateInfo,
  IWorkspaceConfiguration,
  IWorkspaceMode
} from './winslow-api';
import {supportsColor} from '@angular/cli/utilities/color';


@Injectable({
  providedIn: 'root'
})
export class ProjectApiService {

  static LOGS_LATEST = 'latest';

  public cachedTags: string[] = [];
  private projectSubscriptionHandler: SubscriptionHandler<string, IProjectInfoExt>;
  private projectStateSubscriptionHandler: SubscriptionHandler<string, StateInfo>;

  private static getUrl(more?: string) {
    return `${environment.apiLocation}projects${more != null ? `/${more}` : ''}`;
  }

  private static fixExecutionGroupInfo(origin: IExecutionGroupInfoExt): IExecutionGroupInfoExt {
    origin.stages = origin.stages.map(stage => new IStageInfo(stage));
    return new IExecutionGroupInfoExt(origin);
  }

  private static fixExecutionGroupInfoArray(groups: IExecutionGroupInfoExt[]): IExecutionGroupInfoExt[] {
    return groups.map(origin => {
      return ProjectApiService.fixExecutionGroupInfo(origin);
    });
  }

  static toMap(entry) {
    return entry != null ? new Map(Object.keys(entry).map(key => [key, entry[key]])) : new Map();
  }


  constructor(private client: HttpClient, private rxStompService: RxStompService) {
    this.projectSubscriptionHandler = new SubscriptionHandler<string, IProjectInfoExt>(rxStompService, '/projects');
    this.projectStateSubscriptionHandler = new SubscriptionHandler<string, StateInfo>(
      rxStompService, '/projects/states',
      s => new StateInfo(s));

    this.projectSubscriptionHandler.subscribe((id, value) => {
      if (value != null) {
        this.cacheTags(value.tags);
      }
    });
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
        if (event.identifier === projectId) {
          listener(event.value ? event.value : new StatsInfo());
        }
      });
    });
  }

  private watchProjectExecutionGroupInfo(
    projectId: string,
    specialization: string,
    listener: (update: IExecutionGroupInfoExt[]) => void): Subscription {
    return this.rxStompService.watch(`/projects/${projectId}/${specialization}`).subscribe((message: Message) => {
      const events: ChangeEvent<string, IExecutionGroupInfoExt[]>[] = JSON.parse(message.body);
      events.forEach(event => {
        if (event.identifier === projectId) {
          listener(event.value ? ProjectApiService.fixExecutionGroupInfoArray(event.value) : []);
        }
      });
    });
  }

  public watchProjectHistory(projectId: string, listener: (update: IExecutionGroupInfoExt[]) => void): Subscription {
    return this.watchProjectExecutionGroupInfo(projectId, 'history', listener);
  }

  public watchProjectExecutions(projectId: string, listener: (update: IExecutionGroupInfoExt[]) => void): Subscription {
    return this.watchProjectExecutionGroupInfo(projectId, 'executing', listener);
  }

  public watchProjectEnqueued(projectId: string, listener: (update: IExecutionGroupInfoExt[]) => void): Subscription {
    return this.watchProjectExecutionGroupInfo(projectId, 'enqueued', groups => {
      if (groups != null) {
        for (let i = 0; i < groups.length; ++i) {
          groups[i].enqueueIndex = i;
        }
      }
      listener(groups);
    });
  }

  public watchLogs(projectId: string, listener: (logs: LogEntry[]) => void, stageId: string = ProjectApiService.LOGS_LATEST): Subscription {
    return this.rxStompService.watch(`/projects/${projectId}/logs/${stageId}`).subscribe(message => {
      const events: ChangeEvent<string, LogEntry[]>[] = JSON.parse(message.body);
      events.forEach(event => {
        if (event.value) {
          listener(event.value);
        }
      });
    });
  }

  public getProjectSubscriptionHandler(): SubscriptionHandler<string, IProjectInfoExt> {
    return this.projectSubscriptionHandler;
  }

  public getProjectStateSubscriptionHandler(): SubscriptionHandler<string, StateInfo> {
    return this.projectStateSubscriptionHandler;
  }

  createProject(name: string, pipeline: IPipelineInfo, tags?: string[]): Promise<IProjectInfoExt> {
    return this.client
      .post<IProjectInfoExt>(
        ProjectApiService.getUrl(null),
        {
          name,
          pipeline: pipeline.id,
          tags
        })
      .toPromise();
  }

  listProjects(): Promise<IProjectInfoExt[]> {
    return Promise.all([...this.projectSubscriptionHandler.getCached()]);
  }

  getProjectPipelineDefinition(projectId: string): Promise<IPipelineInfo> {
    return this.client
      .get<IPipelineInfo>(ProjectApiService.getUrl(`${projectId}/pipeline-definition`))
      .toPromise();
  }

  getProjectPartialHistory(projectId: string, olderThanGroupId: string, count: number): Promise<IExecutionGroupInfoExt[]> {
    return this.client.get<IExecutionGroupInfoExt[]>(ProjectApiService.getUrl(`${projectId}/history/reversed/${olderThanGroupId}/${count}`))
      .toPromise()
      .then(ProjectApiService.fixExecutionGroupInfoArray);
  }

  getProjectHistory(projectId: string): Promise<IExecutionGroupInfoExt[]> {
    return this.client.get<IExecutionGroupInfoExt[]>(ProjectApiService.getUrl(`${projectId}/history`))
      .toPromise()
      .then(ProjectApiService.fixExecutionGroupInfoArray);
  }

  getProjectEnqueued(projectId: string): Promise<IExecutionGroupInfoExt[]> {
    return this.client.get<IExecutionGroupInfoExt[]>(ProjectApiService.getUrl(`${projectId}/enqueued`))
      .pipe(map(enqueued => {
        const fixed = ProjectApiService.fixExecutionGroupInfoArray(enqueued);
        for (let i = 0; i < fixed.length; ++i) {
          fixed[i].enqueueIndex = i;
        }
        return fixed;
      }))
      .toPromise();
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
    nextStageIndex: number,
    env: any,
    rangedEnv?: Map<string, RangeWithStepSize>,
    image: ImageInfo = null,
    requiredResources: IResourceInfo = null,
    workspaceConfiguration: IWorkspaceConfigurationExt = null,
    comment: string = null,
    runSingle: boolean = false,
    resume?: boolean,
  ): Promise<void> {
    return this.client.post<void>(
      ProjectApiService.getUrl(`${projectId}/enqueued`),
      {
        env,
        rangedEnv,
        stageIndex: nextStageIndex,
        image,
        requiredResources,
        workspaceConfiguration,
        comment,
        runSingle,
        resume
      }
    ).toPromise();
  }


  configureGroup(
    projectId: string,
    stageIndex: number,
    projectIds: string[],
    env: any,
    image: ImageInfo = null,
    requiredResources: IResourceInfo = null): Promise<boolean[]> {
    return this.client.post<boolean[]>(
      ProjectApiService.getUrl(`${projectId}/enqueued-on-others`),
      {
        stageIndex,
        projectIds,
        env,
        image,
        requiredResources
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

  getProjectRawPipelineDefinition(projectId: string): Promise<string> {
    return this.client.get<string>(ProjectApiService.getUrl(`${projectId}/pipeline-definition-raw`)).toPromise();
  }

  setProjectRawPipelineDefinition(projectId: string, raw: string): Promise<void | ParseError> {
    return this.client.put<ParseError>(ProjectApiService.getUrl(`${projectId}/pipeline-definition-raw`), raw)
      .toPromise()
      .then(r => {
        if (r != null && Object.keys(r).length !== 0) {
          return Promise.reject(r);
        } else {
          return Promise.resolve(null);
        }
      });
  }

  getEnvironment(projectId: string, stageIndex: number): Promise<Map<string, EnvVariable>> {
    return this.client
      .get<object>(ProjectApiService.getUrl(`${projectId}/${stageIndex}/environment`))
      .pipe(map(response => new Map(Object.entries(response))))
      .toPromise();
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

  setPipelineDefinition(projectId: string, pipelineId: string): Promise<boolean> {
    return this
      .client
      .put<boolean>(ProjectApiService.getUrl(`${projectId}/pipeline/${pipelineId}`), {})
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

  pruneHistory(projectId: string): Promise<IExecutionGroupInfoExt[]> {
    return this
      .client
      .post<IExecutionGroupInfoExt[]>(ProjectApiService.getUrl(`${projectId}/history/prune`), {})
      .toPromise()
      .then(ProjectApiService.fixExecutionGroupInfoArray);
  }

  getWorkspaceConfigurationMode(projectId: string): Promise<IWorkspaceMode> {
    return this
      .client
      .get<IWorkspaceMode>(ProjectApiService.getUrl(`${projectId}/workspace-configuration-mode`))
      .toPromise();
  }

  setWorkspaceConfigurationMode(projectId: string, mode: IWorkspaceMode): Promise<IWorkspaceMode> {
    return this
      .client
      .put<IWorkspaceMode>(ProjectApiService.getUrl(`${projectId}/workspace-configuration-mode`), mode)
      .toPromise();
  }


  getResourceLimitation(projectId: string): Promise<IResourceLimitationExt> {
    return this
      .client
      .get<IResourceLimitationExt>(ProjectApiService.getUrl(`${projectId}/resource-limitation`))
      .toPromise();
  }

  setResourceLimitation(projectId: string, limit: IResourceLimitationExt): Promise<IResourceLimitationExt> {
    return this
      .client
      .put<IResourceLimitationExt>(ProjectApiService.getUrl(`${projectId}/resource-limitation`), limit)
      .toPromise();
  }

  getAuthTokens(projectId: string): Promise<AuthTokenInfo[]> {
    return this
      .client
      .get<AuthTokenInfo[]>(ProjectApiService.getUrl(`${projectId}/auth-tokens`))
      .toPromise();
  }

  createAuthToken(projectId: string, name: string): Promise<AuthTokenInfo> {
    return this
      .client
      .post<AuthTokenInfo>(ProjectApiService.getUrl(`${projectId}/auth-tokens?name=${name}`), new FormData())
      .toPromise();
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

  findProjectPipeline(project: IProjectInfoExt, pipelines: IPipelineInfo[]) {
    for (const pipeline of pipelines) {
      if (pipeline.name === project.pipelineDefinition.name) {
        return pipeline.id;
      }
    }
    return null;
  }
}

export class IProjectInfoExt extends IProjectInfo {
  version: number;
  environment: Map<string, string>;
  userInput: string[];
}


export class ProjectGroup {
  name: string;
  projects: IProjectInfoExt[];
}

declare module './winslow-api' {
  interface IRangedValue {
    getStageCount(): number;
  }
}


export class RangedValue {
  DiscreteSteps?: RangeWithStepSize;
  List?: RangedList;

  constructor(origin?: RangedValue) {
    if (origin != null) {
      if (origin.DiscreteSteps != null) {
        this.DiscreteSteps = new RangeWithStepSize(origin.DiscreteSteps);
      } else if (origin.List != null) {
        this.List = new RangedList(origin.List);
      }
    }
  }

  getStageCount(): number {
    if (this.DiscreteSteps != null) {
      return this.DiscreteSteps.getStageCount();
    } else if (this.List != null) {
      return this.List.getStageCount();
    } else {
      return 0;
    }
  }
}

export class RangedList {
  values: string[];

  constructor(origin?: RangedList) {
    if (origin != null) {
      this.values = origin.values ?? [];
    }
  }

  getStageCount(): number {
    return this.values.length;
  }
}

export class RangeWithStepSize {
  min: number;
  max: number;
  stepSize: number;

  constructor(origin?: RangeWithStepSize) {
    if (origin != null) {
      this.min = origin.min;
      this.max = origin.max;
      this.stepSize = origin.stepSize;
    }
  }

  getStageCount(): number {
    const min = Math.min(this.min, this.max);
    const max = Math.max(this.min, this.max);
    const stp = Math.abs(this.stepSize);
    const dist = (max - min);
    return Math.ceil(dist / stp) + 1;
  }
}

export class AuthTokenInfo {
  id: string;
  secret?: string;
  name: string;
  capabilities: string[];
}


export class IExecutionGroupInfoExt extends IExecutionGroupInfo {
  enqueueIndex?: number;


  public rangedValues_keys() {
    return Object.keys(this.rangedValues);
  }

  hasStagesState(state: IState): boolean {
    for (const stage of this.stages) {
      if (stage.state === state) {
        return true;
      }
    }
    return false;
  }

  public getMostRecentStage(): IStageInfo {
    for (const stage of [...this.stages].reverse()) {
      if (stage.finishTime != null) {
        return stage;
      } else if (stage.startTime != null) {
        return stage;
      }
    }
    return null;
  }

  public getMostRecentStartOrFinishTime(): number {
    const stage = this.getMostRecentStage();
    if (stage.startTime != null) {
      return stage.startTime;
    } else if (stage?.finishTime != null) {
      return stage.finishTime;
    } else {
      return null;
    }
  }

  public getMostRelevantState(projectState: IState = null): IState {
    const states: Array<IState> = ['Running', 'Preparing', 'Failed'];
    for (const state of states) {
      if (this.hasStagesState(state)) {
        return state;
      }
    }
    if (this.enqueued) {
      return 'Enqueued';
    } else if (this.active) {
      const alternative = projectState === 'Paused' ? 'Paused' : 'Preparing';
      return this.getMostRecentStage()?.state ?? alternative;
    } else {
      return this.getMostRecentStage()?.state ?? 'Skipped';
    }
  }

  public isMostRecentStateRunning() {
    return this.getMostRelevantState() === 'Running';
  }

  public hasRunningStages(): boolean {
    for (const stage of this.stages) {
      if (stage.state === 'Running') {
        return true;
      }
    }
    return false;
  }

  public getGroupSize(): number {
    const rvKeys = Object.keys(this.rangedValues);

    if (rvKeys.length > 0) {
      let size = 0;
      for (const entry of Object.entries(this.rangedValues)) {

        size += entry[1].getStageCount();
      }
      return size;
    } else {
      return 1;
    }
  }
}


export class ExecutionGroupInfo {
  id: string;
  configureOnly: boolean;
  stageDefinition: IStageDefinitionInfo;
  rangedValues: Map<string, RangedValue>;
  workspaceConfiguration: IWorkspaceConfigurationExt;
  stages: StageInfo[];
  active: boolean;
  enqueued: boolean;
  comment?: string;
  // local only
  enqueueIndex?: number;

  constructor(origin: ExecutionGroupInfo = null) {
    if (origin != null) {
      Object.assign(this, origin);
      this.stageDefinition = new IStageDefinitionInfo(origin.stageDefinition);
      this.rangedValues = new Map();

      for (const [key, value] of ProjectApiService.toMap(origin.rangedValues)) {
        this.rangedValues.set(key, new RangedValue(value));
      }
    }
  }

}

export class IStageInfoExt extends IStageInfo {
  constructor(origin: IStageInfo = null) {
    if (origin != null) {
      super(origin);
    }
  }

}

export class StageInfo {
  id: string;
  startTime?: number;
  finishTime?: number;
  state?: IState;
  workspace?: string;
  env: Map<string, string>;
  envPipeline: Map<string, string>;
  envSystem: Map<string, string>;
  envInternal: Map<string, string>;

  constructor(origin: StageInfo = null) {
    if (origin != null) {
      Object.assign(this, origin);
      this.env = ProjectApiService.toMap(origin.env);
      this.envPipeline = ProjectApiService.toMap(origin.envPipeline);
      this.envSystem = ProjectApiService.toMap(origin.envSystem);
      this.envInternal = ProjectApiService.toMap(origin.envInternal);
    }
  }
}

/*
export class StageDefinitionInfo {
  name: string;
  image?: ImageInfo;
  requiredEnvVariables: string[];
  requiredResources: ResourceInfo;
  env: Map<string, string>;

  constructor(origin: StageDefinitionInfo = null) {
    if (origin != null) {
      Object.assign(this, origin);
      this.env = ProjectApiService.toMap(origin.env);
    }
  }
}
*/

export enum LogSource {
  STANDARD_IO = 'STANDARD_IO',
  MANAGEMENT_EVENT = 'MANAGEMENT_EVENT'
}

export class LogEntry {
  line: number;
  time: number;
  source: LogSource;
  error: boolean;
  message: string;
  stageId?: string; // ProjectsController.LogEntryInfo
}


export class IStateInfoExt extends IStateInfo {
  isRunning(): boolean {
    return this.getState() === 'Running';
  }

  getState(): IState {
    return this.state;
  }
}

export class StateInfo {
  state: IState;
  pauseReason?: string;
  description?: string;
  stageProgress?: number;
  hasEnqueuedStages: boolean;

  constructor(json: any) {
    Object.keys(json).forEach(key => this[key] = json[key]);
  }

  isRunning(): boolean {
    return this.getState() === 'Running';
  }

  getState(): IState {
    return this.state;
  }

}

export class ImageInfo {
  name?: string;
  args?: string[];
  shmMegabytes?: number;
}

export class ParseError {
  line: number;
  column: number;
  message: string;

  static canShadow(obj: any): boolean {
    return obj != null
      && obj.line != null
      && obj.column != null
      && obj.message != null;
  }
}

export class EnvVariable {
  key: string;
  value?: string;
  valueInherited?: null;
}

export class DeletionPolicy {
  keepWorkspaceOfFailedStage: boolean;
  numberOfWorkspacesOfSucceededStagesToKeep?: number;
  alwaysKeepMostRecentWorkspace?: boolean;
}

export class StatsInfo {
  stageId?: string;
  runningOnNode?: string;
  cpuUsed = 0;
  cpuMaximum = 0;
  memoryAllocated = 0;
  memoryMaximum = 0;
}


export class IWorkspaceConfigurationExt extends IWorkspaceConfiguration {


  // @ts-ignore
  static create(mode: IWorkspaceMode = 'INCREMENTAL',
                value: string = null,
                sharedWithinGroup: boolean = false,
                nestedWithinGroup: boolean = true) {
    return new IWorkspaceConfigurationExt({
      mode, value, sharedWithinGroup, nestedWithinGroup
    });
  }

}

export class IResourceLimitationExt extends IResourceLimitation {

  static create(cpu: number = null, mem: number = null, gpu: number = null) {
    return new IResourceLimitationExt({cpu, mem, gpu});
  }

  static equals(a?: IResourceLimitation, b?: IResourceLimitation) {
    if (a != null && b != null) {
      return a.cpu === b.cpu && a.mem === b.mem && a.gpu === b.gpu;
    } else {
      return a == null && b == null;
    }
  }
}

