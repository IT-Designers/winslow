import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {ProjectInfo} from '../api/project-api.service';

@Component({
  selector: 'app-tag-filter',
  templateUrl: './tag-filter.component.html',
  styleUrls: ['./tag-filter.component.css']
})
export class TagFilterComponent implements OnInit {

  availableTagsValue: string[];
  projectsValue: ProjectInfo[];

  @Output('filtered') filtered = new EventEmitter<ProjectInfo[]>();

  includeTags: string[] = [];
  includeEmpty = false;
  excludeTags: string[] = [];
  excludeEmpty = false;

  constructor() {
  }

  ngOnInit() {

  }

  @Input('projects')
  set projects(projects: ProjectInfo[]) {
    this.projectsValue = projects;
    this.updateFilter();
  }

  @Input('availableTags')
  set availableTags(tags: string[]) {
    this.availableTagsValue = tags;
    this.updateFilter();
  }

  updateFilter() {
    if (this.projectsValue == null) {
      this.filtered.emit(null);
      return;
    }
    this.filtered.emit(this.projectsValue.filter(project => {
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
    }));
  }

}
