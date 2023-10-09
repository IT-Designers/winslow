import {Component, Inject, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {PipelineApiService} from '../api/pipeline-api.service';
import {ProjectApiService} from '../api/project-api.service';
import {PipelineDefinitionInfo} from '../api/winslow-api';

export enum CreateProjectPipelineOption {
  CreateLocal,
  CreateShared,
  UseShared,
}

export interface CreateProjectData {
  name: string;
  pipelineId: string;
  pipelineOption: CreateProjectPipelineOption;
  tags: string[];
}

@Component({
  selector: 'app-project-create',
  templateUrl: './projects-create-dialog.component.html',
  styleUrls: ['./projects-create-dialog.component.css']
})
export class ProjectsCreateDialog implements OnInit {
  PipelineOptionEnum = CreateProjectPipelineOption;

  pipelineOption: CreateProjectPipelineOption = CreateProjectPipelineOption.UseShared;
  pipelines: PipelineDefinitionInfo[];
  cachedTags: string[];

  constructor(
    public dialogRef: MatDialogRef<ProjectsCreateDialog>,
    @Inject(MAT_DIALOG_DATA) public data: CreateProjectData,
    private api: PipelineApiService,
    projectApi: ProjectApiService
  ) {
    this.cachedTags = projectApi.cachedTags;
  }

  ngOnInit() {
    this.api.getPipelineDefinitions().then(pipelines => this.pipelines = pipelines);
  }

  needsPipelineId() {
    return this.data.pipelineOption == CreateProjectPipelineOption.UseShared;
  }

  blockSubmitButton() {
     if (this.data.name?.trim().length == 0) {
       return true
     }

     if (this.needsPipelineId()) {
       return this.data.pipelineId == null;
     }

     return false
  }
}
