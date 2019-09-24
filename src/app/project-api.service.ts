import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from '../environments/environment';
import {PipelineDefinition} from './api.service';
import {map} from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class ProjectApiService {

  constructor(private client: HttpClient) {
  }

  createProject(name: string, pipeline: PipelineDefinition) {
    const form = new FormData();
    form.append('name', name);
    form.append('pipeline', pipeline.id);
    return this.client.post<any>(environment.apiLocation + 'projects', form);
  }

  listProjects() {
    return this.client.get<Project[]>(environment.apiLocation + 'projects');
  }

  getProjectState(projectId: string) {
    return this.client.get<State>(environment.apiLocation + `projects/${projectId}/state`);
  }

  getProjectStates(projectIds: string[]) {
    return this.client.get<StateInfo[]>(`${environment.apiLocation}/projects/states?projectIds=${projectIds.join(',')}`);
  }

  getProjectHistory(projectId: string) {
    return this.client.get<HistoryEntry[]>(environment.apiLocation + 'projects/' + projectId + '/history');
  }

  getProjectPaused(projectId: string) {
    return this.client.get<boolean>(`${environment.apiLocation}/projects/${projectId}/paused`);
  }

  resume(projectId: string, nextStageIndex: number, singleStageOnly = false, env: any) {
    const form = new FormData();
    form.set('env', JSON.stringify(env));
    return this.client.post(
      `${environment.apiLocation}/projects/${projectId}/resume/${nextStageIndex}${singleStageOnly ? '?strategy=once' : ''}`,
      form
    );
  }

  setProjectPaused(projectId: string, paused: boolean) {
    return this.client.post(`${environment.apiLocation}/projects/${projectId}/paused/${paused}`, new FormData());
  }

  getLog(projectId: string, stageId: string) {
    return this.client.get<LogEntry[]>(`${environment.apiLocation}/projects/${projectId}/logs/${stageId}`);
  }

  getPauseReason(projectId: string) {
    return this.client.get<string>(`${environment.apiLocation}/projects/${projectId}/pause-reason`);
  }

  getEnvironment(projectId: string, stageIndex: number) {
    return this.client
      .get<object>(`${environment.apiLocation}/projects/${projectId}/${stageIndex}/environment`)
      .pipe(map(response => new Map(Object.entries(response))));
  }

  getRequiredUserInput(projectId: string, stageIndex: number) {
    return this.client.get<string[]>(`${environment.apiLocation}/projects/${projectId}/${stageIndex}/required-user-input`);
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


export class LogEntry {
  time: number;
  error: boolean;
  message: string;
}

export class StateInfo {
  state: State;
  pauseReason?: string;
  stageProgress?: number;
}
