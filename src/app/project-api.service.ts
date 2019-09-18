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

  getProjectHistory(projectId: string) {
    return this.client.get<HistoryEntry[]>(environment.apiLocation + 'projects/' + projectId + '/history');
  }

  getProjectPaused(projectId: string) {
    return this.client.get<boolean>(`${environment.apiLocation}/projects/${projectId}/paused`);
  }

  setProjectNextStage(projectId: string, nextStageIndex: number, singleStageOnly = false) {
    return this.client.post(`${environment.apiLocation}/projects/${projectId}/nextStage/${nextStageIndex}${singleStageOnly ? '?strategy=once' : ''}`, new FormData());
  }

  setProjectPaused(projectId: string, paused: boolean) {
    return this.client.post(`${environment.apiLocation}/projects/${projectId}/paused/${paused}`, new FormData());
  }

  getLog(projectId: string, stageId: string) {
    return this.client.get<LogEntry[]>(`${environment.apiLocation}/projects/${projectId}/logs/${stageId}`);
  }
}

export enum State {
  Running = 'Running',
  Paused = 'Paused',
  Succeeded = 'Succeeded',
  Failed = 'Failed'
}

export class Project {
  id: string;
  owner: string;
  groups: string[];
  name: string;
  pipelineDefinition: PipelineDefinition;
  // local only
  history?: HistoryEntry[];
  state?: State;
  paused?: boolean;
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
