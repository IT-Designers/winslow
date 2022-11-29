import { Component, EventEmitter, HostListener, Input, OnInit, Output } from '@angular/core';
import {ExecutionGroupInfo, IExecutionGroupInfoExt, IStageInfoExt, StageInfo} from '../api/project-api.service';
import {EditorState} from '../pipeline-editor/pipeline-editor.component';
import {IState} from '../api/winslow-api';

@Component({
  selector: 'app-project-history-details',
  templateUrl: './project-history-details.component.html',
  styleUrls: ['./project-history-details.component.css']
})
export class ProjectHistoryDetailsComponent implements OnInit {

  @Input() entry: IExecutionGroupInfoExt;
  @Input() entryNumber: number;
  @Input() stageNumber: number;
  @Input() selectedStage: IStageInfoExt;
  @Input() projectState: IState;
  @Input() firstEntry = true;
  @Input() executionGroup: IExecutionGroupInfoExt;
  @Input() expanded = false;
  @Input() pipelineIsPaused: boolean = null;

  @Output() clickResumeOnlyThisStage = new EventEmitter<IExecutionGroupInfoExt>();
  @Output() clickResume = new EventEmitter<IExecutionGroupInfoExt>();
  @Output() clickDelete = new EventEmitter<IExecutionGroupInfoExt>();
  @Output() clickPauseAfterThis = new EventEmitter<IExecutionGroupInfoExt>();
  @Output() clickKillStage = new EventEmitter<IStageInfoExt>();
  @Output() clickUseAsBlueprint = new EventEmitter<IStageInfoExt>();
  @Output() clickOpenWorkspace = new EventEmitter<IStageInfoExt>();
  @Output() clickOpenLogs = new EventEmitter<IStageInfoExt>();
  @Output() clickOpenAnalysis = new EventEmitter<IStageInfoExt>();
  @Output() clickOpenTensorboard = new EventEmitter<IStageInfoExt>();

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
    this.setHistoryDetailsHeight(window.innerHeight)
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
    this.onButtonValueChange("stage-definition")
    this.showStageDefinition = true;
  }

  getStageInformation() {
    this.onButtonValueChange("stage-information")
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
