import {Component, ElementRef, Input, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {LogEntry, LogSource, ProjectApiService} from '../api/project-api.service';
import {Subscription} from 'rxjs';
import {LongLoadingDetector} from '../long-loading-detector';
import {MatMenuTrigger} from '@angular/material/menu';
import {MatDialog} from '@angular/material/dialog';
import {RegularExpressionEditorDialogComponent} from '../regular-expression-editor-dialog/regular-expression-editor-dialog.component';
import {ProjectInfo} from '../api/winslow-api';

@Component({
  selector: 'app-log-view',
  templateUrl: './log-view.component.html',
  styleUrls: ['./log-view.component.css']
})
export class LogViewComponent implements OnInit, OnDestroy {

  private static readonly LONG_LOADING_FLAG = 'logs';

  @ViewChild('console') htmlConsole: ElementRef<HTMLElement>;
  @ViewChild('scrollTopTarget') scrollTopTarget: ElementRef<HTMLElement>;
  @ViewChild('scrollBottomTarget') scrollBottomTarget: ElementRef<HTMLElement>;

  selectedProject: ProjectInfo = null;
  selectedStageId: string = null;

  logs?: LogEntry[] = [];
  displayLatest = false;

  stateSubscription: Subscription = null;
  logSubscription: Subscription = null;
  longLoading = new LongLoadingDetector();

  stickConsole = true;
  downloadUrl: string = '';
  projectHasRunningStage = false;
  scrollCallback: () => void = () => this.onWindowScroll();

  contextMenu: { x: number, y: number, log: LogEntry } = {
    x: 0,
    y: 0,
    log: null,
  };

  regularExpressionPattern = '';

  constructor(
    private api: ProjectApiService,
    private dialog: MatDialog,
  ) {
  }

  ngOnInit(): void {
    window.addEventListener('scroll', this.scrollCallback, true);
    this.stateSubscription = this.api.getProjectStateSubscriptionHandler().subscribe((id, info) => {
      if (id === this.selectedProject?.id && info != null) {
        this.projectHasRunningStage = info.state === 'Running' ;
      }
    });
  }

  ngOnDestroy() {
    window.removeEventListener('scroll', this.scrollCallback, true);
    this.unsubscribe();
    if (this.stateSubscription) {
      this.stateSubscription.unsubscribe();
      this.stateSubscription = null;
    }
  }

  onWindowScroll() {
    if (this.htmlConsole != null) {
      const rangeSize = 50;
      let element = this.htmlConsole.nativeElement;
      let offset = element.offsetHeight - window.innerHeight + rangeSize;

      while (element) {
        offset += element.offsetTop;
        element = element.offsetParent as HTMLElement;
      }

      this.stickConsole = offset <= window.scrollY + rangeSize && offset >= window.scrollY - rangeSize;
    }
  }

  onConsoleScroll($event: Event) {
    const element = $event.target as HTMLDivElement;
    this.stickConsole = (element.scrollHeight - element.clientHeight) <= element.scrollTop;
  }

  @Input()
  set project(value: ProjectInfo) {
    const changed = value?.id !== this.selectedProject?.id;
    this.selectedProject = value;

    if (changed) {
      this.logs = [];
      this.selectedStageId = null;
      this.displayLatest = true;
      this.resubscribe(value.id, this.selectedStageId);
      this.projectHasRunningStage = false;
    }
  }

  @Input()
  set selectedStage(id: string) {
    this.unsubscribe();
    this.selectedStageId = id;
    if (this.selectedProject) {
      this.resubscribe(this.selectedProject.id, id);
    }
  }

  isLongLoading() {
    return this.longLoading.isLongLoading();
  }

  private resubscribe(projectId: string, stageId = ProjectApiService.LOGS_LATEST) {
    this.unsubscribe();
    this.logs = [];
    this.selectedStageId = null;
    this.subscribeLogs(projectId, stageId);
  }

  private unsubscribe() {
    if (this.logSubscription) {
      this.logSubscription.unsubscribe();
      this.logSubscription = null;
    }
  }

  private subscribeLogs(projectId: string, stageId = ProjectApiService.LOGS_LATEST) {
    if (stageId == null) {
      stageId = ProjectApiService.LOGS_LATEST;
    }
    this.downloadUrl = this.api.getLogRawUrl(projectId, stageId);
    this.longLoading.raise(LogViewComponent.LONG_LOADING_FLAG);
    this.displayLatest = ProjectApiService.LOGS_LATEST === stageId;
    this.logSubscription = this.api.watchLogs(projectId, (logs) => {
      this.longLoading.clear(LogViewComponent.LONG_LOADING_FLAG);
      if (logs?.length > 0) {
        if (this.logs == null || this.logs.length === 0 || this.logs[0].stageId !== logs[0].stageId) {
          this.selectedStageId = logs[0].stageId;
          this.downloadUrl = this.api.getLogRawUrl(projectId, this.selectedStageId);
          this.logs = [];
        }
        this.logs.push(...logs);
        this.scrollConsoleToBottom(this.stickConsole, false);
      } else {
        this.logs = [];
      }
    }, stageId);
  }


  showLatestLogs() {
    this.resubscribe(this.selectedProject.id);
  }

  forceReloadLogs() {
    this.resubscribe(this.selectedProject.id, this.displayLatest ? null : this.selectedStageId);
  }

  scrollConsoleToTop(smooth = true) {
    this.stickConsole = false;
    this.scrollTopTarget.nativeElement.scrollIntoView({
      behavior: smooth ? 'smooth' : 'auto',
      block: 'end'
    });
  }

  private scrollToBottomTarget(smooth = true) {
    this.scrollBottomTarget.nativeElement.scrollIntoView({
      behavior: smooth ? 'smooth' : 'auto',
      block: 'end'
    });
  }

  scrollConsoleToBottomTimeout(checked: boolean) {
    setTimeout(() => {
      if (checked) {
        this.scrollConsoleToBottom(checked);
      }
    });
  }

  scrollConsoleToBottom(overwrite = false, smooth = true) {
    if (this.stickConsole || overwrite) {
      this.stickConsole = true;
      if (!smooth) {
        setTimeout(() => {
          if (this.htmlConsole) {
            this.htmlConsole.nativeElement.scrollTop = 9_999_999_999;
          }
        });
      } else {
        setTimeout(() => this.scrollToBottomTarget());
      }
    }
  }

  lineId(index: number, log: LogEntry): string {
    return log?.stageId + log?.line;
  }

  sourceIsManagement(source: LogSource) {
    return source === LogSource.MANAGEMENT_EVENT;
  }

  toDate(time: number) {
    if (time) {
      return new Date(time).toLocaleString();
    } else {
      return '';
    }
  }

  isPatternMatching() {
    return this.regularExpressionPattern.trim().length > 0;
  }

  rightClickAction(matMenuTrigger: MatMenuTrigger, event: MouseEvent, log: LogEntry) {
    event.preventDefault();
    this.contextMenu.x = event.x;
    this.contextMenu.y = event.y;
    this.contextMenu.log = log;
    matMenuTrigger.openMenu();
  }

  contextMenuCopy() {
    navigator.clipboard.writeText(this.contextMenu.log.message).then();
  }

  contextMenuOpenEditor() {
    this.dialog.open(RegularExpressionEditorDialogComponent, {data: this.contextMenu.log.message});
  }
}
