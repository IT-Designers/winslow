import {AfterViewInit, Component, OnDestroy, OnInit, QueryList, ViewChildren} from '@angular/core';
import {ProjectsCreateDialog} from '../projects-create/projects-create-dialog.component';
import {MatDialog, MatSnackBar} from '@angular/material';
import {Project, ProjectApiService} from '../project-api.service';
import {ProjectViewComponent} from '../project-view/project-view.component';

@Component({
  selector: 'app-projects',
  templateUrl: './projects.component.html',
  styleUrls: ['./projects.component.css']
})
export class ProjectsComponent implements OnInit, OnDestroy {

  @ViewChildren(ProjectViewComponent) views!: QueryList<ProjectViewComponent>;

  projects: Project[];
  interval;

  constructor(private api: ProjectApiService, private createDialog: MatDialog, private snack: MatSnackBar) {
  }

  ngOnInit() {
    this.projects = null;
    this.api.listProjects().toPromise().then(projects => {
      this.projects = projects;
      this.pollAllProjectsForChanges();
    });
    this.interval = setInterval(() => this.pollAllProjectsForChanges(), 2000);
  }

  ngOnDestroy() {
    this.projects = null;
    clearInterval(this.interval);
  }

  pollAllProjectsForChanges() {
    this.views.forEach(view => {
      view.pollForChanges();
    });
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
  stopLoading(project: Project) {
    this.views.forEach(view => {
      if (view.project.id === project.id) {
        view.stopLoading();
      }
    });
  }

  startLoading(project: Project) {
    this.views.forEach(view => {
      if (view.project.id === project.id) {
        view.startLoading();
      }
    });
  }
}
