import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../environments/environment';
import {map} from 'rxjs/operators';
import {PipelineInfo, ResourceInfo} from './pipeline-api.service';

@Injectable({
  providedIn: 'root'
})
export class ProjectApiService {

  constructor(private client: HttpClient) {
  }

  public cachedTags: string[] = [];

  private static getUrl(more?: string) {
    return `${environment.apiLocation}projects${more != null ? `/${more}` : ''}`;
  }

  private static fixExecutionGroupInfo(groups: ExecutionGroupInfo[]): ExecutionGroupInfo[] {
    return groups.map(origin => {
      origin.stages = origin.stages.map(stage => new StageInfo(stage));
      return new ExecutionGroupInfo(origin);
    });
  }

  static toMap(entry) {
    return entry != null ? new Map(Object.keys(entry).map(key => [key, entry[key]])) : new Map();
  }

  private cacheTags(tags: string[]) {
    tags.forEach(tag => {
      if (this.cachedTags.indexOf(tag) < 0) {
        this.cachedTags.push(tag);
      }
    });
    this.cachedTags = this.cachedTags.sort();
  }

  createProject(name: string, pipeline: PipelineInfo, tags?: string[]) {
    const form = new FormData();
    form.append('name', name);
    form.append('pipeline', pipeline.id);
    if (tags != null) {
      form.set('tags', JSON.stringify(tags));
    }
    return this.client.post<any>(ProjectApiService.getUrl(null), form).toPromise();
  }

  listProjects() {
    return this.client
      .get<ProjectInfo[]>(ProjectApiService.getUrl(null))
      .pipe(map(projects => {
        projects.forEach(project => this.cacheTags(project.tags));
        return projects;
      }))
      .toPromise();
  }

  getProjectPipelineDefinition(projectId: string) {
    return this.client.get<PipelineInfo>(ProjectApiService.getUrl(`${projectId}/pipeline-definition`)).toPromise();
  }

  getProjectState(projectId: string) {
    return this.client.get<State>(ProjectApiService.getUrl(`${projectId}/state`)).toPromise();
  }

  getProjectStates(projectIds: string[]) {
    return this.client
      .get<StateInfo[]>(ProjectApiService.getUrl(`states?projectIds=${projectIds.join(',')}`))
      .toPromise()
      .then(result => result.map(r => r != null ? new StateInfo(r) : null));
  }

  getProjectHistory(projectId: string): Promise<ExecutionGroupInfo[]> {
    return this.client.get<ExecutionGroupInfo[]>(ProjectApiService.getUrl(`${projectId}/history`))
      .toPromise()
      .then(ProjectApiService.fixExecutionGroupInfo);
  }

