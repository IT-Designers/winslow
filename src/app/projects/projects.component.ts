import {Component, OnDestroy, OnInit} from '@angular/core';
import {ProjectsCreateDialog} from '../projects-create/projects-create-dialog.component';
import {MatDialog, MatSnackBar} from '@angular/material';
import {Project, ProjectApiService, State} from '../project-api.service';

@Component({
  selector: 'app-projects',
  templateUrl: './projects.component.html',
  styleUrls: ['./projects.component.css']
})
export class ProjectsComponent implements OnInit, OnDestroy {
  projects: Project[];
  interval;

  constructor(private api: ProjectApiService, private createDialog: MatDialog, private snack: MatSnackBar) {
  }

  ngOnInit() {
    this.projects = null;
    this.api.listProjects().toPromise().then(projects => {
      this.projects = projects;
      this.intervalUpdateProjects();
    });
    this.interval = setInterval(() => this.intervalUpdateProjects(), 2000);
  }

  intervalUpdateProjects() {
    if (this.projects != null) {
      this.projects.forEach(project => {
        this.api.getProjectState(project.id).toPromise().then(state => {
          project.state = state;
          if (project.history != null) {
            this.loadHistory(project, true);
          }
        });
      });
    }
  }

  ngOnDestroy() {
    this.projects = null;
    clearInterval(this.interval);
  }


  create() {
    this.createDialog.open(ProjectsCreateDialog, {
      width: '50%',
      data: {}
    }).afterClosed().subscribe(result => {
      console.log(JSON.stringify(result));
      this.api.createProject(result.name, result.pipeline).toPromise().then(result => {
        this.snack.open(JSON.stringify(result), 'Great!');
      });
    });
  }

  maybeLoadHistory(project: Project, load: boolean, resetHistoryBeforeLoading = true) {
    if (load) {
      if (resetHistoryBeforeLoading) {
        project.history = null;
      }
      this.loadHistory(project);
    } else {
      project.history = null;
    }
  }

  loadHistory(project: Project, onlyIfNotNull = false) {
    this.api.getProjectHistory(project.id).toPromise().then(history => {
      if (!onlyIfNotNull || project.history == null) {
        project.history = history.reverse();
      }
    });
  }

  toDate(time: number) {
    return new Date(time).toLocaleString();
  }

  setNextStage(project: Project, value: string) {
    this.api.setProjectNextStage(project.id, Number(value)).toPromise().then(result => {
      this.snack.open('Request has been accepted', 'OK');
      this.intervalUpdateProjects();
    }).catch(error => {
      this.snack.open('Request failed: ' + error);
    });
  }
}
