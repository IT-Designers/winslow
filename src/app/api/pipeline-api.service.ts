import {Injectable} from '@angular/core';
import {environment} from '../../environments/environment';
import {HttpClient} from '@angular/common/http';
import {ParseError, StageDefinitionInfo} from './project-api.service';

@Injectable({
  providedIn: 'root'
})
export class PipelineApiService {

  constructor(private client: HttpClient) { }

  private static getUrl(more?: string) {
    return `${environment.apiLocation}pipelines${more != null ? `/${more}` : ''}`;
  }

  getRaw(pipeline: string) {
    return this.client.get<string>(PipelineApiService.getUrl(`${pipeline}/raw`)).toPromise();
  }

  checkPipelineDefinition(raw: string) {
    return this.client.post<string>(PipelineApiService.getUrl(`check`), raw).toPromise();
  }

  updatePipelineDefinition(pipeline: string, raw: string) {
    return this.client.put<string|ParseError>(PipelineApiService.getUrl(`${pipeline}/raw`), raw).toPromise();
  }

  getPipelineDefinition(pipeline: string) {
    return this.client
      .get<PipelineInfo>(PipelineApiService.getUrl(`${pipeline}`))
      .toPromise()
      .then(info => new PipelineInfo(info));
  }

  getPipelineDefinitions() {
    return this
      .client
      .get<PipelineInfo[]>(PipelineApiService.getUrl())
      .toPromise()
      .then(info => info.map(i => new PipelineInfo(i)));
  }

  createPipelineDefinition(name: string) {
    return this
      .client
      .post<PipelineInfo>(PipelineApiService.getUrl(`create`), name)
      .toPromise()
      .then(info => new PipelineInfo(info));
  }
}


export class PipelineInfo {
  id: string;
  name: string;
  desc?: string;
  requiredEnvVariables: string[];
  stages: StageDefinitionInfo[];
  markers: string[];

  constructor(info: PipelineInfo) {
    this.id = info.id;
    this.name = info.name;
    this.desc = info.desc;
    this.requiredEnvVariables = info.requiredEnvVariables;
    this.stages = info.stages;
    this.markers = info.markers;
  }

  hasActionMarker() {
    for (const marker of this.markers) {
      const lower = marker.toLowerCase();
      if (lower.startsWith('action')) {
        return true;
      }
    }
    return false;
  }

  hasActionMarkerFor(pipelineName: string) {
    const markers = this.markers.map(m => m.toLowerCase());
    return markers.indexOf('action') >= 0 || markers.indexOf('action for ' + pipelineName.toLowerCase()) >= 0;
  }
}

export class ResourceInfo {
  cpus: number;
  megabytesOfRam: number;
  gpus?: number;
}
