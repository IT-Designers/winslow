import {Injectable} from '@angular/core';
import {environment} from '../../environments/environment';
import {HttpClient} from '@angular/common/http';
import {loadStageDefinition} from './project-api.service';
import {
  GpuRequirementsInfo,
  ImageInfo,
  ParseError,
  PipelineDefinitionInfo,
  RequirementsInfo,
  StageWorkerDefinitionInfo,
  UserInputInfo
} from './winslow-api';

@Injectable({
  providedIn: 'root'
})
export class PipelineApiService {

  constructor(private client: HttpClient) {
  }

  private static getUrl(more?: string) {
    return `${environment.apiLocation}pipelines${more != null ? `/${more}` : ''}`;
  }

  getRawPipelineDefinition(id: string) {
    return this.client.get<string>(PipelineApiService.getUrl(`${id}/raw`)).toPromise();
  }

  setRawPipelineDefinition(id: string, raw: string) {
    return this.client
      .put<object | ParseError>(PipelineApiService.getUrl(`${id}/raw`), raw)
      .toPromise()
      .then(r => {
        if (r != null && Object.keys(r).length !== 0) {
          return Promise.reject(new ParseError(r as ParseError));
        } else {
          return Promise.resolve(null);
        }
      });
  }

  checkPipelineDefinition(raw: string): Promise<string | ParseError> {
    return this.client.post<string | ParseError>(PipelineApiService.getUrl(`check`), raw).toPromise();
  }

  updatePipelineDefinition(pipeline: string, raw: string) {
    return this.client
      .put<string | ParseError>(PipelineApiService.getUrl(`${pipeline}/raw`), raw)
      .toPromise()
      .then(response => {
        if (response == null) {
          return null;
        } else if (typeof response === typeof '') {
          console.log('pretend string');
          return response;
        } else {
          console.log('pretend parse error');
          return new ParseError(response as ParseError);
        }
      });
  }

  getPipelineDefinition(pipelineId: string) {
    return this.client
      .get<PipelineDefinitionInfo>(PipelineApiService.getUrl(`${pipelineId}`))
      .toPromise()
      .then(info => loadPipelineDefinition(info));
  }

  deletePipeline(pipelineId: string) : Promise<void> {
    return this.client
      .delete<void>(PipelineApiService.getUrl(`${pipelineId}`))
      .toPromise();
  }

  setPipelineDefinition(pipeline: PipelineDefinitionInfo) {
    return this.client
      .put<PipelineDefinitionInfo>(PipelineApiService.getUrl(), pipeline)
      .toPromise();
  }

  getPipelineDefinitions(): Promise<PipelineDefinitionInfo[]> {
    return this
      .client
      .get<PipelineDefinitionInfo[]>(PipelineApiService.getUrl())
      .toPromise()
      .then(info => info.map(i => loadPipelineDefinition(i)));
  }

  getSharedPipelineDefinitions() {
    return this
      .getPipelineDefinitions()
      .then(pipelines => pipelines.filter(pipeline => pipeline.belongsToProject == null));
  }

  createPipelineDefinition(name: string) {
    return this
      .client
      .post<PipelineDefinitionInfo>(PipelineApiService.getUrl(`create`), name)
      .toPromise()
      .then(info => loadPipelineDefinition(info));
  }
}

export function loadPipelineDefinition(origin: PipelineDefinitionInfo): PipelineDefinitionInfo {
  return new PipelineDefinitionInfo({
    ...origin,
    stages: origin.stages.map(stage => loadStageDefinition(stage))
  });
}

export function createStageWorkerDefinitionInfo(id: string, name: string): StageWorkerDefinitionInfo {
  return new StageWorkerDefinitionInfo({
    '@type': 'Worker',
    id,
    name,
    description: '',
    discardable: false,
    environment: {},
    highlight: null,
    ignoreFailuresWithinExecutionGroup: false,
    image: new ImageInfo({
      name: 'hello-world',
      args: [],
      shmMegabytes: 0
    }),
    logParsers: [],
    nextStages: [],
    privileged: false,
    requiredResources: new RequirementsInfo({
      cpus: 0,
      gpu: new GpuRequirementsInfo({
        count: 0,
        vendor: '',
        support: []
      }),
      megabytesOfRam: 0,
      tags: []
    }),
    userInput: new UserInputInfo({
      confirmation: 'NEVER',
      requiredEnvVariables: []
    })
  });
}


export class LogParser {
  destination: string;
  formatter: string;
  matcher: string;
  type: string;

  constructor(info) {
    this.destination = info.destination;
    this.formatter = info.formatter;
    this.matcher = info.matcher;
    this.type = info.type;
  }
}
