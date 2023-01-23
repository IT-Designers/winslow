import { Injectable } from '@angular/core';
import {environment} from '../../environments/environment';
import {HttpClient} from '@angular/common/http';
import {StageAndGatewayDefinitionInfo, StageWorkerDefinitionInfo, StageXOrGatewayDefinitionInfo} from './winslow-api';

@Injectable({
  providedIn: 'root'
})
export class DefaultApiServiceService {

  private static getUrl(more?: string) {
    return `${environment.apiLocation}default${more != null ? `/${more}` : ''}`;
  }

  constructor(private client: HttpClient) { }

  getWorkerDefinition(): Promise<StageWorkerDefinitionInfo> {
    return this
      .client
      .get<StageWorkerDefinitionInfo>(DefaultApiServiceService.getUrl('worker'))
      .toPromise();
  }

  getAndSplitterDefinition(): Promise<StageAndGatewayDefinitionInfo> {
    return this
      .client
      .get<StageAndGatewayDefinitionInfo>(DefaultApiServiceService.getUrl('and-splitter'))
      .toPromise();
  }

  getAllMergerDefinition(): Promise<StageAndGatewayDefinitionInfo> {
    return this
      .client
      .get<StageAndGatewayDefinitionInfo>(DefaultApiServiceService.getUrl('all-merger'))
      .toPromise();
  }

  getIfSplitterDefinition(): Promise<StageXOrGatewayDefinitionInfo> {
    return this
      .client
      .get<StageXOrGatewayDefinitionInfo>(DefaultApiServiceService.getUrl('if-splitter'))
      .toPromise();
  }

  getAnyMergerDefinition(): Promise<StageXOrGatewayDefinitionInfo> {
    return this
      .client
      .get<StageXOrGatewayDefinitionInfo>(DefaultApiServiceService.getUrl('any-merger'))
      .toPromise();
  }
}
