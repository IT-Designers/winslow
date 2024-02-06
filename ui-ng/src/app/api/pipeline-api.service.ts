import {Injectable} from '@angular/core';
import {environment} from '../../environments/environment';
import {HttpClient} from '@angular/common/http';
import {loadStageDefinition} from './project-api.service';
import {
  ChartDefinition,
  ParseError,
  PipelineDefinitionInfo, RangedList, RangedValueUnion, RangeWithStepSize,
  StageAndGatewayDefinitionInfo, StageDefinitionInfoUnion,
  StageWorkerDefinitionInfo, StageXOrGatewayDefinitionInfo,
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
      this.client.get<PipelineDefinitionInfo>(PipelineApiService.getUrl(`${pipelineId}`))
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
      this.client.get<PipelineDefinitionInfo[]>(PipelineApiService.getUrl())
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
    stages: origin.stages.map(stage => loadStageDefinition(stage)),
    charts: origin.charts.map(chart => new ChartDefinition(chart))
  });
}

export function isStageWorkerDefinitionInfo(def: StageDefinitionInfoUnion): def is StageWorkerDefinitionInfo {
  return (def as StageWorkerDefinitionInfo)["@type"] == "Worker"
}

export function isStageAndGatewayDefinitionInfo(def: StageDefinitionInfoUnion): def is StageAndGatewayDefinitionInfo {
  return (def as StageAndGatewayDefinitionInfo)["@type"] == "AndGateway"
}

export function isStageXorGatewayDefinitionInfo(def: StageDefinitionInfoUnion): def is StageXOrGatewayDefinitionInfo {
  return (def as StageXOrGatewayDefinitionInfo)["@type"] == "XorGateway"
}
export function isRangeWithStepSize(val: RangedValueUnion): val is RangeWithStepSize {
  return (val as RangeWithStepSize)["@type"] == "DiscreteSteps"
}

export function isRangedList(val: RangedValueUnion): val is RangedList {
  return (val as RangedList)["@type"] == "List"
}
