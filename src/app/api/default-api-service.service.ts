import { Injectable } from '@angular/core';
import {environment} from '../../environments/environment';
import {HttpClient} from '@angular/common/http';
import {StageDefinitionInfoUnion} from './winslow-api';
import {map} from 'rxjs/operators';
import {loadStageDefinition} from "./project-api.service";


@Injectable({
  providedIn: 'root'
})
export class DefaultApiServiceService {

  private static getUrl(more?: string) {
    return `${environment.apiLocation}default${more != null ? `/${more}` : ''}`;
  }

  constructor(private client: HttpClient) { }

  getWorkerDefinition(): Promise<StageDefinitionInfoUnion> {
    return this.client.get<StageDefinitionInfoUnion>(DefaultApiServiceService.getUrl('worker'))
      .pipe(map(response => loadStageDefinition(response)))
      .toPromise();
  }

  getAndSplitterDefinition(): Promise<StageDefinitionInfoUnion> {
    return this
      .client
      .get<StageDefinitionInfoUnion>(DefaultApiServiceService.getUrl('and-splitter'))
      .pipe(map(response => loadStageDefinition(response)))
      .toPromise();
  }

  getAllMergerDefinition(): Promise<StageDefinitionInfoUnion> {
    return this
      .client
      .get<StageDefinitionInfoUnion>(DefaultApiServiceService.getUrl('all-merger'))
      .pipe(map(response => loadStageDefinition(response)))
      .toPromise();
  }

  getIfSplitterDefinition(): Promise<StageDefinitionInfoUnion> {
    return this
      .client
      .get<StageDefinitionInfoUnion>(DefaultApiServiceService.getUrl('if-splitter'))
      .pipe(map(response => loadStageDefinition(response)))
      .toPromise();
  }

  getAnyMergerDefinition(): Promise<StageDefinitionInfoUnion> {
    return this
      .client
      .get<StageDefinitionInfoUnion>(DefaultApiServiceService.getUrl('any-merger'))
      .pipe(map(response => loadStageDefinition(response)))
      .toPromise();
  }
}