  getProjectEnqueued(projectId: string): Promise<ExecutionGroupInfo[]> {
    return this.client.get<ExecutionGroupInfo[]>(ProjectApiService.getUrl(`${projectId}/enqueued`))
      .pipe(map(enqueued => {
        const fixed = ProjectApiService.fixExecutionGroupInfo(enqueued);
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

  getProjectPaused(projectId: string) {
    return this.client.get<boolean>(ProjectApiService.getUrl(`${projectId}/paused`)).toPromise();
  }

  action(projectId: string, actionId: string) {
    return this.client.put(
      ProjectApiService.getUrl(`${projectId}/action/${actionId}`),
      new FormData()
    ).toPromise();
  }

  enqueue(
    projectId: string,
    nextStageIndex: number,
    env: any,
    rangedEnv?: Map<string, RangedWithStepSize>,
    image: ImageInfo = null,
    requiredResources: ResourceInfo = null,
    workspaceConfiguration: WorkspaceConfiguration = null
  ) {
    const form = new FormData();
    form.set('env', JSON.stringify(env));
    if (rangedEnv != null) {
      form.set('rangedEnv', JSON.stringify(rangedEnv));
    }
    form.set('stageIndex', '' + nextStageIndex);
    if (image != null) {
      form.set('image', JSON.stringify(image));
    }
    if (requiredResources != null) {
      form.set('requiredResources', JSON.stringify(requiredResources));
    }
    if (workspaceConfiguration != null) {
      form.set('workspaceConfiguration', JSON.stringify(workspaceConfiguration));
    }
    return this.client.put(
      ProjectApiService.getUrl(`${projectId}/enqueued`),
      form
    ).toPromise();
  }

  configureGroup(projectId: string, stageIndex: number, projectIds: string[], env: any, image: ImageInfo = null, requiredResources: ResourceInfo = null) {
    const form = new FormData();
    form.set('stageIndex', '' + stageIndex);
    form.set('projectIds', JSON.stringify(projectIds));
    form.set('env', JSON.stringify(env));
    if (image != null) {
      form.set('image', JSON.stringify(image));
    }
    if (requiredResources != null) {
      form.set('requiredResources', JSON.stringify(requiredResources));
    }
    return this.client.put<boolean[]>(
      ProjectApiService.getUrl(`${projectId}/enqueued-on-others`),
      form
    ).toPromise();
  }

  pause(projectId: string) {
    return this.resume(projectId, true);
  }

  resume(projectId: string, paused: boolean = false, singleStageOnly = false) {
    return this.client.post(
      ProjectApiService.getUrl(`${projectId}/paused/${paused}${singleStageOnly ? '?strategy=once' : ''}`),
      new FormData()
    ).toPromise();
  }

  getLog(projectId: string, stageId: string) {
    return this.client.get<LogEntry[]>(ProjectApiService.getUrl(`${projectId}/logs/${stageId}`)).toPromise();
  }

  getLatestLogs(projectId: string, skipLines: number, expectingStageId: string, stageId?: string) {
    return this.client.get<LogEntry[]>(
      ProjectApiService.getUrl(`${projectId}/logs/${stageId ?? 'latest'}?skipLines=${skipLines}&expectingStageId=${expectingStageId}`)
    ).toPromise();
  }

  getLogRawUrl(projectId: string, stageId: string) {
    return ProjectApiService.getUrl(`${projectId}/raw-logs/${stageId}`);
  }

  getProjectRawPipelineDefinition(projectId: string) {
    return this.client.get<string>(ProjectApiService.getUrl(`${projectId}/pipeline-definition-raw`)).toPromise();
  }

  setProjectRawPipelineDefinition(projectId: string, raw: string) {
    const form = new FormData();
    form.set('raw', raw);
    return this.client.post<ParseError>(ProjectApiService.getUrl(`${projectId}/pipeline-definition-raw`), form)
      .toPromise()
      .then(r => {
        if (r != null && Object.keys(r).length !== 0) {
          return Promise.reject(r);
        } else {
          return Promise.resolve(null);
        }
      });
  }

  getPauseReason(projectId: string) {
    return this.client.get<string>(ProjectApiService.getUrl(`${projectId}/pause-reason`)).toPromise();
  }

  getEnvironment(projectId: string, stageIndex: number): Promise<Map<string, EnvVariable>> {
    return this.client
      .get<object>(ProjectApiService.getUrl(`${projectId}/${stageIndex}/environment`))
      .pipe(map(response => new Map(Object.entries(response))))
      .toPromise();
  }

  setName(projectId: string, name: string): Promise<void> {
    const form = new FormData();
    form.set('name', name);
    return this.client.post<void>(ProjectApiService.getUrl(`${projectId}/name`), form).toPromise();
  }

  setTags(projectId: string, tags: string[]) {
    const form = new FormData();
    form.set('tags', JSON.stringify(tags));
    return this.client.post<void>(ProjectApiService.getUrl(`${projectId}/tags`), form)
      .toPromise()
      .then(result => {
        this.cacheTags(tags);
        return result;
      });
  }

  stopStage(projectId: string, pause: boolean, stageId: string = null): Promise<boolean> {
    return this.client
      .put<boolean>(ProjectApiService.getUrl(
        `${projectId}/stop${stageId != null ? '/' + stageId : ''}?pause=${pause}`),
        new FormData()
      )
      .toPromise();
  }

  killStage(projectId: string, stageId: string = null): Promise<boolean> {
    return this.client
      .put<boolean>(ProjectApiService.getUrl(
        `${projectId}/kill${stageId != null ? '/' + stageId : ''}`),
        new FormData()
      )
      .toPromise();
  }

  setPipelineDefinition(projectId: string, pipelineId: string) {
    return this.client.post<boolean>(ProjectApiService.getUrl(`${projectId}/pipeline/${pipelineId}`), new FormData()).toPromise();
  }

  delete(projectId: string) {
    return this.client.delete(ProjectApiService.getUrl(`${projectId}`)).toPromise();
  }

  getDeletionPolicy(projectId: string): Promise<DeletionPolicy> {
    return this.client.get<DeletionPolicy>(ProjectApiService.getUrl(`${projectId}/deletion-policy`)).toPromise();
  }

  resetDeletionPolicy(projectId: string): Promise<any> {
    return this.client.delete<any>(ProjectApiService.getUrl(`${projectId}/deletion-policy`)).toPromise();
  }

  updateDeletionPolicy(projectId: string, policy: DeletionPolicy): Promise<DeletionPolicy> {
    const form = new FormData();
    form.set('value', JSON.stringify(policy));
    return this.client.post<DeletionPolicy>(ProjectApiService.getUrl(`${projectId}/deletion-policy`), form).toPromise();
  }

  updatePublicAccess(projectId: string, publicAccess: boolean): Promise<boolean> {
    return this.client.post<boolean>(ProjectApiService.getUrl(`${projectId}/public`), publicAccess).toPromise();
  }

  getDefaultDeletionPolicy(projectId: string): Promise<DeletionPolicy> {
    return this.client.get<DeletionPolicy>(ProjectApiService.getUrl(`${projectId}/deletion-policy/default`)).toPromise();
  }

  getStats(projectId: string): Promise<StatsInfo> {
    return this.client.get<StatsInfo>(ProjectApiService.getUrl(`${projectId}/stats`)).toPromise();
  }

  pruneHistory(projectId: string): Promise<ExecutionGroupInfo[]> {
    return this.client.post<ExecutionGroupInfo[]>(ProjectApiService.getUrl(`${projectId}/history/prune`), new FormData())
      .toPromise()
      .then(ProjectApiService.fixExecutionGroupInfo);
  }

  getWorkspaceConfigurationMode(projectId: string): Promise<WorkspaceMode> {
    return this.client.get<WorkspaceMode>(ProjectApiService.getUrl(`${projectId}/workspace-configuration-mode`)).toPromise();
  }

  setWorkspaceConfigurationMode(projectId: string, mode: WorkspaceMode): Promise<WorkspaceMode> {
    const form = new FormData();
    form.set('value', JSON.stringify(mode));
    return this.client.post<WorkspaceMode>(ProjectApiService.getUrl(`${projectId}/workspace-configuration-mode`), form).toPromise();
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
}

export enum State {
  Running = 'Running',
  Paused = 'Paused',
  Succeeded = 'Succeeded',
  Failed = 'Failed',
  Preparing = 'Preparing',
  // local only
  Warning = 'Warning',
  Enqueued = 'Enqueued'
}

export enum Action {
  Execute = 'Execute',
  Configure = 'Configure',
}

export class ProjectInfo {
  id: string;
  owner: string;
  groups: string[];
  tags: string[];
  name: string;
  publicAccess: boolean;
  pipelineDefinition: PipelineInfo;
  // local only
  version: number;
  environment: Map<string, string>;
  userInput: string[];
}

export class RangedWithStepSize {
  min: number;
  max: number;
  stepSize: number;
}

export class ExecutionGroupInfo {
  id: string;
  configureOnly: boolean;
  stageDefinition: StageDefinitionInfo;
  rangedValues: Map<string, RangedWithStepSize>;
  workspaceConfiguration: WorkspaceConfiguration;
  stages: StageInfo[];
  active: boolean;
  // local only
  enqueueIndex?: number;

  constructor(origin: ExecutionGroupInfo = null) {
    if (origin != null) {
      Object.assign(this, origin);
      this.stageDefinition = new StageDefinitionInfo(origin.stageDefinition);
      this.rangedValues = ProjectApiService.toMap(origin.rangedValues);
    }
  }

  public getMostRecentStage(): StageInfo {
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
    if (stage?.startTime != null) {
      return stage.startTime;
    } else if (stage?.finishTime != null) {
      return stage.finishTime;
    } else {
      return null;
    }
  }

  public getMostRecentState(): State {
    return this.getMostRecentStage()?.state;
  }

  public isMostRecentStateRunning() {
    return this.getMostRecentState() === State.Running;
  }

  public hasRunningStages(): boolean {
    for (const stage of this.stages) {
      if (stage.state === State.Running) {
        return true;
      }
    }
    return false;
  }
}

export class StageInfo {
  id: string;
  startTime?: number;
  finishTime?: number;
  state?: State;
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

export class StateInfo {
  state: State;
  pauseReason?: string;
  mostRecentStage?: string;
  stageProgress?: number;
  hasEnqueuedStages: boolean;

  constructor(json: any) {
    Object.keys(json).forEach(key => this[key] = json[key]);
  }

  hasEnqueuedFlag(): boolean {
    return this.state !== State.Paused && this.state !== State.Running && this.hasEnqueuedStages;
  }

  hasWarningFlag(): boolean {
    return this.state !== State.Failed && this.pauseReason != null;
  }

  isRunning(): boolean {
    return State.Running === this.actualState();
  }

  actualState(): State {
    let state = this.state;

    if (this.hasEnqueuedFlag()) {
      state = State.Enqueued;
    }

    if (this.hasWarningFlag()) {
      state = State.Warning;
    }

    return state;
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
}

export class StatsInfo {
  cpuUsed = 0;
  cpuMaximum = 0;
  memoryAllocated = 0;
  memoryMaximum = 0;
}

export enum WorkspaceMode {
  STANDALONE = 'STANDALONE',
  INCREMENTAL = 'INCREMENTAL',
  CONTINUATION = 'CONTINUATION',
}

export class WorkspaceConfiguration {
  mode: WorkspaceMode;
  value: string;
  sharedWithinGroup: boolean;

  constructor(mode: WorkspaceMode = WorkspaceMode.INCREMENTAL, value: string = null, sharedWithinGroup: boolean = true) {
    this.mode = mode;
    this.value = value;
    this.sharedWithinGroup = sharedWithinGroup != null && sharedWithinGroup;
  }

}
