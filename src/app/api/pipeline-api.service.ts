import {Injectable} from '@angular/core';
import {environment} from '../../environments/environment';
import {map} from 'rxjs/operators';
import {HttpClient} from '@angular/common/http';
import {ImageInfo} from './project-api.service';

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
    const form = new FormData();
    form.set('raw', raw);
    return this.client.post<string>(PipelineApiService.getUrl(`check`), form).toPromise();
  }

  updatePipelineDefinition(pipeline: string, raw: string) {
    const form = new FormData();
    form.set('raw', raw);
    return this.client.put<string>(PipelineApiService.getUrl(`${pipeline}/raw`), form).toPromise();
  }

  getPipelineDefinition(pipeline: string) {
    return this.client.get<PipelineInfo>(PipelineApiService.getUrl(`${pipeline}`)).toPromise();
  }

  getPipelineDefinitions() {
    return this.client.get<PipelineInfo[]>(PipelineApiService.getUrl()).toPromise();
  }

  /**
   * @deprecated Stages ({@link StageInfo}[]) is already included in the {@link PipelineInfo}
   */
  getStageDefinitions(pipelineId: string) {
    return this.client
        .get<StageInfo[]>(environment.apiLocation + 'stages/' + pipelineId)
        .toPromise();
  }

  createPipelineDefinition(name: string) {
    const form = new FormData();
    form.set('name', name);
    return this.client.put<PipelineInfo>(PipelineApiService.getUrl(`create`), form).toPromise();
  }
}


export class PipelineInfo {
  id: string;
  name: string;
  desc?: string;
  requiredEnvVariables: string[];
  stages: StageInfo[];
}

export class StageInfo {
  name: string;
  image?: ImageInfo;
  requiredEnvVariables: string[];
}
