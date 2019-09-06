import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from '../environments/environment';
import {PipelineInfo} from './api.service';

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

  getProjectHistory(projectId: string) {
    return this.client.get<HistoryEntry[]>(environment.apiLocation + 'projects/' + projectId + '/history');
  }

}

export class Project {
  id: string;
  pipeline: any;
  owner: string;
  groups: string[];
  name: string;
  nextStage: number;
  forceProgressOnce: boolean;
  history: HistoryEntry[];
}

export class HistoryEntry {
  time: number;
  state: string;
  stageIndex: number;
  description: string;
}

