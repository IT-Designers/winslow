import {Injectable} from '@angular/core';
import {environment} from '../../environments/environment';
import {HttpClient} from '@angular/common/http';
import {StageAndGatewayDefinitionInfo, StageWorkerDefinitionInfo, StageXOrGatewayDefinitionInfo} from './winslow-api';
import {lastValueFrom} from "rxjs";

@Injectable({
  providedIn: 'root'
})
export class DefaultApiServiceService {

  private static getUrl(more?: string) {
    return `${environment.apiLocation}default${more != null ? `/${more}` : ''}`;
  }

  constructor(private client: HttpClient) {
  }

  getWorkerDefinition(): Promise<StageWorkerDefinitionInfo> {
    return lastValueFrom(this.client
      .get<StageWorkerDefinitionInfo>(DefaultApiServiceService.getUrl('worker'))
    );
  }

  getAndSplitterDefinition(): Promise<StageAndGatewayDefinitionInfo> {
    return lastValueFrom(this.client
      .get<StageAndGatewayDefinitionInfo>(DefaultApiServiceService.getUrl('and-splitter'))
    );
  }

  getAllMergerDefinition(): Promise<StageAndGatewayDefinitionInfo> {
    return lastValueFrom(this.client
      .get<StageAndGatewayDefinitionInfo>(DefaultApiServiceService.getUrl('all-merger'))
    );
  }

  getIfSplitterDefinition(): Promise<StageXOrGatewayDefinitionInfo> {
    return lastValueFrom(this.client
      .get<StageXOrGatewayDefinitionInfo>(DefaultApiServiceService.getUrl('if-splitter'))
    );
  }

  getAnyMergerDefinition(): Promise<StageXOrGatewayDefinitionInfo> {
    return lastValueFrom(this.client
      .get<StageXOrGatewayDefinitionInfo>(DefaultApiServiceService.getUrl('any-merger'))
    );
  }
}
