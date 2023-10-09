import {Component, OnDestroy, OnInit, QueryList, ViewChildren} from '@angular/core';
import {CreateProjectData, ProjectsCreateDialog} from '../projects-create-dialog/projects-create-dialog.component';
import {MatLegacyDialog as MatDialog} from '@angular/material/legacy-dialog';
import {ProjectApiService, ProjectGroup} from '../api/project-api.service';
import {ProjectViewComponent} from '../project-view/project-view.component';
import {NotificationService} from '../notification.service';
import {DialogService} from '../dialog.service';
import {ActivatedRoute, Router} from '@angular/router';
import {Subscription} from 'rxjs';
import {
  ProjectDiskUsageDialogComponent,
  ProjectDiskUsageDialogData
} from '../project-disk-usage-dialog/project-disk-usage-dialog.component';
import {UserApiService} from '../api/user-api.service';
import {FilesApiService} from '../api/files-api.service';
import {GroupActionsComponent} from '../group-actions/group-actions.component';
import {LocalStorageService} from '../api/local-storage.service';
import {ProjectInfo, StateInfo} from '../api/winslow-api';

@Component({
  selector: 'app-projects',
  templateUrl: './projects.component.html',
  styleUrls: ['./projects.component.css']
})
export class ProjectsComponent implements OnInit, OnDestroy {

  @ViewChildren(ProjectViewComponent) views!: QueryList<ProjectViewComponent>;

  projects: ProjectInfo[] = [];
  projectsFiltered: ProjectInfo[] = null;
  projectsGroups: ProjectGroup[] = [];
  stateInfo: Map<string, StateInfo> = null;
  interval;
  selectedProject: ProjectInfo = null;
  selectedProjectId: string = null;

  paramsSubscription: Subscription = null;
  projectSubscription: Subscription = null;
  projectStateSubscription: Subscription = null;
  effects: Effects = null;
  groupsOnTop: boolean;
  GROUPS_ON_TOP_SETTING = 'GROUPS_ON_TOP';
  context: string;
  SELECTED_CONTEXT = 'SELECTED_CONTEXT';

  constructor(readonly api: ProjectApiService,
              readonly users: UserApiService,
              private createDialog: MatDialog,
              private notification: NotificationService,
              private dialog: DialogService,
              public route: ActivatedRoute,
              public router: Router,
              private localStorageService: LocalStorageService) {
  }

  ngOnInit() {
    this.groupsOnTop = this.localStorageService.getSettings(this.GROUPS_ON_TOP_SETTING);
    this.context = this.localStorageService.getSettings(this.SELECTED_CONTEXT);
    this.createEffects();

    this.projectSubscription = this.createProjectSubscription();
    this.projectStateSubscription = this.createProjectStateSubscription();

    this.paramsSubscription = this.route.params.subscribe(params => {
      this.selectedProjectId = params.id;
      this.updateSelectedProject();
    });
  }

  private createEffects() {
    try {
      this.effects = new Effects(this.users);
    } catch (e) {
      // ignore all errors
    }
  }

  private createProjectSubscription() {
    return this.api.getProjectSubscriptionHandler().subscribe((id, value) => {
      const projects = this.projects == null ? [] : [...this.projects];
      const index = projects.findIndex(project => project.id === id);
      if (index >= 0) {
        projects[index] = value;
      } else {
        projects.push(value);
        projects.sort((a, b) => a.name.localeCompare(b.name));
      }
      this.projects = projects;
      if (this.selectedProjectId === id) {
        this.selectedProject = value;
      }
    });
  }

  private createProjectStateSubscription() {
    return this.api.getProjectStateSubscriptionHandler().subscribe((id, value) => {
      const stateInfo = this.stateInfo == null ? new Map() : new Map(this.stateInfo);
      if (stateInfo.has(id) && value == null) {
        stateInfo.delete(id);
      } else {
        stateInfo.set(id, value);
        if (this.selectedProject?.id === id && this.effects != null) {
          this.effects.update(value);
        }
      }
      this.stateInfo = stateInfo;
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
    if (this.projectSubscription) {
      this.projectSubscription.unsubscribe();
      this.projectSubscription = null;
    }
    if (this.projectStateSubscription) {
      this.projectStateSubscription.unsubscribe();
      this.projectStateSubscription = null;
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
              this.projectsFiltered.push(r);
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
    console.log('--------------- Deleting project: ' + project.name);
    console.dir(this.projects);
    console.dir(this.projectsFiltered);
    for (let i = 0; i < this.projects.length; ++i) {
      if (this.projects[i].id === project.id) {
        this.projects.splice(i, 1);
        this.projectsFiltered.splice(i, 1);
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

  openGroupActions(name: string) {
    this.createDialog
      .open(GroupActionsComponent, {
        data: {tag: name}
      });
  }

  changeContext($event: string) {
    let newContext: string;
    newContext = $event;
    this.context = newContext;
  }
}


class Effects {
  audio: HTMLAudioElement;
  prev: StateInfo;
  username = '';


  constructor(users: UserApiService) {
    try {
      users.getSelfUserName().then(name => {
        this.username = name;
      });
    } catch (e) {
      // ignore all errors
    }
  }

  update(state: StateInfo) {
    try {
      if (state.state === 'RUNNING' && (this.prev == null || this.prev.state !== 'RUNNING')) {
        if (this.audio != null) {
          this.audio.pause();
        }
        this.audio = new Audio(FilesApiService.getUrl(`resources/winslow-ui/${this.username}/effects/running.mp3`));
        this.audio.loop = true;
        this.audio.play();
      } else if (this.prev != null && this.prev.state !== 'FAILED' && state.state === 'FAILED') {
        if (this.audio != null) {
          this.audio.pause();
        }
        this.audio = new Audio(FilesApiService.getUrl(`resources/winslow-ui/${this.username}/effects/failed.mp3`));
        this.audio.loop = false;
        this.audio.play();
      } else if (this.prev != null && this.prev.state === 'RUNNING' && state.state !== 'RUNNING') {
        if (this.audio != null) {
          this.audio.pause();
        }
        this.audio = new Audio(FilesApiService.getUrl(`resources/winslow-ui/${this.username}/effects/completed.mp3`));
        this.audio.loop = false;
        this.audio.play();
      }
    } catch (e) {
      // ignore all errors
    } finally {
      this.prev = state;
    }
  }
}
