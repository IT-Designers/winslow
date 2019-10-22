import {Component, ElementRef, EventEmitter, Inject, Input, OnDestroy, OnInit, Output, ViewChild} from '@angular/core';
import {HistoryEntry, ImageInfo, LogEntry, LogSource, Project, ProjectApiService, State, StateInfo} from '../api/project-api.service';
import {NotificationService} from '../notification.service';
import {CanDisable, MAT_DIALOG_DATA, MatDialog, MatDialogRef, MatSelect, MatTabGroup} from '@angular/material';
import {LongLoadingDetector} from '../long-loading-detector';
import {FileBrowseDialog} from '../file-browse-dialog/file-browse-dialog.component';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {parseArgsStringToArgv} from 'string-argv';
import {PipelineApiService, PipelineInfo} from '../api/pipeline-api.service';


@Component({
  selector: 'app-project-view',
  templateUrl: './project-view.component.html',
  styleUrls: ['./project-view.component.css']
})
export class ProjectViewComponent implements OnInit, OnDestroy {

  constructor(private api: ProjectApiService, private notification: NotificationService,
              private pipelinesApi: PipelineApiService, private createDialog: MatDialog) {
  }

  @ViewChild('tabGroup', {static: false}) tabs: MatTabGroup;
  @ViewChild('console', {static: false}) htmlConsole: ElementRef<HTMLElement>;
  @ViewChild('scrollTopTarget', {static: false}) scrollTopTarget: ElementRef<HTMLElement>;
  @ViewChild('scrollBottomTarget', {static: false}) scrollBottomTarget: ElementRef<HTMLElement>;
  @ViewChild('stageSelection', {static: false}) stageSelection: MatSelect;

  @Input() project: Project;
  @Output('state') stateEmitter = new EventEmitter<State>();

  filesAdditionalRoot: string = null;
  filesNavigationTarget: string = null;

  state?: State = null;
  history?: HistoryEntry[] = null;
  logs?: LogEntry[] = null;
  paused: boolean = null;
  pauseReason?: string = null;
  progress?: number;

  watchHistory = false;
  watchPaused = false;
  watchLogs = false;
  watchLogsInterval: any = null;
  watchLogsId?: string = null;
  watchLatestLogs = true;
  watchVersion: number = null;

  loadLogsOnceAnyway = false;

  longLoading = new LongLoadingDetector();
  formGroupControl = new FormGroup({}, [Validators.required]);

  image: ImageInfo = null;
  imageOriginal: ImageInfo = null;

  stickConsole = true;
  consoleIsLoading = false;
  scrollCallback;
  pipelines: PipelineInfo[];

  private static deepClone(obj: any): any {
    return JSON.parse(JSON.stringify(obj));
  }

  ngOnInit(): void {
    this.filesAdditionalRoot = `${this.project.name};workspaces/${this.project.id}`;
    this.scrollCallback = () => this.onWindowScroll();
    window.addEventListener('scroll', this.scrollCallback, true);
    this.pipelinesApi.getPipelineDefinitions().then(result => this.pipelines = result);
  }

  ngOnDestroy(): void {
    window.removeEventListener('scroll', this.scrollCallback, true);
  }

  update(info: StateInfo) {
    this.state = info.state;
    this.pauseReason = info.pauseReason;
    this.progress = info.stageProgress;

    if (this.state !== State.Paused && info.hasEnqueuedStages) {
      this.state = State.Enqueued;
    }

    if (this.state !== State.Failed && this.pauseReason != null) {
      this.state = State.Warning;
    }

    this.stateEmitter.emit(this.state);
    this.pollWatched();
  }

  isEnqueued(state = this.state): boolean {
    return State.Enqueued === state;
  }

  isRunning(state = this.state): boolean {
    return State.Running === state;
  }

  pollWatched(): void {
    const changed = this.watchVersion !== this.project.version;
    this.watchVersion = this.project.version;

    if (this.watchHistory && (this.isRunning() || changed)) {
      this.loadHistory();
    }
    if (this.watchPaused && (this.isRunning() || changed)) {
      this.loadPaused();
    }
    if (this.watchLogs && (this.isRunning() || changed || this.loadLogsOnceAnyway)) {
      if (!this.watchLogsInterval) {
        this.watchLogsInterval = setInterval(() => this.loadLogs(), 1000);
      }
      this.loadLogs();
    } else if (this.watchLogsInterval) {
      clearInterval(this.watchLogsInterval);
      this.watchLogsInterval = null;
    }
  }

