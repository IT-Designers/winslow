import {Component, Inject, OnInit} from '@angular/core';
import {ApiService, PipelineInfo} from '../api.service';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material';

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
    private api: ApiService) {
  }

  ngOnInit() {
    this.api.listPipelines().toPromise().then(pipelines => this.pipelines = pipelines);
  }
}
