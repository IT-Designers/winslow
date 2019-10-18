import {Injectable} from '@angular/core';
import {ProjectApiService} from './project-api.service';
import {FilesApiService} from './files-api.service';
import {PipelineApiService} from './pipeline-api.service';

@Injectable({
  providedIn: 'root'
})
export class ApiService {

  constructor(private projectApi: ProjectApiService, private filesApi: FilesApiService, private pipelineApi: PipelineApiService) {
  }

  getProjectApi(): ProjectApiService {
    return this.projectApi;
  }

  getFilesApi(): FilesApiService {
    return this.filesApi;
  }

  getPipelineApi(): PipelineApiService {
    return this.pipelineApi;
  }
}
