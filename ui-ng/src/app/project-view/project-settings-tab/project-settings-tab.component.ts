import {Component, EventEmitter, Input, Output} from '@angular/core';
import {
  AuthTokenInfo,
  Link,
  PipelineDefinitionInfo,
  ProjectInfo,
  ResourceLimitation,
  WorkspaceMode
} from "../../api/winslow-api";
import {GroupApiService} from "../../api/group-api.service";
import {UserApiService} from "../../api/user-api.service";
import {DeletionPolicy, ProjectApiService} from "../../api/project-api.service";
import {PipelineApiService} from "../../api/pipeline-api.service";
import {DialogService} from "../../dialog.service";

@Component({
  selector: 'app-project-settings-tab',
  templateUrl: './project-settings-tab.component.html',
  styleUrls: ['./project-settings-tab.component.css']
})
export class ProjectSettingsTabComponent {

  @Output() projectChanged = new EventEmitter<ProjectInfo>();
  @Output() projectDeleted = new EventEmitter<string>();

  @Input() set project(projectInfo: ProjectInfo) {
    if (projectInfo == null) {
      return;
    }

    this._project = projectInfo;

    this.projectApi.getDeletionPolicy(this.project.id)
      .then(policy => {
        this.deletionPolicyLocal = policy;
        this.deletionPolicyRemote = policy;
      });

    this.projectApi.getAuthTokens(this.project.id)
      .then(tokens => {
        this.authTokens = tokens;
      });


    this.projectApi.getWorkspaceConfigurationMode(this.project.id)
      .then(mode => {
        this.workspaceMode = mode;
      });


    this.projectApi.getResourceLimitation(this.project.id)
      .then(limit => {
        this.resourceLimit = limit;
      });

    this.userApi.getSelfUserName()
      .then(name => this.userApi.hasSuperPrivileges(name))
      .then(isAdmin => {
        if (isAdmin) {
          this.userCanEditProject = true;
        }
      })

    this.groupApi.getGroups()
      .then(userGroups => this.project.groups.forEach(projectGroup => {
        if (projectGroup.role !== 'OWNER' && userGroups.some(userGroup => userGroup.name == projectGroup.name)) {
          this.userCanEditProject = true;
        }
      }))

    this.pipelineApi.getPipelineDefinitions()
      .then(usablePipelines => this.pipelines = usablePipelines.filter(
        pipeline => pipeline.belongsToProject == null || pipeline.belongsToProject == this.project.id)
      )
  };

  get project() {
    return this._project;
  }

  private _project: ProjectInfo | null = null;

  userCanEditProject: boolean = false;
  cachedTags: string[] = [];
  pipelines: PipelineDefinitionInfo[] = [];
  showGroupList: boolean = false;
  authTokens: AuthTokenInfo[] = [];
  deletionPolicyLocal: DeletionPolicy | null = null;
  deletionPolicyRemote: DeletionPolicy | null = null;
  resourceLimit: ResourceLimitation | null = null;
  workspaceMode: WorkspaceMode | null = null;

  constructor(
    private pipelineApi: PipelineApiService,
    private projectApi: ProjectApiService,
    private groupApi: GroupApiService,
    private userApi: UserApiService,
    private dialog: DialogService,
  ) {
    this.cachedTags = projectApi.cachedTags;
    this.userCanEditProject = false;
  }

  setName(name: string): void {
    this.dialog.openLoadingIndicator(
      this.projectApi
        .setName(this.project.id, name)
        .then(() => this.project.name = name),
      `Updating name to ${name}`
    );
  }

  setPipeline(pipelineId: string): void {
    this.dialog.openLoadingIndicator(
      this.projectApi
        .setPipelineDefinition(this.project.id, pipelineId)
        .then(pipeline => this.project.pipelineDefinition = pipeline),
      `Submitting pipeline selection`
    );
  }

  setTags(tags: string[]): void {
    return this.dialog.openLoadingIndicator(
      this.projectApi
        .setTags(this.project.id, tags)
        .then(tagsReceived => this.project.tags = tagsReceived),
      'Updating tags'
    );
  }

  setPublicAccess(value: boolean): void {
    this.dialog.openLoadingIndicator(
      this.projectApi.updatePublicAccess(this.project.id, value)
        .then(valueReceived => this.project.publicAccess = valueReceived),
      `Updating public access property`
    );
  }

