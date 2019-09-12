import {Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {ProjectsCreateDialog} from '../projects-create/projects-create-dialog.component';
import {MatDialog, MatSnackBar, MatTabChangeEvent, MatTabGroup} from '@angular/material';
import {HistoryEntry, Project, ProjectApiService, State} from '../project-api.service';

@Component({
  selector: 'app-projects',
  templateUrl: './projects.component.html',
  styleUrls: ['./projects.component.css']
})
export class ProjectsComponent implements OnInit, OnDestroy {
  @ViewChild('tabs') tabs: MatTabGroup;
  projects: View[];
  interval;

  constructor(private api: ProjectApiService, private createDialog: MatDialog, private snack: MatSnackBar) {
  }

  ngOnInit() {
    this.projects = null;
    this.api.listProjects().toPromise().then(projects => {
      this.projects = projects.map(p => new View(p));
      this.puseAllProjects();
    });
    this.interval = setInterval(() => this.puseAllProjects(), 2000);
  }

  ngOnDestroy() {
    this.projects = null;
    clearInterval(this.interval);
  }

  puseAllProjects() {
    if (this.projects != null) {
      this.projects.forEach(view => {
        this.api.getProjectState(view.project.id).toPromise().then(state => {
          view.state = state;
          this.updateProject(view);
        });
      });
    }
  }

  private updateProject(view: View) {
    if (view.watchHistory) {
      this.loadHistory(view);
    }
    if (view.watchPaused) {
      this.loadPaused(view);
    }
  }

  createNewProject() {
    this.createDialog.open(ProjectsCreateDialog, {
      width: '50%',
      data: {}
    }).afterClosed().subscribe(result => {
      console.log(JSON.stringify(result));
      this.api.createProject(result.name, result.pipeline).toPromise().then(r => {
        this.snack.open('Project created successfully', 'Great!');
      });
    });
  }

  loadHistory(view: View) {
    return this.api.getProjectHistory(view.project.id).toPromise().then(history => view.history = history.reverse());
  }

  loadPaused(view: View) {
    return this.api.getProjectPaused(view.project.id).then(paused => view.paused = paused);
  }

  toDate(time: number) {
    return new Date(time).toLocaleString();
  }

  setNextStage(project: Project, nextStageIndex: string) {
    this.api.setProjectNextStage(project.id, Number(nextStageIndex)).toPromise().then(result => {
      this.snack.open('Request has been accepted', 'OK');
      this.puseAllProjects();
    }).catch(error => {
      this.snack.open('Request failed: ' + error);
    });
  }

  updateRequestPause(project: Project, checked: boolean) {
    const before = project.paused;
    project.paused = checked;
    this.api.setProjectPaused(project.id, checked)
      .toPromise()
      .then(result => {
        this.snack.open('Project updated', 'OK', {
          duration: 3000
        });
      })
      .catch(err => {
        project.paused = before;
        this.snack.open('Request failed: ' + JSON.stringify(err), 'OK');
      });
  }

  focusChange($event: MatTabChangeEvent, view: View) {
    const label = $event.tab.textLabel.toLowerCase();
    this.loadForTab(view, label);
  }

  startLoading(view: View) {
    this.loadForTab(view, null);
  }

  stopLoading(view: View) {
    this.loadForTab(view, null);
  }

  loadForTab(view: View, label: string) {
    this.snack.open('loadForTab ' + label, 'OK', {duration: 1000});
    view.watchPaused = 'control' === label || null === label;
    view.watchHistory = 'history' === label;
    this.updateProject(view);
  }
}

class View {
  project: Project;
  state?: State;
  history?: HistoryEntry[];
  paused = false;
  watchPaused = false;
  watchHistory = false;

  constructor(project: Project) {
    this.project = project;
  }
}
