import {Component, EventEmitter, HostListener, Input, OnInit, Output} from '@angular/core';
import {
  ExecutionGroupInfo,
  StageInfo,
  State
} from '../../../api/winslow-api';
import {
  isRangeWithStepSize,
  isStageWorkerDefinitionInfo,
} from "../../../api/pipeline-api.service";
import {ExecutionGroupInfoHelper} from "../../../api/project-api.service";

@Component({
  selector: 'app-project-history-details',
  templateUrl: './project-history-details.component.html',
  styleUrls: ['./project-history-details.component.css']
})
export class ProjectHistoryDetailsComponent implements OnInit {

  @Input() entry!: ExecutionGroupInfoHelper;
  @Input() entryNumber!: number;
  @Input() selectedStage!: StageInfo;
  @Input() projectState?: State;

  @Output() clickKillStage = new EventEmitter<StageInfo>();
  @Output() clickUseAsBlueprint = new EventEmitter<StageInfo>();
  @Output() clickOpenWorkspace = new EventEmitter<StageInfo>();
  @Output() clickOpenLogs = new EventEmitter<StageInfo>();
  @Output() clickOpenAnalysis = new EventEmitter<StageInfo>();
  @Output() clickOpenTensorboard = new EventEmitter<StageInfo>();

  historyDetailsHeight: any;
  buttonValue: string;
  showStageDefinition: boolean;

  constructor() {
    this.buttonValue = 'stage-definition';
    this.showStageDefinition = true;
    this.setHistoryDetailsHeight(window.innerHeight);
  }

  @HostListener('window:resize', ['$event'])
  getScreenSize(_event?: Event) {
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
    this.onButtonValueChange('stage-definition');
    this.showStageDefinition = true;
  }

  getStageInformation() {
    this.onButtonValueChange('stage-information');
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

  isEmpty(record: Record<string, unknown>): boolean {
    return Object.keys(record).length == 0;
  }

  protected readonly isStageWorkerDefinitionInfo = isStageWorkerDefinitionInfo;
  protected readonly isRangeWithStepSize = isRangeWithStepSize;
}
