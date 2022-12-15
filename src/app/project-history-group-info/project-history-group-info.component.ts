import {ChangeDetectionStrategy, Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {ProjectApiService} from '../api/project-api.service';
import {ExecutionGroupInfo, StageInfo} from '../api/winslow-api';

@Component({
  selector: 'app-project-history-group-info',
  templateUrl: './project-history-group-info.component.html',
  styleUrls: ['./project-history-group-info.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProjectHistoryGroupInfoComponent implements OnInit {



  @Input() executionGroup: ExecutionGroupInfo;
  @Input() visibleStages = 10;
  @Input() selectedStageIndex: number;
  @Output() selectedStageIndexChange = new EventEmitter<number>();
  @Output() clickKillStage = new EventEmitter<StageInfo>();
  @Output() clickUseAsBlueprint = new EventEmitter<StageInfo>();
  @Output() clickOpenWorkspace = new EventEmitter<StageInfo>();
  @Output() clickOpenLogs = new EventEmitter<StageInfo>();
  @Output() clickOpenAnalysis = new EventEmitter<StageInfo>();
  @Output() clickOpenTensorboard = new EventEmitter<StageInfo>();
  @Output() clickGetStage = new EventEmitter<StageInfo>();

  constructor(private api: ProjectApiService) { }

  ngOnInit(): void {

  }

  emitStageAndSetIndex(stage: StageInfo, index: number) {
    this.clickGetStage.emit(stage);
    this.selectedStageIndex = index;
    this.selectedStageIndexChange.emit(index);
  }

  tryParseStageNumber(stageId: string, alt: number): number {
    return this.api.tryParseStageNumber(stageId, alt);
  }

  min(a: number, b: number) {
    return Math.min(a, b);
  }

  toDate(startTime: number): string {
    if (startTime) {
      return new Date(startTime).toLocaleString();
    } else {
      return '';
    }
  }

  max(a: number, b: number) {
    return Math.max(a, b);
  }

  getRangeEnvVariableValues(stage: StageInfo): string {
    if (this.executionGroup.getGroupSize() > 1) {
      return [...this.executionGroup
        .rangedValuesKeys() ]
        .sort()
        .map(e => e + '=' + stage.env[e])
        .join(', ');
    } else {
      return null;
    }
  }

  trackExecutionGroup(value: ExecutionGroupInfo): string {
    return value.id;
  }

  trackStageInfo(value: StageInfo): string {
    return value.id;
  }

  trackKey(keyValue: any): any {
    return keyValue.key;
  }

}
