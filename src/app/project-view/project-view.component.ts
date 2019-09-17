import {AfterViewInit, Component, EventEmitter, Input, OnDestroy, OnInit, Output, ViewChild} from '@angular/core';
import {HistoryEntry, LogEntry, Project, ProjectApiService, State} from '../project-api.service';
import {NotificationService} from '../notification.service';
import {MatSlideToggleChange, MatTabGroup} from '@angular/material';
import {LongLoadingDetector} from '../long-loading-detector';
import {FilesComponent} from '../files/files.component';

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

  watchHistory = false;
  watchPaused = false;
  watchLogs = false;
  watchLatestLogs = true;

  watchLogsId?: string = null;

  longLoading = new LongLoadingDetector();

  constructor(private api: ProjectApiService, private notification: NotificationService) {
  }

  ngOnInit(): void {
    this.pollForChanges();
  }

  ngOnDestroy(): void {
  }

  pollForChanges(): void {
    this.longLoading.increase();
    this.api.getProjectState(this.project.id).toPromise()
      .finally(() => this.longLoading.decrease())
      .then(state => {
        this.stateEmitter.emit(this.state = state);
        this.pollWatched();
      })
      .catch(err => this.notification.error('Failed to update state' + JSON.stringify(err)));
  }

  isRunning(): boolean {
    return State.Running === this.state;
  }

  pollWatched(): void {
    if (this.watchHistory && (this.isRunning() || this.history == null)) {
      this.loadHistory();
    }
    if (this.watchPaused) {
      this.loadPaused();
    }
    if (this.watchLogs && (this.isRunning() || this.logs == null)) {
      this.loadLogs();
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
    this.api.setProjectNextStage(this.project.id, Number(nextStageIndex), singleStageOnly).toPromise().then(result => {
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
}
