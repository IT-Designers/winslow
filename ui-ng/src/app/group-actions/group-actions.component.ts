import {Component, Inject, OnInit} from '@angular/core';
import {ProjectApiService, ProjectGroup} from '../api/project-api.service';
import {LongLoadingDetector} from '../long-loading-detector';
import {PipelineApiService} from '../api/pipeline-api.service';
import {MAT_DIALOG_DATA} from '@angular/material/dialog';
import {PipelineDefinitionInfo, ProjectInfo, StateInfo} from '../api/winslow-api';

@Component({
  selector: 'app-group-actions',
  templateUrl: './group-actions.component.html',
  styleUrls: ['./group-actions.component.css']
})
export class GroupActionsComponent implements OnInit {

  projects: ProjectInfo[] = [];
  projectsFiltered?: ProjectInfo[];
  projectsGroups: ProjectGroup[] = [];
  stateInfo?: Map<string, StateInfo>;
  selectedProject?: ProjectInfo;

  projectsLoadError = null;
  projectsLongLoading = new LongLoadingDetector();

  pipelines?: PipelineDefinitionInfo[];
  actionLoadError?: Error;
  actionLongLoading = new LongLoadingDetector();
  groupsOnTop?: boolean;

  constructor(
    public api: ProjectApiService,
    @Inject(MAT_DIALOG_DATA) public data: {tag: string},
    private pipelineApi: PipelineApiService
  ) {
  }

  ngOnInit() {
    this.projectsLongLoading.increase();
    this.api.listProjects()
      .then(projects => this.projectsFiltered = this.projects = projects)
      .catch(err => this.projectsLoadError = err)
      .finally(() => this.projectsLongLoading.decrease());

    this.actionLongLoading.increase();
    this.pipelineApi.getPipelineDefinitions()
      .then(pipelines => this.pipelines = pipelines)
      .catch(err => this.actionLoadError = err)
      .finally(() => this.actionLongLoading.decrease());
  }
}
