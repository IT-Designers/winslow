import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../environments/environment';
import {map} from 'rxjs/operators';
import {ProjectApiService} from './project-api.service';
import {FilesApiService} from './files-api.service';

@Injectable({
  providedIn: 'root'
})
export class ApiService {

  constructor(private client: HttpClient, private projectApi: ProjectApiService, private filesApi: FilesApiService) {
  }

  getProjectApi(): ProjectApiService {
    return this.projectApi;
  }

  getFilesApi(): FilesApiService {
    return this.filesApi;
  }

  getPipelineDefinitions() {
    return this.client.get<PipelineDefinition[]>(environment.apiLocation + 'pipelines');
  }

  getStageDefinitions(pipeline: PipelineDefinition) {
    return this.client
      .get<StageInfo[]>(environment.apiLocation + 'stages/' + pipeline.id)
      .pipe(map(p => {
        const names: string[] = [];
        p.forEach(s => names.push(s.name));
        return names;
      }));
  }

}

export class PipelineDefinition {
  id: string;
  name: string;
  desc: string;
  stageDefinitions: StageInfo[];
}

export class StageInfo {
  name: string;
}