  loadLogs() {
    if (this.consoleIsLoading) {
      return;
    }
    this.consoleIsLoading = true;
    this.longLoading.increase();
    return this.requestLogs()
      .then(logs => {
        if (this.logs == null) {
          this.logs = [];
        }
        this.loadLogsOnceAnyway = this.isRunning();
        if (logs.length > 0 && this.logs.length > 0 && logs[0].stageId !== this.logs[0].stageId) {
          this.logs = null;
          return this.loadLogs();
        } else {
          logs.forEach(entry => this.logs.push(entry));
          if (logs.length > 0) {
            // execute it after the DOM update
            setTimeout(() => this.scrollConsoleToBottom());
          }
        }
      })
      .finally(() => {
        this.longLoading.decrease();
        this.consoleIsLoading = false;
      });
  }

  requestLogs() {
    if (this.watchLatestLogs) {
      const skipLines = this.logs != null ? this.logs.length : 0;
      const expectingStageId = this.logs != null && this.logs.length > 0 ? this.logs[0].stageId : null;
      return this.api.getLatestLogs(this.project.id, skipLines, expectingStageId).toPromise();
    } else {
      this.logs = [];
      return this.api.getLog(this.project.id, this.watchLogsId).toPromise();
    }
  }

  loadHistory() {
    this.longLoading.increase();
    return this.api.getProjectHistory(this.project.id)
      .toPromise()
      .then(history => {
        history = history.reverse();
        return this.api.getProjectEnqueued(this.project.id)
          .then(enqueued => {
            // remember state before adding to other history entires
            for (let i = 0; i < enqueued.length; ++i) {
              enqueued[i].enqueueIndex = i;
              enqueued[i].enqueueControlSize = enqueued.length;
            }

            const latest = enqueued.reverse();
            history.forEach(h => latest.push(h));
            if (this.history === null || this.history.length !== latest.length || JSON.stringify(this.history) !== JSON.stringify(latest)) {
              this.history = latest;
            }
          });
      })
      .finally(() => this.longLoading.decrease());
  }

  loadPaused(): void {
    this.longLoading.increase();
    this.api.getProjectPaused(this.project.id)
      .toPromise()
      .then(paused => this.paused = paused)
      .finally(() => this.longLoading.decrease());
  }


  toDate(time: number) {
    if (time) {
      return new Date(time).toLocaleString();
    } else {
      return '';
    }
  }

  enqueueNextStage(nextStageIndex: string) {
    this.longLoading.increase();
    this.api.enqueue(this.project.id, Number(nextStageIndex), this.formGroupControl.value, this.image)
      .toPromise()
      .then(result => {
        this.notification.info('Request has been accepted');
        this.imageOriginal = this.image;
      }).catch(error => {
      this.notification.error('Request failed: ' + JSON.stringify(error));
    }).finally(() => this.longLoading.decrease());
  }

  updateRequestPause(pause: boolean, singleStageOnly?: boolean) {
    this.longLoading.increase();
    const before = this.paused;
    this.paused = pause;
    this.api.resume(this.project.id, pause, singleStageOnly)
      .toPromise()
      .then(result => {
        this.notification.info('Project updated');
        if (!this.paused) {
          this.stateEmitter.emit(this.state = State.Running);
          this.pauseReason = null;
          this.openLogs(null, true);
        }
      })
      .catch(err => {
        this.paused = before;
        this.notification.error('Request failed: ' + JSON.stringify(err));
      })
      .finally(() => this.longLoading.decrease());
  }

  startLoading() {
    this.onSelectedTabChanged(this.tabs.selectedIndex);
  }

  stopLoading() {
    this.onSelectedTabChanged(null);
  }

  onSelectedTabChanged(index: number) {
    this.watchPaused = this.conditionally(0 === index, () => this.loadPaused());
    this.watchHistory = this.conditionally(1 === index, () => this.loadHistory());
    this.watchLogs = this.conditionally(3 === index, () => this.loadLogs());
  }

  conditionally(condition: boolean, fn): boolean {
    if (condition) {
      fn();
    }
    return condition;
  }

  isLongLoading() {
    return this.longLoading.isLongLoading();
  }

  openFolder(project: Project, entry: HistoryEntry) {
    this.tabs.selectedIndex = 2;
    this.filesAdditionalRoot = `${project.name};workspaces/${project.id}`;
    this.filesNavigationTarget = `/workspaces/${project.id}/${entry.workspace}/`;
  }


