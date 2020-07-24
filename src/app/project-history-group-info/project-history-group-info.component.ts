import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {ExecutionGroupInfo, ProjectApiService, StageInfo, State} from '../api/project-api.service';

@Component({
  selector: 'app-project-history-group-info',
  templateUrl: './project-history-group-info.component.html',
  styleUrls: ['./project-history-group-info.component.css']
})
export class ProjectHistoryGroupInfoComponent implements OnInit {

  State = State;

  @Input() executionGroup: ExecutionGroupInfo;
  @Input() visibleStages = 10;

  @Output() clickKillStage = new EventEmitter<StageInfo>();
  @Output() clickUseAsBlueprint = new EventEmitter<StageInfo>();
  @Output() clickOpenLogs = new EventEmitter<StageInfo>();
  @Output() clickOpenWorkspace = new EventEmitter<StageInfo>();
  @Output() clickOpenTensorboard = new EventEmitter<StageInfo>();

  constructor(private api: ProjectApiService) { }

  ngOnInit(): void {
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
}
