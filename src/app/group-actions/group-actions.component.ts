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

  includeTags: string[] = [];
  includeEmpty = false;
  excludeTags: string[] = [];
  excludeEmpty = false;

  pipelines: PipelineInfo[] = null;
  stages: StageInfo[] = null;
  actionLoadError = null;
  actionLongLoading = new LongLoadingDetector();


  constructor(public api: ProjectApiService, private pipelineApi: PipelineApiService) {
  }

  ngOnInit() {
    this.projectsLongLoading.increase();
    this.api.listProjects()
      .toPromise()
      .then(projects => this.filtered = this.projects = projects)
      .catch(err => this.projectsLoadError = err)
      .finally(() => this.projectsLongLoading.decrease());

    this.actionLongLoading.increase();
    this.pipelineApi.getPipelineDefinitions()
      .then(pipelines => this.pipelines = pipelines)
      .catch(err => this.actionLoadError = err)
      .finally(() => this.actionLongLoading.decrease());
  }

  updateFilter() {
    this.filtered = this.projects.filter(project => {
      if (project.tags.length === 0) {
        if (this.includeEmpty) {
          return true;
        } else if (this.excludeEmpty) {
          return false;
        }
      }

      for (const tag of this.includeTags) {
        if (project.tags.indexOf(tag) < 0) {
          return false;
        }
      }
      for (const tag of this.excludeTags) {
        if (project.tags.indexOf(tag) >= 0) {
          return false;
        }
      }
      return true;
    });
  }

  loadStagesForPipeline(pipelineId: string) {
    this.actionLongLoading.increase();
    this.pipelineApi.getStageDefinitions(pipelineId)
      .then(result => this.stages = result)
      .catch(err => this.actionLoadError = err)
      .finally(() => this.actionLongLoading.decrease());
  }
}
