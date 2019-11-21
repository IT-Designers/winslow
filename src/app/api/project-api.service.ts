import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../environments/environment';
import {map} from 'rxjs/operators';
import {PipelineInfo} from './pipeline-api.service';

@Injectable({
  providedIn: 'root'
})
export class ProjectApiService {

  public cachedTags: string[] = [];

  constructor(private client: HttpClient) {
  }

  private static getUrl(more?: string) {
    return `${environment.apiLocation}projects${more != null ? `/${more}` : ''}`;
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

  getProjectHistory(projectId: string) {
    return this.client.get<HistoryEntry[]>(ProjectApiService.getUrl(`${projectId}/history`))
      .toPromise()
      .then(result => {
        return result.map(entry => {
          entry.env = this.toMap(entry.env);
          entry.envPipeline = this.toMap(entry.envPipeline);
          entry.envSystem = this.toMap(entry.envSystem);
          entry.envInternal = this.toMap(entry.envInternal);
          return entry;
        });
      });
  }

  getProjectEnqueued(projectId: string): Promise<HistoryEntry[]> {
    return this.client.get<any[]>(ProjectApiService.getUrl(`${projectId}/enqueued`))
      .pipe(map(enqueued => {
        return enqueued.map(entry => {
          const history = new HistoryEntry();
          history.state = State.Enqueued;
          history.stageName = entry.name;
          history.imageInfo = entry.image;
          history.env = this.toMap(entry);
          history.envInternal = new Map();
          return history;
        });
      }))
      .toPromise();
  }

  private toMap(entry) {
    return entry != null ? new Map(Object.keys(entry).map(key => [key, entry[key]])) : new Map();
  }

  deleteEnqueued(projectId: string, index: number, controlSize) {
    return this.client.delete<void>(ProjectApiService.getUrl(`${projectId}/enqueued/${index}/${controlSize}`)).toPromise();
  }

  getProjectPaused(projectId: string) {
    return this.client.get<boolean>(ProjectApiService.getUrl(`${projectId}/paused`)).toPromise();
  }

  enqueue(projectId: string, nextStageIndex: number, env: any, image: ImageInfo = null) {
    const form = new FormData();
    form.set('env', JSON.stringify(env));
    form.set('stageIndex', '' + nextStageIndex);
    if (image != null) {
      form.set('imageName', image.name);
      form.set('imageArgs', JSON.stringify(image.args));
    }
    return this.client.put(
      ProjectApiService.getUrl(`${projectId}/enqueued`),
      form
    ).toPromise();
  }

  configureGroup(projectIds: string[], pipelineId: string, stageIndex: number, env: any, image: ImageInfo = null) {
    const form = new FormData();
    form.set('projectIds', JSON.stringify(projectIds));
    form.set('pipelineId', pipelineId);
    form.set('stageIndex', '' + stageIndex);
    form.set('env', JSON.stringify(env));
    if (image != null) {
      form.set('image.name', image.name);
      form.set('image.args', JSON.stringify(image.args));
    }
    return this.client.put(
      ProjectApiService.getUrl(`configuration`),
      form
    ).toPromise();
  }

  resume(projectId: string, paused: boolean, singleStageOnly = false) {
    return this.client.post(
      ProjectApiService.getUrl(`${projectId}/paused/${paused}${singleStageOnly ? '?strategy=once' : ''}`),
      new FormData()
    ).toPromise();
  }

  getLog(projectId: string, stageId: string) {
    return this.client.get<LogEntry[]>(ProjectApiService.getUrl(`${projectId}/logs/${stageId}`)).toPromise();
  }

  getLatestLogs(projectId: string, skipLines: number, expectingStageId: string) {
    return this.client.get<LogEntry[]>(
      ProjectApiService.getUrl(`${projectId}/logs/latest?skipLines=${skipLines}&expectingStageId=${expectingStageId}`)
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

  setName(projectId: string, name: string) {
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

  killStage(projectId: string) {
    return this.client.put<void>(ProjectApiService.getUrl(`${projectId}/kill`), new FormData()).toPromise();
  }

  setPipelineDefinition(projectId: string, pipelineId: string) {
    return this.client.post<boolean>(ProjectApiService.getUrl(`${projectId}/pipeline/${pipelineId}`), new FormData()).toPromise();
  }

  delete(projectId: string) {
    return this.client.delete(ProjectApiService.getUrl(`${projectId}`)).toPromise();
  }
}

export enum State {
  Running = 'Running',
  Paused = 'Paused',
  Succeeded = 'Succeeded',
  Failed = 'Failed',
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
  pipelineDefinition: PipelineInfo;
  // local only
  version: number;
  environment: Map<string, string>;
  userInput: string[];
}

export class HistoryEntry {
  stageId: string;
  startTime: number;
  finishTime?: number;
  state: State;
  action: Action;
  stageName: string;
  workspace?: string;
  imageInfo?: ImageInfo;
  env: Map<string, string>;
  envPipeline: Map<string, string>;
  envSystem: Map<string, string>;
  envInternal: Map<string, string>;
  // local only
  enqueueIndex?: number;
  enqueueControlSize?: number;
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
  runningStage?: string;
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
