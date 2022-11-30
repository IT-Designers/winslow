import {Injectable} from '@angular/core';
import {environment} from '../../environments/environment';
import {HttpClient} from '@angular/common/http';
import {ParseError} from './project-api.service';
import {PipelineInfo} from './winslow-api';

@Injectable({
  providedIn: 'root'
})
export class PipelineApiService {

  constructor(private client: HttpClient) {
  }

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
    return this.client.put<string | ParseError>(PipelineApiService.getUrl(`${pipeline}/raw`), raw).toPromise();
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

  getLogParsers(pipeline: string, stage: string) {
    return this
      .client
      .get<LogParser[]>(PipelineApiService.getUrl(`${pipeline}/${stage}/logparsers`))
      .toPromise()
      .then(info => info.map(i => new LogParser(i)));
  }
}


// https://putridparrot.com/blog/extension-methods-in-typescript/
// add new functions to IPipelineInfo
// requires an Import from this module or
// import "... pipeline-api.service.ts";
declare module './winslow-api' {
  interface PipelineInfo {
    hasActionMarker(): boolean;
    hasActionMarkerFor(pipelineName: string): boolean;
  }
}


// tslint:disable-next-line:only-arrow-functions
PipelineInfo.prototype.hasActionMarker = function() {
  for (const marker of this.markers) {
    const lower = marker.toLowerCase();
    if (lower.startsWith('action')) {
      return true;
    }
  }
  return false;
};

PipelineInfo.prototype.hasActionMarkerFor = function(pipelineName: string) {
  const markers = this.markers.map(m => m.toLowerCase());
  return markers.indexOf('action') >= 0 || markers.indexOf('action for ' + pipelineName.toLowerCase()) >= 0;
};



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
