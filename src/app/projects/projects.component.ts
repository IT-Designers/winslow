import {Component, OnInit} from '@angular/core';
import {ProjectsCreateDialog} from '../projects-create/projects-create-dialog.component';
import {MatDialog, MatSnackBar} from '@angular/material';
import {ApiService, Project} from '../api.service';

@Component({
  selector: 'app-projects',
  templateUrl: './projects.component.html',
  styleUrls: ['./projects.component.css']
})
export class ProjectsComponent implements OnInit {
  projects: Project[];

  constructor(private api: ApiService, private createDialog: MatDialog, private snack: MatSnackBar) { }

  ngOnInit() {
    this.api.listProjects().toPromise().then(projects => this.projects = projects);
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
}
