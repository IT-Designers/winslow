import {Component, OnInit} from '@angular/core';
import {Project, ProjectApiService} from '../api/project-api.service';
import {LongLoadingDetector} from '../long-loading-detector';
import {PipelineApiService, PipelineInfo, StageInfo} from '../api/pipeline-api.service';

@Component({
  selector: 'app-group-actions',
  templateUrl: './group-actions.component.html',
  styleUrls: ['./group-actions.component.css']
})
export class GroupActionsComponent implements OnInit {

  projects: Project[];
  filtered: Project[];

  projectsLoadError = null;
  projectsLongLoading = new LongLoadingDetector();

  pipelines: PipelineInfo[] = null;
  actionLoadError = null;
  actionLongLoading = new LongLoadingDetector();

  constructor(public api: ProjectApiService, private pipelineApi: PipelineApiService) {
  }

  ngOnInit() {
    this.projectsLongLoading.increase();
    this.api.listProjects()
      .then(projects => this.filtered = this.projects = projects)
      .catch(err => this.projectsLoadError = err)
      .finally(() => this.projectsLongLoading.decrease());

    this.actionLongLoading.increase();
    this.pipelineApi.getPipelineDefinitions()
      .then(pipelines => this.pipelines = pipelines)
      .catch(err => this.actionLoadError = err)
      .finally(() => this.actionLongLoading.decrease());
  }



}
