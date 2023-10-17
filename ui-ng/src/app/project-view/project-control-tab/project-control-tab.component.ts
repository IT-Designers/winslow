import {Component, Input, ViewChild} from '@angular/core';
import {
  ImageInfo,
  PipelineDefinitionInfo, ProjectInfo,
  ResourceInfo,
  StageDefinitionInfo, WorkspaceConfiguration,
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

  @Input() set project(project: ProjectInfo) {
    this._project = project;
    // todo make stage execution selection able to react to pipeline changes on its own (or replace it entirely)
    // trigger rerender of stage execution selection
    this.pipelineDefinition = project.pipelineDefinition;
  };

  get project() {
    return this._project;
  }

  private _project: ProjectInfo;

  pipelineDefinition: PipelineDefinitionInfo;

  constructor(
    private dialog: DialogService,
    private projectApi: ProjectApiService,
  ) {
  }

  enqueue(
    pipeline: PipelineDefinitionInfo,
    stageDefinitionInfo: StageDefinitionInfo,
    env: any,
    rangedEnv: any,
    image: ImageInfo,
    requiredResources: ResourceInfo,
    workspaceConfiguration: WorkspaceConfiguration,
    comment: string,
    runSingle: boolean,
    resume: boolean,
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
