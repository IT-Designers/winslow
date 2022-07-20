import {Component, Inject, OnInit} from '@angular/core';
import {ProjectApiService, ProjectGroup, ProjectInfo, StateInfo} from '../api/project-api.service';
import {LongLoadingDetector} from '../long-loading-detector';
import {PipelineApiService, PipelineInfo} from '../api/pipeline-api.service';
import {MAT_DIALOG_DATA} from '@angular/material/dialog';

@Component({
  selector: 'app-group-actions',
  templateUrl: './group-actions.component.html',
  styleUrls: ['./group-actions.component.css']
})
export class GroupActionsComponent implements OnInit {

  projects: ProjectInfo[] = [];
  projectsFiltered: ProjectInfo[] = null;
  projectsGroups: ProjectGroup[] = [];
  stateInfo: Map<string, StateInfo> = null;
  selectedProject: ProjectInfo = null;

  projectsLoadError = null;
  projectsLongLoading = new LongLoadingDetector();

  pipelines: PipelineInfo[] = null;
  actionLoadError = null;
  actionLongLoading = new LongLoadingDetector();
  groupsOnTop: boolean;

  constructor(public api: ProjectApiService,
              @Inject(MAT_DIALOG_DATA) public data: any,
              private pipelineApi: PipelineApiService) {
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