  // todo figure out what's up with the "// weird"
  createAuthToken(name: string): void {
    this.dialog.openLoadingIndicator(
      this.projectApi.createAuthToken(this.project.id, name)
        .then(authTokenInfo => {
          this.authTokens.push(authTokenInfo);
          // weird
          setTimeout(
            () => this.dialog.info(authTokenInfo.secret, 'The secret value is'),
            100
          );
        }),
      `Creating a new authentication token`
    );
  }

  deleteAuthToken(id: string) {
    this.dialog.openLoadingIndicator(
      this.projectApi.deleteAuthToken(this.project.id, id)
        .then(() => {
          const index = this.authTokens.findIndex(authToken => authToken.id == id);
          this.authTokens.splice(index, 1);
        }),
      `Deleting authentication token`
    );
  }

  workspaceModes(): WorkspaceMode[] {
    return ['STANDALONE', 'INCREMENTAL', 'CONTINUATION'];
  }

  setWorkspaceMode(value: WorkspaceMode) {
    this.dialog.openLoadingIndicator(
      this.projectApi.setWorkspaceConfigurationMode(this.project.id, value)
        .then(mode => {
          this.workspaceMode = mode;
        }),
      `Updating workspace configuration mode`,
    );
  }

  updateDeletionPolicy(set: boolean, limitStr: string, keep: boolean, always: boolean): void {
    let promise: Promise<void>;
    if (set) {
      const policy =  new DeletionPolicy({
        numberOfWorkspacesOfSucceededStagesToKeep: Number(limitStr) > 0 ? Number(limitStr) : undefined,
        keepWorkspaceOfFailedStage: keep,
        alwaysKeepMostRecentWorkspace: always,
      });
      promise = this.projectApi.updateDeletionPolicy(this.project.id, policy)
        .then(result => {
          this.deletionPolicyLocal = result;
          this.deletionPolicyRemote = result;
        });
    } else {
      promise = this.projectApi.resetDeletionPolicy(this.project.id)
        .then(() => {
          this.deletionPolicyLocal = null;
          this.deletionPolicyRemote = null;
        });
    }
    this.dialog.openLoadingIndicator(
      promise,
      'Updating Deletion Policy'
    );
  }

  // ???
  maybeResetDeletionPolicy(reset: boolean) {
    const reApplyCurrentState = () => {
      const before = JSON.parse(JSON.stringify(this.deletionPolicyLocal));
      this.deletionPolicyLocal = null;
      if (before != null) {
        setTimeout(() => {
          this.deletionPolicyLocal = before;
        });
      }
    };
    if (reset) {
      this.deletionPolicyLocal = null;
    } else {
      if (this.deletionPolicyLocal === null) {
        this.dialog.openLoadingIndicator(
          this.projectApi.getDefaultDeletionPolicy(this.project.id)
            .then(result => this.deletionPolicyLocal = result)
            .catch(e => {
              reApplyCurrentState();
              return Promise.reject(e);
            }),
          'Loading default policy',
          false
        );
      }
    }
  }

  setResourceLimitation(limit: ResourceLimitation) {
    this.dialog.openLoadingIndicator(
      this.projectApi.setResourceLimitation(this.project.id, limit)
        .then(receivedLimit => {
          this.resourceLimit = receivedLimit;
        }),
      `Updating resource limitation`
    );
  }

  removeGroup(group: Link): void {
    this.dialog.openLoadingIndicator(
      this.projectApi
        .removeGroup(this.project.id, group.name)
        .then(() => {
          const index = this.project.groups.indexOf(group);
          this.project.groups.splice(index, 1);
        }),
      'Removing group from Project'
    );
  }

  onGroupAdded(group: Link): void {
    if (!this.project.groups.includes(group)) {
      this.project.groups.push(group);
    }
  }

  getGroupLinkColor(group: Link): string {
    if (group.role === 'OWNER') {
      return '#8ed69b';
    } else {
      return '#d88bca';
    }
  }

  deleteProject() {
    this.dialog.openAreYouSure(
      `Project being deleted: ${this.project.name}`,
      () => this.projectApi
        .delete(this.project.id)
        .then(() => this.projectDeleted.emit(this.project.id))
    ).then(() => console.log("Deleted project"));
  }
}
