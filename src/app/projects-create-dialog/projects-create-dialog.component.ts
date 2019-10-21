import {Component, Inject, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material';
import {PipelineApiService, PipelineInfo} from '../api/pipeline-api.service';

interface CreateProjectData {
  name: string;
  pipeline: PipelineInfo;
}

@Component({
  selector: 'app-project-create',
  templateUrl: './projects-create-dialog.component.html',
  styleUrls: ['./projects-create-dialog.component.css']
})
export class ProjectsCreateDialog implements OnInit {

  pipelines: PipelineInfo[];

  constructor(
    public dialogRef: MatDialogRef<ProjectsCreateDialog>,
    @Inject(MAT_DIALOG_DATA) public data: CreateProjectData,
    private api: PipelineApiService) {
  }

  ngOnInit() {
    this.api.getPipelineDefinitions().then(pipelines => this.pipelines = pipelines);
  }
}
