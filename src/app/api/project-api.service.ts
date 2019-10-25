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
  }

  createProject(name: string, pipeline: PipelineInfo) {
    const form = new FormData();
    form.append('name', name);
    form.append('pipeline', pipeline.id);
    return this.client.post<any>(ProjectApiService.getUrl(null), form).toPromise();
  }

  listProjects() {
    return this.client
      .get<Project[]>(ProjectApiService.getUrl(null))
      .pipe(map(projects => {
        projects.forEach(project => this.cacheTags(project.tags));
        return projects;
      }))
      .toPromise();
  }

  getProjectState(projectId: string) {
    return this.client.get<State>(ProjectApiService.getUrl(`${projectId}/state`)).toPromise();
  }

  getProjectStates(projectIds: string[]) {
    return this.client.get<StateInfo[]>(ProjectApiService.getUrl(`states?projectIds=${projectIds.join(',')}`)).toPromise();
  }

  getProjectHistory(projectId: string) {
    return this.client.get<HistoryEntry[]>(ProjectApiService.getUrl(`${projectId}/history`))
      .toPromise()
      .then(result => {
        return result.map(entry => {
          entry.env = new Map(Object.keys(entry.env).map(key => [key, entry.env[key]]));
          entry.envInternal = new Map(Object.keys(entry.envInternal).map(key => [key, entry.envInternal[key]]));
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
          history.env = entry.env != null ? entry.env : new Map();
          history.envInternal = new Map();
          return history;
        });
      }))
      .toPromise();
  }

  deleteEnqueued(projectId: string, index: number, controlSize) {
    return this.client.delete<boolean>(ProjectApiService.getUrl(`${projectId}/enqueued/${index}/${controlSize}`)).toPromise();
  }

  getProjectPaused(projectId: string) {
    return this.client.get<boolean>(ProjectApiService.getUrl(`${projectId}/paused`)).toPromise();
  }

  enqueue(projectId: string, nextStageIndex: number, env: any, image: ImageInfo = null) {
    const form = new FormData();
    form.set('env', JSON.stringify(env));
    if (image != null) {
      form.set('imageName', image.name);
      form.set('imageArgs', JSON.stringify(image.args));
    }
    return this.client.post(
      ProjectApiService.getUrl(`${projectId}/enqueue/${nextStageIndex}`),
      form
    ).toPromise();
  }

  resume(projectId: string, paused: boolean, singleStageOnly = false) {
    return this.client.post(ProjectApiService.getUrl(`${projectId}/paused/${paused}${singleStageOnly ? '?strategy=once' : ''}`), new FormData()).toPromise();
  }

  getLog(projectId: string, stageId: string) {
    return this.client.get<LogEntry[]>(ProjectApiService.getUrl(`${projectId}/logs/${stageId}`)).toPromise();
  }

  getLatestLogs(projectId: string, skipLines: number, expectingStageId: string) {
    return this.client.get<LogEntry[]>(ProjectApiService.getUrl(`${projectId}/logs/latest?skipLines=${skipLines}&expectingStageId=${expectingStageId}`)).toPromise();
  }

  getPauseReason(projectId: string) {
    return this.client.get<string>(ProjectApiService.getUrl(`${projectId}/pause-reason`)).toPromise();
  }

  getEnvironment(projectId: string, stageIndex: number) {
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

export class Project {
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
  stageName: string;
  workspace: string;
  imageInfo?: ImageInfo;
  env: Map<string, string>;
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
  time: number;
  source: LogSource;
  error: boolean;
  message: string;
  stageId?: string; // ProjectsController.LogEntryInfo
}

export class StateInfo {
  state: State;
  pauseReason?: string;
  stageProgress?: number;
  hasEnqueuedStages: boolean;
}

export class ImageInfo {
  name?: string;
  args?: string[];
}
