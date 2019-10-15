import {Component, ElementRef, EventEmitter, Inject, Input, OnDestroy, OnInit, Output, ViewChild} from '@angular/core';
import {HistoryEntry, ImageInfo, LogEntry, LogSource, Project, ProjectApiService, State, StateInfo} from '../api/project-api.service';
import {NotificationService} from '../notification.service';
import {MAT_DIALOG_DATA, MatDialog, MatDialogRef, MatSelect, MatSelectChange, MatTabGroup} from '@angular/material';
import {LongLoadingDetector} from '../long-loading-detector';
import {FileBrowseDialog} from '../file-browse-dialog/file-browse-dialog.component';
import {FormControl, FormGroup, Validators} from '@angular/forms';


@Component({
  selector: 'app-project-view',
  templateUrl: './project-view.component.html',
  styleUrls: ['./project-view.component.css']
})
export class ProjectViewComponent implements OnInit, OnDestroy {

  @ViewChild('tabGroup', {static: false}) tabs: MatTabGroup;
  @ViewChild('console', {static: false}) console: ElementRef<HTMLElement>;
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

  constructor(private api: ProjectApiService, private notification: NotificationService,
              private createDialog: MatDialog) {
  }

  ngOnInit(): void {
    this.filesAdditionalRoot = `${this.project.name};workspaces/${this.project.id}`;
  }

  ngOnDestroy(): void {
  }

  update(info: StateInfo) {
    this.state = info.state;
    this.pauseReason = info.pauseReason;
    this.progress = info.stageProgress;

    if (this.state !== State.Failed && this.pauseReason != null) {
      this.state = State.Warning;
    }

    this.stateEmitter.emit(this.state);
    this.pollWatched();
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
      .finally(() => this.longLoading.decrease());
  }

  requestLogs() {
    if (this.watchLatestLogs) {
      const skipLines = this.logs != null ? this.logs.length : 0;
      const expectingStageId = this.logs != null && this.logs.length > 0 ? this.logs[0].stageId: null;
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
        if (this.history === null || this.history.length !== history.length || JSON.stringify(this.history) !== JSON.stringify(history)) {
          this.history = history;
        }
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
    return new Date(time).toLocaleString();
  }

  setNextStage(nextStageIndex: string, singleStageOnly = false) {
    this.longLoading.increase();
    this.api.resume(this.project.id, Number(nextStageIndex), singleStageOnly, this.formGroupControl.value, this.image)
      .toPromise()
      .then(result => {
        this.notification.info('Request has been accepted');
        this.paused = false;
        this.stateEmitter.emit(this.state = State.Running);
        this.pauseReason = null;
        this.imageOriginal = this.image;
        this.openLogs(null, true);
      }).catch(error => {
      this.notification.error('Request failed: ' + JSON.stringify(error));
    }).finally(() => this.longLoading.decrease());
  }

  updateRequestPause(checked: boolean) {
    this.longLoading.increase();
    const before = this.paused;
    this.paused = checked;
    this.api.setProjectPaused(this.project.id, checked)
      .toPromise()
      .then(result => {
        this.notification.info('Project updated');
        if (!this.paused) {
          this.stateEmitter.emit(this.state = State.Running);
          this.pauseReason = null;
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
        additionalRoot: `${this.project.name};workspaces/${this.project.id}`,
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
    return JSON.stringify(args);
  }

  parse(args: string) {
    return JSON.parse(args);
  }

  sourceIsManagement(source: LogSource) {
    return source === LogSource.MANAGEMENT_EVENT;
  }

  forceReloadLogs() {
    this.logs = null;
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

  scrollConsoleToBottom(overwrite = false) {
    if (this.stickConsole || overwrite) {
      this.console.nativeElement.scrollTop = 9_999_999_999;
      this.stickConsole = true;
    }
  }

  killCurrentStage() {
    this.longLoading.increase();
    this.api
        .killStage(this.project.id)
        .toPromise()
        .then(result => {
          this.notification.info('Request accepted');
        })
        .catch(err => {
          this.notification.error('Request failed: ' + JSON.stringify(err));
        })
        .finally(() => this.longLoading.decrease());
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

  private static deepClone(obj: any): any {
    return JSON.parse(JSON.stringify(obj));
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
