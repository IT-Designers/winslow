import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from '../environments/environment';
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

  listPipelines() {
    return this.client.get<PipelineInfo[]>(environment.apiLocation + 'pipelines');
  }

  listStages(pipeline: PipelineInfo) {
    return this.client
      .get<Pipeline[]>(environment.apiLocation + 'stages/' + pipeline.id)
      .pipe(map(p => {
        const names: string[] = [];
        p.forEach(s => names.push(s.name));
        return names;
      }));
  }

}

export class PipelineInfo {
  id: string;
  name: string;
  desc: string;
}

export class Pipeline {
  name: string;
  desc: string;
  stages: Stage[];
}

export class Stage {
  name: string;
}
