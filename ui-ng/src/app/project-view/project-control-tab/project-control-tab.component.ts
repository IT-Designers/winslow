import {Component, Input, ViewChild} from '@angular/core';
import {
  EnvVariable, ExecutionGroupInfo, ImageInfo,
  PipelineDefinitionInfo, ProjectInfo,
  RangedValue, ResourceInfo,
  StageDefinitionInfo, WorkspaceConfiguration,
  WorkspaceMode
} from "../../api/winslow-api";
import {StageExecutionSelectionComponent} from "../../stage-execution-selection/stage-execution-selection.component";
import {DialogService} from "../../dialog.service";
import {ProjectApiService} from "../../api/project-api.service";
import {
  GroupSettingsDialogComponent,
  GroupSettingsDialogData
} from "../../group-settings-dialog/group-settings-dialog.component";
import {MatDialog} from "@angular/material/dialog";

@Component({
  selector: 'app-project-control-tab',
  templateUrl: './project-control-tab.component.html',
  styleUrls: ['./project-control-tab.component.css']
})
export class ProjectControlTabComponent {

  @ViewChild('executionSelection') executionSelection: StageExecutionSelectionComponent;
  projectInfo: ProjectInfo;

  @Input() set project(project: ProjectInfo) {
    this.projectInfo = project;
  }
  get project() {
    return this.projectInfo;
  }

  constructor(
    private matDialog: MatDialog,
    private dialog: DialogService,
    private projectApi: ProjectApiService,
  ) {
  }

  // todo
  saveConfiguration(pipeline: PipelineDefinitionInfo, stage: StageDefinitionInfo, env: any, image: ImageInfo, requiredResources?: ResourceInfo) {
    if (pipeline.name === this.project.pipelineDefinition.name) {
      let index = null;
      for (let i = 0; i < pipeline.stages.length; ++i) {
        if (pipeline.stages[i].name === stage.name) {
          index = i;
          break;
        }
      }
      if (index !== null) {
        this.dialog.openLoadingIndicator(
          this.projectApi.configureGroup(this.project.id, index, [this.project.id], env, image, requiredResources),
          `Submitting selections`
        );
      }
    } else {
      this.dialog.error('Changing the Pipeline is not yet supported!');
    }
  }

  // todo
  saveConfigurationOnOtherProjects(pipeline: PipelineDefinitionInfo, stage: StageDefinitionInfo, env: any, image: ImageInfo) {
    for (let i = 0; i < pipeline.stages.length; ++i) {
      if (stage.name === pipeline.stages[i].name) {
        return this.projectApi.listProjects()
          .then(projects => {
            return this.matDialog
              .open(GroupSettingsDialogComponent, {
                data: {
                  projects,
                  availableTags: this.projectApi.cachedTags,
                } as GroupSettingsDialogData
              })
              .afterClosed()
              .toPromise()
              .then((selectedProjects: ProjectInfo[] | null) => {
                if (selectedProjects) {
                  const selectedProjectIds = selectedProjects.map(project => project.id)
                  return this.dialog.openLoadingIndicator(
                    this.projectApi.configureGroup(this.project.id, i, selectedProjectIds, env, image)
                      .then(configureResult => {
                        const failed = [];
                        for (let n = 0; n < configureResult.length && n < selectedProjectIds.length; ++n) {
                          if (!configureResult[n]) {
                            failed.push(selectedProjectIds[n]);
                          }
                        }
                        if (failed.length === 0) {
                          return Promise.resolve();
                        } else {
                          return Promise.reject('The operation failed for at least one project: ' + (failed.join(', ')));
                        }
                      }),
                    `Applying settings on all selected projects`,
                  );
                }
              });
          });
      }
    }
  }

  enqueue(
    pipeline: PipelineDefinitionInfo,
    stageDefinitionInfo: StageDefinitionInfo,
    env: any,
    rangedEnv: any,
    image: ImageInfo,
    requiredResources?: ResourceInfo,
    workspaceConfiguration?: WorkspaceConfiguration,
    comment?: string,
    runSingle?: boolean,
    resume?: boolean,
  ) {
    if (pipeline.name === this.project.pipelineDefinition.name) {
      this.dialog.openLoadingIndicator(
        this.projectApi.enqueue(
          this.project.id,
          stageDefinitionInfo.id,
          env,
          rangedEnv,
          image,
          requiredResources,
          workspaceConfiguration,
          comment,
          runSingle,
          resume),
        `Submitting selections`
      );
    } else {
      this.dialog.error('Changing the Pipeline is not yet supported!');
    }
  }
}
