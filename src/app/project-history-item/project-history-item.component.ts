import { Component, Input, OnInit } from '@angular/core';
import { ExecutionGroupInfo, ProjectApiService, StageInfo, State } from '../api/project-api.service';

@Component({
  selector: 'app-project-history-item',
  templateUrl: './project-history-item.component.html',
  styleUrls: ['./project-history-item.component.css']
})
export class ProjectHistoryItemComponent implements OnInit {

  State = State;

  @Input() firstEntry = true;
  @Input() entryNumber = 0;
  @Input() executionGroup: ExecutionGroupInfo;
  @Input() expanded = false;
  @Input() pipelineIsPaused: boolean = null;

  constructor(private api: ProjectApiService) { }

  ngOnInit(): void {
    console.log(this.firstEntry)
    console.log(this.entryNumber)
    console.log(this.executionGroup)
    console.log(this.expanded)
    console.log(this.pipelineIsPaused)
  }

  toDate(time: number) {
    if (time) {
      return new Date(time).toLocaleString();
    } else {
      return '';
    }
  }

  trackStageInfo(value: StageInfo): string {
    return value.id;
  }

  min(a: number, b: number) {
    return Math.min(a, b);
  }

  max(a: number, b: number) {
    return Math.max(a, b);
  }

  tryParseStageNumber(stageId: string, alt: number): number {
    return this.api.tryParseStageNumber(stageId, alt);
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

}
