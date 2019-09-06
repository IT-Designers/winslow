import {Component, OnDestroy, OnInit} from '@angular/core';
import {ProjectsCreateDialog} from '../projects-create/projects-create-dialog.component';
import {MatDialog, MatSnackBar} from '@angular/material';
import {Project, ProjectApiService} from '../project-api.service';

@Component({
  selector: 'app-projects',
  templateUrl: './projects.component.html',
  styleUrls: ['./projects.component.css']
})
export class ProjectsComponent implements OnInit, OnDestroy {
  projects: Project[];

  constructor(private api: ProjectApiService, private createDialog: MatDialog, private snack: MatSnackBar) { }

  ngOnInit() {
    this.projects = null;
    this.api.listProjects().toPromise().then(projects => this.projects = projects);
  }

  ngOnDestroy() {
    this.projects = null;
  }


  create() {
    this.createDialog.open(ProjectsCreateDialog, {
      width: '50%',
      data: {  }
    }).afterClosed().subscribe(result => {
      console.log(JSON.stringify(result));
      this.api.createProject(result.name, result.pipeline).toPromise().then(result => {
        this.snack.open(JSON.stringify(result), 'Great!');
      });
    });
  }

  loadHistory(project: Project, load: boolean) {
    if (load) {
      project.history = null;
      this.api.getProjectHistory(project.id).toPromise().then(history => project.history = history.reverse());
    }
  }

  toDate(time: number) {
    return new Date(time).toLocaleString();
  }
}
