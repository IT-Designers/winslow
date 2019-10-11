import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../environments/environment';
import {PipelineDefinition} from './api.service';
import {map} from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class ProjectApiService {

  constructor(private client: HttpClient) {
  }

  private static getUrl(more?: string) {
    return `${environment.apiLocation}projects${more != null ? `/${more}` : ''}`;

  }

  createProject(name: string, pipeline: PipelineDefinition) {
    const form = new FormData();
    form.append('name', name);
    form.append('pipeline', pipeline.id);
    return this.client.post<any>(ProjectApiService.getUrl(null), form);
  }

  listProjects() {
    return this.client.get<Project[]>(ProjectApiService.getUrl(null));
  }

  getProjectState(projectId: string) {
    return this.client.get<State>(ProjectApiService.getUrl(`${projectId}/state`));
  }

  getProjectStates(projectIds: string[]) {
    return this.client.get<StateInfo[]>(ProjectApiService.getUrl(`states?projectIds=${projectIds.join(',')}`));
  }

  getProjectHistory(projectId: string) {
    return this.client.get<HistoryEntry[]>(ProjectApiService.getUrl( `${projectId}/history`));
  }

  getProjectPaused(projectId: string) {
    return this.client.get<boolean>(ProjectApiService.getUrl(`${projectId}/paused`));
  }

  resume(projectId: string, nextStageIndex: number, singleStageOnly = false, env: any, image: ImageInfo = null) {
    const form = new FormData();
    form.set('env', JSON.stringify(env));
    if (image != null) {
      form.set('imageName', image.name);
      form.set('imageArgs', JSON.stringify(image.args));
    }
    return this.client.post(
      ProjectApiService.getUrl(`${projectId}/resume/${nextStageIndex}${singleStageOnly ? '?strategy=once' : ''}`),
      form
    );
  }

  setProjectPaused(projectId: string, paused: boolean) {
    return this.client.post(ProjectApiService.getUrl(`${projectId}/paused/${paused}`), new FormData());
  }

  getLog(projectId: string, stageId: string) {
    return this.client.get<LogEntry[]>(ProjectApiService.getUrl(`${projectId}/logs/${stageId}`));
  }

  getLatestLogs(projectId: string, skipLines: number, expectingStageId: string) {
    return this.client.get<LogEntry[]>(ProjectApiService.getUrl(`${projectId}/logs/latest?skipLines=${skipLines}&expectingStageId=${expectingStageId}`));
  }

  getPauseReason(projectId: string) {
    return this.client.get<string>(ProjectApiService.getUrl(`${projectId}/pause-reason`));
  }

  getEnvironment(projectId: string, stageIndex: number) {
    return this.client
      .get<object>(ProjectApiService.getUrl(`${projectId}/${stageIndex}/environment`))
      .pipe(map(response => new Map(Object.entries(response))));
  }

  getRequiredUserInput(projectId: string, stageIndex: number) {
    return this.client.get<string[]>(ProjectApiService.getUrl(`${projectId}/${stageIndex}/required-user-input`));
  }

  getImage(projectId: string, stageIndex: number) {
    return this.client.get<ImageInfo>(ProjectApiService.getUrl(`${projectId}/${stageIndex}/image`));
  }

    setName(projectId: string, name: string) {
      const form = new FormData();
      form.set('name', name);
      return this.client.post<void>(ProjectApiService.getUrl(`${projectId}/name`), form);
  }
}

export enum State {
  Running = 'Running',
  Paused = 'Paused',
  Succeeded = 'Succeeded',
  Failed = 'Failed',
  // local only
  Warning = 'Warning'
}

export class Project {
  id: string;
  owner: string;
  groups: string[];
  name: string;
  pipelineDefinition: PipelineDefinition;
  environment: Map<string, string>;
  userInput: string[];
  // local only
  version: number;
}

export class HistoryEntry {
  stageId: string;
  startTime: number;
  finishTime?: number;
  state?: State;
  stageName?: string;
  workspace?: string;
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
}

export class ImageInfo {
  name?: string;
  args?: string[];
}
