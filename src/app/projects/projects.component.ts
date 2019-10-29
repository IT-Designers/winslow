import {Component, OnDestroy, OnInit, QueryList, ViewChildren} from '@angular/core';
import {ProjectsCreateDialog} from '../projects-create-dialog/projects-create-dialog.component';
import {MatDialog} from '@angular/material';
import {Project, ProjectApiService, StateInfo} from '../api/project-api.service';
import {ProjectViewComponent} from '../project-view/project-view.component';
import {NotificationService} from '../notification.service';
import {ActivatedRoute, Params} from '@angular/router';
import {Subscription} from 'rxjs';

@Component({
  selector: 'app-projects',
  templateUrl: './projects.component.html',
  styleUrls: ['./projects.component.css']
})
export class ProjectsComponent implements OnInit, OnDestroy {

  @ViewChildren(ProjectViewComponent) views!: QueryList<ProjectViewComponent>;

  projects: Project[] = null;
  stateInfo: Map<string, StateInfo> = null;
  loadError = null;
  interval;
  selectedProject: Project = null;

  queryParams: Params = null;
  queryParamsSubscription: Subscription = null;

  constructor(private api: ProjectApiService,
              private createDialog: MatDialog,
              private notification: NotificationService,
              private route: ActivatedRoute) {
    this.queryParamsSubscription = route.queryParams.subscribe(params => this.queryParams = params);
  }

  ngOnInit() {
    this.projects = null;
    this.api.listProjects()
      .then(projects => {
        this.projects = projects.sort((a, b) => a.name.localeCompare(b.name));
        setTimeout(() => this.pollAllProjectsForChanges(), 10);
      })
      .catch(error => this.loadError = error);
    this.interval = setInterval(() => this.pollAllProjectsForChanges(), 3000);
  }

  ngOnDestroy() {
    this.queryParamsSubscription.unsubscribe();
    this.queryParamsSubscription = null;
    this.projects = null;
    clearInterval(this.interval);
  }

  pollAllProjectsForChanges() {
    this.views.forEach(view => view.longLoading.increase());
    const projectIds = this.projects.map(project => project.id);
    if (projectIds != null && projectIds.length > 0) {
      this.api.getProjectStates(projectIds)
        .then(result => {
          let index = 0;
          this.views.forEach(view => {
            view.update(result[index++]);
          });
          this.stateInfo = new Map<string, StateInfo>();
          for (let i = 0; i < result.length; ++i) {
            this.stateInfo.set(projectIds[i], result[i]);
            this.views.forEach(view => {
              if (view.projectValue.id === projectIds[i]) {
                view.update(result[i]);
              }
            });
          }
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
      this.api.createProject(result.name, result.pipeline).then(r => {
        this.notification.info('Project created successfully');
        this.projects.push(r);
      });
    });
  }

  stopLoading(project: Project) {
    if (project != null) {
      this.views.forEach(view => {
        if (view.projectValue.id === project.id) {
          view.stopLoading();
        }
      });
    }
  }

  startLoading(project: Project) {
    if (project != null) {
      this.views.forEach(view => {
        if (view.projectValue.id === project.id) {
          const stateInfo = this.stateInfo.get(project.id);
          if (stateInfo != null) {
            view.update(stateInfo);
          }
          view.startLoading();
        }
      });
    }
  }
}
