import {Injectable} from '@angular/core';
import {environment} from '../../environments/environment';
import {HttpClient} from '@angular/common/http';
import {loadStageDefinition} from './project-api.service';
import {
  GpuRequirementsInfo, HighlightInfo,
  ImageInfo,
  ParseError,
  PipelineDefinitionInfo, Raw,
  RequirementsInfo,
  StageWorkerDefinitionInfo,
  UserInputInfo
} from './winslow-api';
import {lastValueFrom} from "rxjs";

@Injectable({
  providedIn: 'root'
})
export class PipelineApiService {

  constructor(private client: HttpClient) {
  }

  private static getUrl(more?: string): string {
    return `${environment.apiLocation}pipelines${more != null ? `/${more}` : ''}`;
  }

  getRawPipelineDefinition(id: string): Promise<string> {
    return lastValueFrom(
      this.client.get<string>(PipelineApiService.getUrl(`${id}/raw`))
    );
  }

  setRawPipelineDefinition(id: string, raw: string): Promise<void> {
    return lastValueFrom(
      this.client.put<object | ParseError>(PipelineApiService.getUrl(`${id}/raw`), raw)
    ).then(r => {
      if (r != null && Object.keys(r).length !== 0) {
        return Promise.reject(new ParseError(r as ParseError));
      } else {
        return Promise.resolve();
      }
    });
  }

  checkPipelineDefinition(raw: string): Promise<string | ParseError> {
    return lastValueFrom(
      this.client.post<string | ParseError>(PipelineApiService.getUrl(`check`), raw)
    );
  }

  updatePipelineDefinition(pipeline: string, raw: string): Promise<null | string | ParseError> {
    return lastValueFrom(
      this.client.put<string | ParseError>(PipelineApiService.getUrl(`${pipeline}/raw`), raw)
    )
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

  getPipelineDefinition(pipelineId: string): Promise<PipelineDefinitionInfo> {
    return lastValueFrom(
      this.client.get<Raw<PipelineDefinitionInfo>>(PipelineApiService.getUrl(`${pipelineId}`))
    ).then(info => loadPipelineDefinition(info));
  }

  deletePipeline(pipelineId: string): Promise<void> {
    return lastValueFrom(
      this.client.delete<void>(PipelineApiService.getUrl(`${pipelineId}`))
    );
  }

  setPipelineDefinition(pipeline: PipelineDefinitionInfo) {
    return lastValueFrom(
      this.client.put<PipelineDefinitionInfo>(PipelineApiService.getUrl(), pipeline)
    );
  }

  getPipelineDefinitions(): Promise<PipelineDefinitionInfo[]> {
    return lastValueFrom(
      this.client.get<Raw<PipelineDefinitionInfo>[]>(PipelineApiService.getUrl())
    ).then(info => info.map(i => loadPipelineDefinition(i)));
  }

  getSharedPipelineDefinitions(): Promise<PipelineDefinitionInfo[]> {
    return this
      .getPipelineDefinitions()
      .then(pipelines => pipelines.filter(pipeline => pipeline.belongsToProject == null));
  }

  createPipelineDefinition(name: string): Promise<PipelineDefinitionInfo> {
    return lastValueFrom(
      this.client.post<PipelineDefinitionInfo>(PipelineApiService.getUrl(`create`), name)
    ).then(info => loadPipelineDefinition(info));
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
    highlight: new HighlightInfo({
      resources: []
    }),
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
