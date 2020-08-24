import {ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {ExecutionGroupInfo, StageInfo} from '../api/project-api.service';

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
  @Input() executionGroup: ExecutionGroupInfo;
  @Input() expanded = false;
  @Input() pipelineIsPaused: boolean = null;

  @Output() clickResumeOnlyThisStage = new EventEmitter<ExecutionGroupInfo>();
  @Output() clickResume = new EventEmitter<ExecutionGroupInfo>();
  @Output() clickDelete = new EventEmitter<ExecutionGroupInfo>();
  @Output() clickPauseAfterThis = new EventEmitter<ExecutionGroupInfo>();
  @Output() clickKillStage = new EventEmitter<StageInfo>();
  @Output() clickUseAsBlueprint = new EventEmitter<StageInfo>();
  @Output() clickOpenLogs = new EventEmitter<StageInfo>();
  @Output() clickOpenWorkspace = new EventEmitter<StageInfo>();
  @Output() clickOpenTensorboard = new EventEmitter<StageInfo>();

  constructor(private cdr: ChangeDetectorRef) {
  }

  ngOnInit(): void {

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
