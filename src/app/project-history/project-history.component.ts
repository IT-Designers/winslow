import {ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {ExecutionGroupInfo, IExecutionGroupInfoExt, ProjectApiService, StageInfo} from '../api/project-api.service';
import {IStageInfo, IState} from '../api/winslow-api';

@Component({
  selector: 'app-project-history',
  templateUrl: './project-history.component.html',
  styleUrls: ['./project-history.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProjectHistoryComponent implements OnInit {

  visibleStages = 10;

  @Input() firstEntry = true;
  @Input() entryNumber = 0;
  @Input() executionGroup: IExecutionGroupInfoExt;
  @Input() expanded = false;
  @Input() pipelineIsPaused: boolean = null;
  @Input() active = false;
  @Input() projectState: IState;

  @Output() clickResumeOnlyThisStage = new EventEmitter<IExecutionGroupInfoExt>();
  @Output() clickResume = new EventEmitter<IExecutionGroupInfoExt>();
  @Output() clickDelete = new EventEmitter<IExecutionGroupInfoExt>();
  @Output() clickPauseAfterThis = new EventEmitter<IExecutionGroupInfoExt>();
  @Output() clickKillStage = new EventEmitter<IStageInfo>();
  @Output() clickUseAsBlueprint = new EventEmitter<IStageInfo>();
  @Output() clickOpenWorkspace = new EventEmitter<IStageInfo>();
  @Output() clickOpenLogs = new EventEmitter<IStageInfo>();
  @Output() clickOpenAnalysis = new EventEmitter<IStageInfo>();
  @Output() clickOpenTensorboard = new EventEmitter<IStageInfo>();
  @Output() clickGetStage = new EventEmitter<IStageInfo>();

  selectedStageIndex: number;

  constructor(private cdr: ChangeDetectorRef,
              private api: ProjectApiService) {
  }

  ngOnInit(): void {

  }

  setSelectedStageIndexAndEmitStage() {
    this.selectedStageIndex = 0;
    this.clickGetStage.emit(this.executionGroup.stages[this.executionGroup.stages.length - 1]);
  }

  getRangeEnvVariableValues(stage: IStageInfo): string {
    if (this.executionGroup.getGroupSize() > 1) {
      return [...this.executionGroup
        .rangedValues_keys()]
        .sort()
        .map(e => e + '=' + stage.env[e])
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
