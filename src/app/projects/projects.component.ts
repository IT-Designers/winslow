import {AfterViewInit, Component, OnDestroy, OnInit, QueryList, ViewChildren} from '@angular/core';
import {ProjectsCreateDialog} from '../projects-create-dialog/projects-create-dialog.component';
import {MatDialog, MatSnackBar} from '@angular/material';
import {Project, ProjectApiService} from '../api/project-api.service';
import {ProjectViewComponent} from '../project-view/project-view.component';
import {NotificationService} from '../notification.service';

@Component({
  selector: 'app-projects',
  templateUrl: './projects.component.html',
  styleUrls: ['./projects.component.css']
})
export class ProjectsComponent implements OnInit, OnDestroy {

  @ViewChildren(ProjectViewComponent) views!: QueryList<ProjectViewComponent>;

  projects: Project[];
  interval;

  constructor(private api: ProjectApiService, private createDialog: MatDialog, private notification: NotificationService) {
  }

  ngOnInit() {
    this.projects = null;
    this.api.listProjects().toPromise().then(projects => {
      this.projects = projects;
      setTimeout(() => this.pollAllProjectsForChanges(), 10);
    });
    this.interval = setInterval(() => this.pollAllProjectsForChanges(), 3000);
  }

  ngOnDestroy() {
    this.projects = null;
    clearInterval(this.interval);
  }

  pollAllProjectsForChanges() {
    const projectIds = this.views.map(view => {
      view.longLoading.increase();
      return view.project.id;
    });
    if (projectIds != null && projectIds.length > 0) {
      this.api.getProjectStates(projectIds).toPromise()
        .then(result => {
          let index = 0;
          this.views.forEach(view => {
            view.update(result[index++]);
          });
        })
        .finally(() => {
          this.views.forEach(view => view.longLoading.decrease());
        });
    }
  }

  createNewProject() {
    this.createDialog.open(ProjectsCreateDialog, {
      width: '50%',
      data: {}
    }).afterClosed().subscribe(result => {
      console.log(JSON.stringify(result));
      this.api.createProject(result.name, result.pipeline).toPromise().then(r => {
        this.notification.info('Project created successfully');
        this.projects.push(r);
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
