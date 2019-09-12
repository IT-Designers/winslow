import {AfterViewInit, Component, EventEmitter, Input, OnDestroy, OnInit, Output, ViewChild} from '@angular/core';
import {HistoryEntry, Project, ProjectApiService, State} from '../project-api.service';
import {NotificationService} from '../notification.service';
import {MatTabGroup} from '@angular/material';
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

  @Input() project: Project;
  @Output('state') stateEmitter = new EventEmitter<State>();

  state?: State = null;
  history?: HistoryEntry[] = null;
  paused: boolean = null;

  watchHistory = false;
  watchPaused = false;

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
    this.api.getProjectState(this.project.id).toPromise().then(state => {
      this.longLoading.decrease();
      this.stateEmitter.emit(this.state = state);
      this.pollWatched();
    });
  }

  pollWatched(): void {
    if (this.watchHistory) {
      this.loadHistory();
    }
    if (this.watchPaused) {
      this.loadPaused();
    }
  }

  loadHistory(): void {
    this.longLoading.increase();
    this.api.getProjectHistory(this.project.id)
      .toPromise()
      .then(history => this.history = history.reverse())
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

  setNextStage(nextStageIndex: any) {
    this.longLoading.increase();
    this.api.setProjectNextStage(this.project.id, Number(nextStageIndex)).toPromise().then(result => {
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
}
