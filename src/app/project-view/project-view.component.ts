import {Component, EventEmitter, Input, OnDestroy, OnInit, Output, ViewChild} from '@angular/core';
import {StateInfo, HistoryEntry, LogEntry, Project, ProjectApiService, State} from '../project-api.service';
import {NotificationService} from '../notification.service';
import {MatDialog, MatTabGroup} from '@angular/material';
import {LongLoadingDetector} from '../long-loading-detector';
import {FilesComponent} from '../files/files.component';
import {map} from 'rxjs/operators';
import {FileBrowseDialog} from '../file-browse/file-browse-dialog.component';

@Component({
  selector: 'app-project-view',
  templateUrl: './project-view.component.html',
  styleUrls: ['./project-view.component.css']
})
export class ProjectViewComponent implements OnInit, OnDestroy {

  @ViewChild('tabGroup', {static: false}) tabs: MatTabGroup;
  @ViewChild('files', {static: false}) files: FilesComponent;
  @ViewChild('console', {static: false}) console: HTMLElement;

  @Input() project: Project;
  @Output('state') stateEmitter = new EventEmitter<State>();

  state?: State = null;
  history?: HistoryEntry[] = null;
  logs?: LogEntry[] = null;
  paused: boolean = null;
  pauseReason?: string = null;

  watchHistory = false;
  watchPaused = false;
  watchLogs = false;
  watchLatestLogs = true;

  watchLogsId?: string = null;

  longLoading = new LongLoadingDetector();

  constructor(private api: ProjectApiService, private notification: NotificationService,
              private createDialog: MatDialog) {
  }

  ngOnInit(): void {
  }

  ngOnDestroy(): void {
  }

  update(complex: StateInfo) {
    this.state = complex.state;
    this.pauseReason = complex.pauseReason;

    if (this.pauseReason !== null) {
      this.state = State.Warning;
    }

    this.stateEmitter.emit(this.state);
  }

  isRunning(): boolean {
    return State.Running === this.state;
  }

  pollWatched(): void {
    if (this.watchHistory && (this.isRunning() || this.historySeemsIncomplete())) {
      this.loadHistory();
    }
    if (this.watchPaused) {
      this.loadPaused();
    }
    if (this.watchLogs && (this.isRunning() || this.logs == null)) {
      this.loadLogs();
    }
  }

  historySeemsIncomplete() {
    if (this.history != null) {
      for (const h of this.history) {
        if (h.state === State.Running || h.state === State.Paused) {
          return true;
        }
      }
      return false;
    } else {
      return true;
    }
  }

  loadLogs() {
    this.longLoading.increase();
    const stage = this.watchLatestLogs ? 'latest' : this.watchLogsId;
    return this.api.getLog(this.project.id, stage)
      .toPromise()
      .then(logs => this.logs = logs)
      .finally(() => this.longLoading.decrease());
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
    this.api.resume(this.project.id, Number(nextStageIndex), singleStageOnly, this.project.environment)
      .toPromise()
      .then(result => {
        this.notification.info('Request has been accepted');
        this.pollForChanges();
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
    let update = false;
    update = update || (this.watchPaused = 0 === index);
    update = update || (this.watchHistory = 1 === index);
    update = update || (this.watchLogs = 3 === index);

    if (update) {
      this.pollWatched();
    }
  }

  isLongLoading() {
    return this.longLoading.isLongLoading();
  }

  openFolder(project: Project, entry: HistoryEntry) {
    this.tabs.selectedIndex = 2;
    this.files.updateAdditionalRoot(`${project.name};workspaces/${project.id}`, true);
    this.files.navigateDirectlyTo(`/workspaces/${project.id}/${entry.workspace}/`);
  }


  openLogs(project: Project, entry: HistoryEntry) {
    this.tabs.selectedIndex = 3;
    this.logs = null;
    this.watchLogs = true;
    this.watchLogsId = entry.stageId;
    this.watchLatestLogs = false;
  }

  showLatestLogs() {
    if (!this.watchLatestLogs) {
      this.logs = null;
      this.watchLogs = true;
      this.watchLogsId = null;
      this.watchLatestLogs = true;
      this.loadLogs();
    }
  }

  onOverwriteStageSelectionChanged(index: number) {
    this.longLoading.increase();
    this.api
      .getRequiredUserInput(this.project.id, index)
      .toPromise()
      .then(required => {
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
          });
      })
      .catch(err => this.notification.error('Failed to retrieve environment: ' + err))
      .finally(() => this.longLoading.decrease());
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
        this.project.environment.set(key, result);
      }
    });
  }
}
