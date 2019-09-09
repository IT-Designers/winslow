import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from '../environments/environment';
import {PipelineInfo} from './api.service';
import {map} from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class ProjectApiService {

  constructor(private client: HttpClient) {
  }

  createProject(name: string, pipeline: PipelineInfo) {
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

}
export enum State {
  Preparing,
  Running,
  Succeeded,
  Failed
}

export class Project {
  id: string;
  pipeline: any;
  owner: string;
  groups: string[];
  name: string;
  nextStage: number;
  forceProgressOnce: boolean;
  // loaded lazy
  history?: HistoryEntry[];
  state?: State;
}

export class HistoryEntry {
  time: number;
  state: State;
  stageIndex: number;
  description: string;
}