  openLogs(entry?: HistoryEntry, watchLatestLogs = false) {
    this.tabs.selectedIndex = 3;
    this.logs = null;
    this.watchLogs = true;
    if (entry != null) {
      this.watchLogsId = entry.stageId;
      this.watchLatestLogs = false;
    }
    if (watchLatestLogs) {
      this.watchLatestLogs = true;
    }
  }

  showLatestLogs(force: boolean) {
    if (!this.watchLatestLogs || force) {
      this.logs = null;
      this.watchLogs = true;
      this.watchLogsId = null;
      this.watchLatestLogs = true;
      this.loadLogsOnceAnyway = true;
      this.loadLogs();
    }
  }

  onOverwriteStageSelectionChanged(index: number) {
    this.longLoading.increase();
    return this.api
      .getRequiredUserInput(this.project.id, index)
      .toPromise()
      .then(required => {
        this.recreateFormGroup(required);
        this.project.userInput = required;
        return this.api
          .getEnvironment(this.project.id, index)
          .toPromise()
          .then(env => {
            for (const req of required) {
              if (!env.has(req)) {
                env.set(req, null);
              }
            }
            this.project.environment = env;
            this.recreateFormGroup([...this.project.environment.keys()]);

            return this.api
              .getImage(this.project.id, index)
              .toPromise()
              .then(image => {
                if (image != null) {
                  this.imageOriginal = this.image = image;
                }
              });
          });
      })
      .catch(err => this.notification.error('Failed to retrieve environment: ' + err))
      .finally(() => this.longLoading.decrease());
  }

  private recreateFormGroup(required: string[]) {
    const fg = {};
    for (const req of required) {
      fg[req] = new FormControl(this.project && this.project.environment && this.project.environment.get(req));
    }
    this.formGroupControl = new FormGroup(fg, Validators.required);
    this.formGroupControl.markAllAsTouched();
  }

  openFileBrowseDialog(key: string) {
    this.createDialog.open(FileBrowseDialog, {
      width: '75%',
      data: {
        // additionalRoot: `${this.project.name};workspaces/${this.project.id}`,
        preselectedPath: this.project.environment.get(key) || '/resources/',
      },
    }).afterClosed().toPromise().then(result => {
      if (result != null) {
        this.formGroupControl.get(key).setValue(result);
      }
    });
  }

  addEnvironment(key: string, value: string) {
    this.project.environment.set(key, value);
    this.formGroupControl.addControl(key, new FormControl(value));
  }

  deleteEnvironment(key: string) {
    this.project.environment.delete(key);
    this.formGroupControl.removeControl(key);
  }

  stringify(args: string[]) {
    return args.map(v => {
      if (v.indexOf(' ') >= 0) {
        return '"' + v + '"';
      } else {
        return v;
      }
    }).join(' ');
  }

  parse(args: string) {
    return parseArgsStringToArgv(args);
  }

  sourceIsManagement(source: LogSource) {
    return source === LogSource.MANAGEMENT_EVENT;
  }

  forceReloadLogs() {
    this.logs = [];
    this.loadLogs();
  }

  setName(name: string) {
    this.longLoading.increase();
    this.api.setName(this.project.id, name)
      .toPromise()
      .then(result => {
        this.project.name = name;
      })
      .finally(() => this.longLoading.decrease());
  }

  delete() {
    this.createDialog.open(DeleteProjectAreYouSureDialog, {
      data: this.project
    }).afterClosed().toPromise().then(result => {
      if (!!result) {
        alert('Not yet implemented');
      }
    });
  }

