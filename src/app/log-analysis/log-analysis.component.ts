import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {LogEntry, ProjectApiService, ProjectInfo} from "../api/project-api.service";
import {Observable, Subscription} from "rxjs";

@Component({
  selector: 'app-log-analysis',
  templateUrl: './log-analysis.component.html',
  styleUrls: ['./log-analysis.component.css']
})
export class LogAnalysisComponent implements OnInit, OnDestroy {


  dummyLogObservable: Observable<LogEntry>;

  logSubscription: Subscription = null;

  logs?: LogEntry[] = [];

  selectedProject: ProjectInfo = null;
  selectedStageId: string = null;

  @Input()
  set project(value: ProjectInfo) {
    this.selectedProject = value;
  }

  @Input()
  set selectedStage(id: string) {
    this.selectedStageId = id;
  }

  constructor(private api: ProjectApiService) {
  }

  ngOnInit(): void {
    this.subscribeLogs(this.selectedProject.id, this.selectedStageId)
  }

  ngOnDestroy() {
    if (this.logSubscription) {
      this.logSubscription.unsubscribe();
      this.logSubscription = null;
    }
  }

  private subscribeLogs(projectId: string, stageId = ProjectApiService.LOGS_LATEST) {
    if (stageId == null) {
      stageId = ProjectApiService.LOGS_LATEST;
    }
    //this.longLoading.raise(LogViewComponent.LONG_LOADING_FLAG);
    this.logSubscription = this.api.watchLogs(projectId, (logs) => {
      //this.longLoading.clear(LogViewComponent.LONG_LOADING_FLAG);
      if (logs?.length > 0) {
        this.logs.push(...logs);
      } else {
        this.logs = [];
      }
      console.log("LOGS:")
      console.log(logs);
      console.log(projectId, stageId);
    }, stageId);
  }

  lineId(index, log): string {
    return log.stageId + log.line;
  }

}
