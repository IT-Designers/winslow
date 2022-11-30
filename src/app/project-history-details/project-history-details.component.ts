import { Component, EventEmitter, HostListener, Input, OnInit, Output } from '@angular/core';
import {ExecutionGroupInfoExt, StageInfoExt} from '../api/project-api.service';
import {State} from '../api/winslow-api';

@Component({
  selector: 'app-project-history-details',
  templateUrl: './project-history-details.component.html',
  styleUrls: ['./project-history-details.component.css']
})
export class ProjectHistoryDetailsComponent implements OnInit {

  @Input() entry: ExecutionGroupInfoExt;
  @Input() entryNumber: number;
  @Input() stageNumber: number;
  @Input() selectedStage: StageInfoExt;
  @Input() projectState: State;
  @Input() firstEntry = true;
  @Input() executionGroup: ExecutionGroupInfoExt;
  @Input() expanded = false;
  @Input() pipelineIsPaused: boolean = null;

  @Output() clickResumeOnlyThisStage = new EventEmitter<ExecutionGroupInfoExt>();
  @Output() clickResume = new EventEmitter<ExecutionGroupInfoExt>();
  @Output() clickDelete = new EventEmitter<ExecutionGroupInfoExt>();
  @Output() clickPauseAfterThis = new EventEmitter<ExecutionGroupInfoExt>();
  @Output() clickKillStage = new EventEmitter<StageInfoExt>();
  @Output() clickUseAsBlueprint = new EventEmitter<StageInfoExt>();
  @Output() clickOpenWorkspace = new EventEmitter<StageInfoExt>();
  @Output() clickOpenLogs = new EventEmitter<StageInfoExt>();
  @Output() clickOpenAnalysis = new EventEmitter<StageInfoExt>();
  @Output() clickOpenTensorboard = new EventEmitter<StageInfoExt>();

  historyDetailsHeight: any;
  @HostListener('window:resize', ['$event'])
  getScreenSize(event?) {
    this.setHistoryDetailsHeight(window.innerHeight);
  }


  buttonValue: string;
  showStageDefinition: boolean;

  constructor() {
    this.buttonValue = "stage-definition";
    this.showStageDefinition = true;
    this.setHistoryDetailsHeight(window.innerHeight);
  }

  ngOnInit(): void {

  }

  setHistoryDetailsHeight(height: number) {
    this.historyDetailsHeight = 0.51 * (height - 136);
  }

  onButtonValueChange(value: string) {
    this.buttonValue = value;
  }

  getStageDefinition() {
    this.onButtonValueChange("stage-definition");
    this.showStageDefinition = true;
  }

  getStageInformation() {
    this.onButtonValueChange("stage-information");
    this.showStageDefinition = false;
  }

  trackKey(keyValue: any): any {
    return keyValue.key;
  }

  toDate(startTime: number): string {
    if (startTime) {
      return new Date(startTime).toLocaleString();
    } else {
      return '';
    }
  }

}
