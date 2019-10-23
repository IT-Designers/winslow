import {Component, OnInit} from '@angular/core';
import {Project, ProjectApiService} from '../api/project-api.service';
import {LongLoadingDetector} from '../long-loading-detector';

@Component({
  selector: 'app-group-actions',
  templateUrl: './group-actions.component.html',
  styleUrls: ['./group-actions.component.css']
})
export class GroupActionsComponent implements OnInit {

  loadError = null;
  longLoading = new LongLoadingDetector();
  projects: Project[];
  filtered: Project[];


  constructor(public api: ProjectApiService) {
  }

  ngOnInit() {
    this.longLoading.increase();
    this.api.listProjects()
      .toPromise()
      .then(projects => this.filtered = this.projects = projects)
      .catch(err => this.longLoading = err)
      .finally(() => this.longLoading.decrease());
  }

  updateFilter(includeTags: string[], excludeTags: string[]) {
    this.filtered = this.projects.filter(project => {
      for (const tag of includeTags) {
        if (project.tags.indexOf(tag) < 0) {
          return false;
        }
      }
      for (const tag of excludeTags) {
        if (project.tags.indexOf(tag) >= 0) {
          return false;
        }
      }
      return true;
    });
  }
}
