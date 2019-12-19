import {Component, OnDestroy, OnInit, QueryList, ViewChildren} from '@angular/core';
import {CreateProjectData, ProjectsCreateDialog} from '../projects-create-dialog/projects-create-dialog.component';
import {MatDialog} from '@angular/material';
import {ProjectApiService, ProjectInfo, StateInfo} from '../api/project-api.service';
import {ProjectViewComponent} from '../project-view/project-view.component';
import {NotificationService} from '../notification.service';
import {DialogService} from '../dialog.service';
import {ActivatedRoute, Params, Router} from '@angular/router';
import {Subscription} from 'rxjs';
import {
  ProjectDiskUsageDialogComponent,
  ProjectDiskUsageDialogData
} from '../project-disk-usage-dialog/project-disk-usage-dialog.component';

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

  paramsSubscription: Subscription = null;

  constructor(readonly api: ProjectApiService,
              private createDialog: MatDialog,
              private notification: NotificationService,
              private dialog: DialogService,
              private route: ActivatedRoute,
              private router: Router) {
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
    this.paramsSubscription = this.route.params.subscribe(params => {
      this.selectedProjectId = params.id;
      this.updateSelectedProject();
    });
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
    if (this.paramsSubscription) {
      this.paramsSubscription.unsubscribe();
      this.paramsSubscription = null;
    }
  }

  pollAllProjectsForChanges() {
    this.views.forEach(view => view.longLoading.increase());
    if (this.projectsFiltered != null && this.projectsFiltered.length > 0) {
      const projectIds = this.projectsFiltered.map(project => project.id);
      this.api.getProjectStates(projectIds)
        .then(result => {
          this.stateInfo = new Map<string, StateInfo>();
          for (let i = 0; i < result.length; ++i) {
            this.stateInfo.set(projectIds[i], result[i]);
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

  openProjectDiskUsageDialog() {
    this.createDialog
      .open(ProjectDiskUsageDialogComponent, {
        data: {
          projects: this.projects,
        } as ProjectDiskUsageDialogData
      });
  }
}
