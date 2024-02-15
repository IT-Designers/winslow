import {ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {ExecutionGroupInfo, StageInfo, State} from '../../../api/winslow-api';
import {ExecutionGroupInfoHelper} from "../../../api/project-api.service";

@Component({
  selector: 'app-project-history',
  templateUrl: './project-history.component.html',
  styleUrls: ['./project-history.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProjectHistoryComponent implements OnInit {

  visibleStages = 10;

  @Input() executionGroup!: ExecutionGroupInfoHelper;

  @Input() firstEntry = true;
  @Input() entryNumber = 0;
  @Input() expanded = false;
  @Input() active = false;

  @Input() projectState?: State;
  @Input() pipelineIsPaused: boolean = false;

  @Output() clickResumeOnlyThisStage = new EventEmitter<ExecutionGroupInfoHelper>();
  @Output() clickResume = new EventEmitter<ExecutionGroupInfoHelper>();
  @Output() clickDelete = new EventEmitter<ExecutionGroupInfoHelper>();
  @Output() clickPauseAfterThis = new EventEmitter<ExecutionGroupInfoHelper>();
  @Output() clickKillStage = new EventEmitter<StageInfo>();
  @Output() clickUseAsBlueprint = new EventEmitter<StageInfo>();
  @Output() clickOpenWorkspace = new EventEmitter<StageInfo>();
  @Output() clickOpenLogs = new EventEmitter<StageInfo>();
  @Output() clickOpenAnalysis = new EventEmitter<StageInfo>();
  @Output() clickOpenTensorboard = new EventEmitter<StageInfo>();
  @Output() clickGetStage = new EventEmitter<StageInfo>();

  selectedStageIndex: number = 0;

  constructor(private cdr: ChangeDetectorRef) {
  }

  ngOnInit(): void {

  }

  setSelectedStageIndexAndEmitStage() {
    this.selectedStageIndex = 0;
    this.clickGetStage.emit(this.executionGroup.executionGroupInfo.stages[this.executionGroup.executionGroupInfo.stages.length - 1]);
  }

  set initiallyVisibleStages(count: number) {
    this.visibleStages = count;
    this.cdr.markForCheck();
  }

  incrementVisibleStagesBy(increment: number) {
    this.visibleStages += increment;
    this.cdr.markForCheck();
  }
}
