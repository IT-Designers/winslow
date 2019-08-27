import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from '../environments/environment';
import {map} from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class ApiService {

  constructor(private client: HttpClient) {}

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

  listFiles(path: string) {
    if (path.startsWith('/')) {
      path = path.substr(1);
    }
    return this.client
      .options<FileInfo[]>(environment.apiLocation + 'files/' + path);
  }

  createDirectory(path: string): Promise<any> {
    if (path.startsWith('/')) {
      path = path.substr(1);
    }
    return this
      .client
      .put(environment.apiLocation + 'files/' + path, null)
      .toPromise();
  }

  uploadFile(pathToDirectory: string, file: File) {
    if (!pathToDirectory.endsWith('/')) {
      pathToDirectory += '/';
    }
    if (pathToDirectory.startsWith('/')) {
      pathToDirectory = pathToDirectory.substr(1);
    }

    const form = new FormData();
    form.append('file', file);

    return this
      .client
      .post(
        environment.apiLocation + 'files/' + pathToDirectory + file.name,
        form,
        { reportProgress: true, observe: 'events' }
      );
  }

  downloadFile(pathToFile: string) {
    window.open(environment.apiLocation + 'files/' + pathToFile);
  }

  delete(path: string) {
    if (path.startsWith('/')) {
      path = path.substr(1);
    }
    return this
      .client
      .delete(environment.apiLocation + 'files/' + path);
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
