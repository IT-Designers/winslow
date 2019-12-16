import {Component, OnDestroy, OnInit, QueryList, ViewChildren} from '@angular/core';
import {CreateProjectData, ProjectsCreateDialog} from '../projects-create-dialog/projects-create-dialog.component';
import {MatDialog} from '@angular/material';
import {ProjectApiService, ProjectInfo, StateInfo} from '../api/project-api.service';
import {ProjectViewComponent} from '../project-view/project-view.component';
import {NotificationService} from '../notification.service';
import {DialogService} from '../dialog.service';
import {ActivatedRoute, Params, Router} from '@angular/router';

@Component({
  selector: 'app-projects',
  templateUrl: './projects.component.html',
  styleUrls: ['./projects.component.css']
})
export class ProjectsComponent implements OnInit, OnDestroy {

  @ViewChildren(ProjectViewComponent) views!: QueryList<ProjectViewComponent>;

  projects: ProjectInfo[] = null;
  projectsFiltered: ProjectInfo[] = null;
  stateInfo: Map<string, StateInfo> = null;
  loadError = null;
  interval;
  selectedProject: ProjectInfo = null;
  selectedProjectId: string = null;

  constructor(readonly api: ProjectApiService,
              private createDialog: MatDialog,
              private notification: NotificationService,
              private dialog: DialogService,
              private route: ActivatedRoute,
              private router: Router) {
    route.params.subscribe(params => {
      this.selectedProjectId = params.id;
      this.updateSelectedProject();
    });
  }

  ngOnInit() {
    this.projects = null;
    this.api.listProjects()
      .then(projects => {
        this.projects = projects.sort((a, b) => a.name.localeCompare(b.name));
        this.updateSelectedProject();
        setTimeout(() => this.pollAllProjectsForChanges(), 10);
      })
      .catch(error => this.loadError = error);
    this.interval = setInterval(() => this.pollAllProjectsForChanges(), 3000);
  }

  private updateSelectedProject() {
    if (this.projects && this.selectedProjectId) {
      for (const p of this.projects) {
        if (p.id === this.selectedProjectId) {
          this.selectedProject = p;
          break;
        }
      }
    }
  }

  ngOnDestroy() {
    this.projects = null;
    clearInterval(this.interval);
  }

  pollAllProjectsForChanges() {
    this.views.forEach(view => view.longLoading.increase());
    const projectIds = this.projectsFiltered.map(project => project.id);
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
              if (view.project.id === projectIds[i]) {
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
    this.createDialog
      .open(ProjectsCreateDialog, {
        data: {
          tags: []
        } as CreateProjectData
      })
      .afterClosed()
      .subscribe(result => {
        if (result) {
          return this.dialog.openLoadingIndicator(
            this.api.createProject(result.name, result.pipeline, result.tags).then(r => {
              this.projects.push(r);
              this.selectedProject = r;
            }),
            `Creating new Project`
          );
        }
      });
  }

  stopLoading(project: ProjectInfo) {
    if (project != null) {
      this.views.forEach(view => {
        if (view.project.id === project.id) {
          view.stopLoading();
        }
      });
    }
  }

  startLoading(project: ProjectInfo) {
    if (project != null) {
      this.views.forEach(view => {
        if (view.project.id === project.id) {
          const stateInfo = this.stateInfo.get(project.id);
          if (stateInfo != null) {
            view.update(stateInfo);
          }
          view.startLoading();
        }
      });
    }
  }

  onDeleted(project: ProjectInfo) {
    for (let i = 0; i < this.projects.length; ++i) {
      if (this.projects[i].id === project.id) {
        this.projects.splice(i, 1);
        this.projects = this.projects.sort();
        if (this.selectedProject != null && this.selectedProject.id === project.id) {
          if (this.projects.length > 0) {
            if (i >= this.projects.length) {
              i -= 1;
            }
            this.selectedProject = this.projects[i];
          } else {
            this.selectedProject = null;
          }
        }
        break;
      }
    }
  }

  selectProject(project: ProjectInfo) {
    this.router.navigate([project.id], {
      relativeTo: this.route.parent
    });
  }
}
