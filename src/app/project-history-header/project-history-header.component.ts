import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {State} from '../api/project-api.service';

@Component({
  selector: 'app-project-history-header',
  templateUrl: './project-history-header.component.html',
  styleUrls: ['./project-history-header.component.css']
})
export class ProjectHistoryHeaderComponent implements OnInit {

  @Input() itemNo: number;
  @Input() state: State;
  @Input() isConfigure: boolean;
  @Input() time: number;
  @Input() stageName: string;
  @Input() paused: boolean;
  @Input() enqueued: boolean;
  @Input() running: boolean;
  @Input() showActiveControls: boolean;
  @Input() showPassiveControls: boolean;
  @Input() showResumeOnlyThisStage: boolean;
  @Input() showDeleteEnqueued: boolean;
  @Input() showUseAsBlueprint: boolean;

  @Output() clickResumeOnlyThisStage = new EventEmitter<MouseEvent>();
  @Output() clickResume = new EventEmitter<MouseEvent>();
  @Output() clickDelete = new EventEmitter<MouseEvent>();
  @Output() clickPauseAfterThis = new EventEmitter<MouseEvent>();
  @Output() clickKillCurrentStage = new EventEmitter<MouseEvent>();
  @Output() clickUseAsBlueprint = new EventEmitter<MouseEvent>();
  @Output() clickOpenLogs = new EventEmitter<MouseEvent>();
  @Output() clickOpenWorkspace = new EventEmitter<MouseEvent>();
  @Output() clickOpenTensorboard = new EventEmitter<MouseEvent>();

  constructor() { }

  ngOnInit() {

  }

  toDate(time: number) {
    if (time) {
      return new Date(time).toLocaleString();
    } else {
      return '';
    }
  }

}
