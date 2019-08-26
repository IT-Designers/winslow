import { Injectable } from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from '../environments/environment';
import {map} from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class ApiService {


  constructor(private client: HttpClient) {

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

  listResources(path: string) {
    return this.client
      .get<FileInfo[]>(environment.apiLocation + 'files/' + path);
  }

  createDirectory(path: string) {
    if (path.startsWith('/')) {
      path = path.substr(1);
    }
    this
      .client
      .put(environment.apiLocation + 'files/' + path, null)
      .toPromise()
      .then(console.log);
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

export class FileInfo {
  name: string;
  directory: boolean;
  path: string;
}