  onConsoleScroll($event: Event) {
    const element = $event.target as HTMLDivElement;
    this.stickConsole = (element.scrollHeight - element.clientHeight) <= element.scrollTop;
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

  scrollConsoleToBottom(overwrite = false) {
    if (this.stickConsole || overwrite) {
      this.stickConsole = true;
      setTimeout(() => this.htmlConsole.nativeElement.scrollTop = 9_999_999_999);
      setTimeout(() => this.scrollToBottomTarget());
    }
  }

  killCurrentStage() {
    this.createDialog
      .open(StopStageAreYouSureDialog, {})
      .afterClosed()
      .toPromise()
      .then(result => {
        if (!!result) {
          this.longLoading.increase();
          return this.api
            .killStage(this.project.id)
            .toPromise()
            .then(r => this.notification.info('Request accepted'))
            .catch(e => this.notification.error('Request failed: ' + JSON.stringify(e)))
            .finally(() => this.longLoading.decrease());
        }
      });
  }

  reRun(entry: HistoryEntry) {
    this.tabs.selectedIndex = 0;
    for (let i = 0; i < this.project.pipelineDefinition.stageDefinitions.length; ++i) {
      if (entry.stageName === this.project.pipelineDefinition.stageDefinitions[i].name) {

        const list = this.stageSelection.options.map(e => e); // clone array

        this.stageSelection.value = i;
        // this.stageSelection.valueChange.emit(list[i]);

        this.onOverwriteStageSelectionChanged(i).then(result => {
          this.image = ProjectViewComponent.deepClone(entry.imageInfo);
          this.imageOriginal = ProjectViewComponent.deepClone(entry.imageInfo);
          this.project.environment = ProjectViewComponent.deepClone(entry.env);
        });
        break;
      }
    }
  }

  scrollBottom() {
    this.scrollConsoleToBottom(true);
    this.scrollToBottomTarget();
  }

  private scrollToBottomTarget(smooth = true) {
    this.scrollBottomTarget.nativeElement.scrollIntoView({
      behavior: smooth ? 'smooth' : 'auto',
      block: 'end'
    });
  }

  scrollToTopTarget(smooth = true) {
    this.scrollTopTarget.nativeElement.scrollIntoView({
      behavior: smooth ? 'smooth' : 'auto',
      block: 'start'
    });
  }

  scrollConsoleToBottomTimeout(checked: boolean) {
    setTimeout(() => {
      if (checked) {
        this.scrollConsoleToBottom(checked);
      }
    });
  }

  cancelEnqueuedStage(index: number, controlSize: number) {
    this.createDialog
      .open(StopStageAreYouSureDialog, {})
      .afterClosed()
      .toPromise()
      .then(result => {
        if (result) {
          this.longLoading.increase();
          return this.api
            .deleteEnqueued(this.project.id, index, controlSize)
            .toPromise()
            .then(r => {
              if (r) {
                this.notification.info('Request has been accepted');
              } else {
                this.notification.error('Request failed because history has changed!');
              }
              return this.loadHistory();
            })
            .catch(e => this.notification.error('Failed: ' + JSON.stringify(e)))
            .finally(() => this.longLoading.decrease());
        }
      });
  }

  setPipeline(pipelineId: string, onSuccessDisable?: HTMLInputElement | HTMLButtonElement | CanDisable) {
    this.longLoading.increase();
    this.api
      .setPipelineDefinition(this.project.id, pipelineId)
      .toPromise()
      .then(result => {
        if (result) {
          return this.pipelinesApi
            .getPipelineDefinition(pipelineId)
            .then(pipeline => {
              return this.pipelinesApi
                .getStageDefinitions(pipeline)
                .then(stages => {
                  this.project.pipelineDefinition = pipeline;
                  this.project.pipelineDefinition.id = pipelineId;
                  this.project.pipelineDefinition.stageDefinitions = stages;
                  this.notification.info('Project updated');
                  if (onSuccessDisable) {
                    onSuccessDisable.disabled = true;
                  }
                });
            });
        } else {
          return Promise.reject(result);
        }
      })
      .catch(err => {
        this.notification.error('Update declined: ' + JSON.stringify(err));
        if (onSuccessDisable) {
          onSuccessDisable.disabled = true;
        }
      })
      .finally(() => this.longLoading.decrease());
  }
}

@Component({
  selector: 'dialog-delete-project-are-you-sure',
  template: `
      <h1 mat-dialog-title>Are you sure you want to delete this project?</h1>
      <div mat-dialog-content>
          <p>{{project.name}}</p>
      </div>
      <div mat-dialog-actions align="end">
          <button mat-raised-button color="warn" (click)="dialogRef.close(true)">Delete</button>
          <button mat-raised-button (click)="dialogRef.close(false)">Cancel</button>
      </div>`
})
export class DeleteProjectAreYouSureDialog {
  constructor(
    public dialogRef: MatDialogRef<DeleteProjectAreYouSureDialog>,
    @Inject(MAT_DIALOG_DATA) public project: Project) {
  }
}


@Component({
  selector: 'dialog-stop-stage-are-you-sure',
  template: `
      <h1 mat-dialog-title>Are you sure you want to stop this stage?</h1>
      <div mat-dialog-content>
      </div>
      <div mat-dialog-actions align="end">
          <button mat-raised-button color="warn" (click)="dialogRef.close(true)">Stop</button>
          <button mat-raised-button (click)="dialogRef.close(false)">Cancel</button>
      </div>`
})
export class StopStageAreYouSureDialog {
  constructor(
    public dialogRef: MatDialogRef<StopStageAreYouSureDialog>,
    @Inject(MAT_DIALOG_DATA) public data: any) {
  }
}
