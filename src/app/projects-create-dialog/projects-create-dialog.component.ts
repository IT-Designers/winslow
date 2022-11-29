import {Component, Inject, OnInit} from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import {PipelineApiService} from '../api/pipeline-api.service';
import {ProjectApiService} from '../api/project-api.service';
import {IPipelineInfo} from '../api/winslow-api';

export interface CreateProjectData {
  name: string;
  pipeline: IPipelineInfo;
  tags: string[];
}

@Component({
  selector: 'app-project-create',
  templateUrl: './projects-create-dialog.component.html',
  styleUrls: ['./projects-create-dialog.component.css']
})
export class ProjectsCreateDialog implements OnInit {

  pipelines: IPipelineInfo[];
  cachedTags: string[];

  constructor(
    public dialogRef: MatDialogRef<ProjectsCreateDialog>,
    @Inject(MAT_DIALOG_DATA) public data: CreateProjectData,
    private api: PipelineApiService,
    private projectApi: ProjectApiService) {
    this.cachedTags = projectApi.cachedTags;
  }

  ngOnInit() {
    this.api.getPipelineDefinitions().then(pipelines => this.pipelines = pipelines);
  }
}
