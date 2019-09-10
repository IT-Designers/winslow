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

  setProjectNextStage(projectId: string, nextStageIndex: number) {
    return this.client.post(`${environment.apiLocation}/projects/${projectId}/nextStage/${nextStageIndex}`, new FormData());
  }

}
export enum State {
  Running,
  Succeeded,
  Failed
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
}

export class HistoryEntry {
  startTime: number;
  finishTime?: number;
  state?: State;
  stageName?: string;
}

