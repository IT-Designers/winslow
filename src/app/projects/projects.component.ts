import {Component, OnDestroy, OnInit} from '@angular/core';
import {
  CreateProjectData,
  CreateProjectPipelineOption,
  ProjectsCreateDialog
} from '../projects-create-dialog/projects-create-dialog.component';
import {MatDialog} from '@angular/material/dialog';
import {ProjectApiService, ProjectGroup} from '../api/project-api.service';
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
import {PipelineApiService} from "../api/pipeline-api.service";

@Component({
  selector: 'app-projects',
  templateUrl: './projects.component.html',
  styleUrls: ['./projects.component.css']
})
export class ProjectsComponent implements OnInit, OnDestroy {

  projects: ProjectInfo[] = [];
  projectsFiltered: ProjectInfo[] | null = null;
  projectsGroups: ProjectGroup[] = [];
  stateInfo: Map<string, StateInfo> = new Map<string, StateInfo>();
  selectedProject: ProjectInfo | null = null;
  selectedProjectId: string | null = null;

  paramsSubscription?: Subscription;
  projectSubscription?: Subscription;
  projectStateSubscription?: Subscription;
  effects: Effects | null = null;
  groupsOnTop: boolean | null;
  context: string | null;


  constructor(
    readonly projectApi: ProjectApiService,
    readonly pipelineApi: PipelineApiService,
    readonly users: UserApiService,
    private createDialog: MatDialog,
    private dialog: DialogService,
    public route: ActivatedRoute,
    public router: Router,
    private localStorageService: LocalStorageService
  ) {
    this.groupsOnTop = this.localStorageService.getGroupsOnTop();
    this.context = this.localStorageService.getSelectedContext();
  }

  ngOnInit() {
    this.createEffects();

    this.projectSubscription = this.createProjectSubscription();
    this.projectStateSubscription = this.createProjectStateSubscription();

    this.paramsSubscription = this.route.params.subscribe(params => {
      this.selectedProjectId = params.id;
      this.updateSelectedProject();
    });
  }

  private refreshProjects(): void {
    // Replace projects array with a new object in order to get the filteredProjects to update.
    // A different implementation for filtering projects might be useful for avoiding this.
    this.projects = [...this.projects.sort((a, b) => a.name.localeCompare(b.name))];
  }

  private addOrUpdateProject(project: ProjectInfo) {
    const index = this.projects.findIndex(preexistingProject => preexistingProject.id == project.id);
    if (index === -1) {
      this.projects.push(project);
    } else {
      this.projects[index] = project;
    }
    this.refreshProjects();
  }

  private createEffects() {
    try {
      this.effects = new Effects(this.users);
    } catch (e) {
      // ignore all errors
    }
  }

  private createProjectSubscription() {
    return this.projectApi.getProjectSubscriptionHandler().subscribe((id, value) => {
      this.addOrUpdateProject(value);

      if (this.selectedProjectId === id) {
        this.selectedProject = value;
      }
    });
  }

  private createProjectStateSubscription() {
    return this.projectApi.getProjectStateSubscriptionHandler().subscribe((id, value) => {
      const stateInfo = new Map(this.stateInfo);
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
    this.paramsSubscription?.unsubscribe();
    this.projectSubscription?.unsubscribe();
    this.projectStateSubscription?.unsubscribe();
  }

  openCreateProjectDialog() {
    this.createDialog
      .open(ProjectsCreateDialog, {
        data: {
          pipelineOption: CreateProjectPipelineOption.UseShared,
          tags: [],
        }
      })
      .afterClosed()
      .subscribe((result: CreateProjectData) => {
        if (result) {
          this.dialog.openLoadingIndicator(this.createProject(result), `Creating new Project`);
        }
      });
  }

  private async createProject(dialogData: CreateProjectData) {
    const pipelineName = `${dialogData.name}`;
    let pipelineToUse: string;

    if (dialogData.pipelineOption == CreateProjectPipelineOption.UseShared) {
      pipelineToUse = dialogData.pipelineId;
    } else {
      pipelineToUse = (await this.pipelineApi.createPipelineDefinition(pipelineName)).id;
    }

    const project = await this.projectApi.createProject(dialogData.name, pipelineToUse, dialogData.tags);

    if (dialogData.pipelineOption == CreateProjectPipelineOption.CreateLocal) {
      // take ownership of new exclusive pipeline
      const definition = await this.pipelineApi.getPipelineDefinition(pipelineToUse);
      definition.belongsToProject = project.id;
      await this.pipelineApi.setPipelineDefinition(definition);
    }

    // project does not need to be added, as it will be validated on the backend and then added via subscription
    this.selectProject(project);
  }

  onDeleted(id: string) {
    const index = this.projects.findIndex(project => project.id == id);

    if (index === -1) {
      console.error(`Could not remove project ${id} from project list as no such project exists.`);
      return;
    }

    this.projects.splice(index, 1);
    this.refreshProjects();

    if (this.selectedProject != null && this.selectedProject.id === id) {
      this.selectedProject = null;
    }

  }

  selectProject(project: ProjectInfo) {
    this.router.navigate([project.id], {
      relativeTo: this.route.parent
    }).then();
  }

  openProjectDiskUsageDialog() {
    this.createDialog
      .open(ProjectDiskUsageDialogComponent, {
        data: {
          projects: this.projects,
        } as ProjectDiskUsageDialogData
      });
  }

  openGroupActions() : void{
    const name = this.context;
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
  audio?: HTMLAudioElement;
  prev?: StateInfo;
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
