import {Component, OnInit} from '@angular/core';
import {Project, ProjectApiService} from '../api/project-api.service';
import {LongLoadingDetector} from '../long-loading-detector';
import {PipelineApiService, PipelineInfo, StageInfo} from '../api/pipeline-api.service';
import {MatDialog} from '@angular/material';
import {FileBrowseDialog} from '../file-browse-dialog/file-browse-dialog.component';

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
  actionLoadError = null;
  actionLongLoading = new LongLoadingDetector();

  selectedPipeline: PipelineInfo = null;
  selectedStage: StageInfo = null;
  environmentVariables: Map<string, [boolean, string]> = null;


  constructor(public api: ProjectApiService, private pipelineApi: PipelineApiService, private dialog: MatDialog) {
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
    this.selectedPipeline = null;
    this.selectedStage = null;
    this.environmentVariables = null;

    for (const pipeline of this.pipelines) {
      if (pipeline.id === pipelineId) {
        this.selectedPipeline = pipeline;
        break;
      }
    }
  }

  loadEnvForStageName(stageName: string) {
    if (this.selectedPipeline != null) {
      for (const stage of this.selectedPipeline.stages) {
        if (stage.name === stageName) {
          this.selectedStage = stage;

          this.environmentVariables = new Map();
          this.selectedPipeline.requiredEnvVariables.forEach(env => this.environmentVariables.set(env, [true, null]));
          this.selectedStage.requiredEnvVariables.forEach(env => this.environmentVariables.set(env, [true, null]));

          break;
        }
      }
    }
  }

  getSelectedImageArgs(): string {
    return this.selectedStage.image.args.join(' ');
  }

  browseForValue(valueReceiver: HTMLInputElement) {
    this.dialog.open(FileBrowseDialog, {
      data: {
        preselectedPath: valueReceiver.value.trim().length > 0 ? valueReceiver.value.trim() : null
      }
    })
      .afterClosed()
      .toPromise()
      .then(result => {
        if (result) {
          valueReceiver.value = result;
        }
      });
  }
}
