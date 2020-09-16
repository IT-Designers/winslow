import {Component, Inject, OnInit} from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import {PipelineApiService, PipelineInfo} from '../api/pipeline-api.service';
import {ProjectApiService} from '../api/project-api.service';

export interface CreateProjectData {
  name: string;
  pipeline: PipelineInfo;
  tags: string[];
}

@Component({
  selector: 'app-project-create',
  templateUrl: './projects-create-dialog.component.html',
  styleUrls: ['./projects-create-dialog.component.css']
})
export class ProjectsCreateDialog implements OnInit {

  pipelines: PipelineInfo[];
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
