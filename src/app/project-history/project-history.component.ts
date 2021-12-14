import {ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {ExecutionGroupInfo, ProjectApiService, StageInfo, State} from '../api/project-api.service';

@Component({
  selector: 'app-project-history',
  templateUrl: './project-history.component.html',
  styleUrls: ['./project-history.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProjectHistoryComponent implements OnInit {

  State = State;

  visibleStages = 10;

  @Input() firstEntry = true;
  @Input() entryNumber = 0;
  @Input() executionGroup: ExecutionGroupInfo;
  @Input() expanded = false;
  @Input() pipelineIsPaused: boolean = null;
  @Input() active: boolean = false;
  @Input() projectState: State;

  @Output() clickResumeOnlyThisStage = new EventEmitter<ExecutionGroupInfo>();
  @Output() clickResume = new EventEmitter<ExecutionGroupInfo>();
  @Output() clickDelete = new EventEmitter<ExecutionGroupInfo>();
  @Output() clickPauseAfterThis = new EventEmitter<ExecutionGroupInfo>();
  @Output() clickKillStage = new EventEmitter<StageInfo>();
  @Output() clickUseAsBlueprint = new EventEmitter<StageInfo>();
  @Output() clickOpenLogs = new EventEmitter<StageInfo>();
  @Output() clickOpenWorkspace = new EventEmitter<StageInfo>();
  @Output() clickOpenTensorboard = new EventEmitter<StageInfo>();
  @Output() clickGetStage = new EventEmitter<StageInfo>();

  selectedStageIndex: number;

  constructor(private cdr: ChangeDetectorRef,
    private api: ProjectApiService) {
  }

  ngOnInit(): void {

  }

  setSelectedStageIndexAndEmitStage() {
    this.selectedStageIndex = 0;
    this.clickGetStage.emit(this.executionGroup.stages[this.executionGroup.stages.length-1])
  }

  getRangeEnvVariableValues(stage: StageInfo): string {
    if (this.executionGroup.getGroupSize() > 1) {
      return [...this.executionGroup
        .rangedValues
        .keys()]
        .sort()
        .map(e => e + '=' + stage.env.get(e))
        .join(', ');
    } else {
      return null;
    }
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
