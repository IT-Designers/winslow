import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {ProjectGroup} from '../../api/project-api.service';
import {LocalStorageService} from '../../api/local-storage.service';
import {ProjectInfo} from '../../api/winslow-api';

@Component({
  selector: 'app-tag-filter',
  templateUrl: './tag-filter.component.html',
  styleUrls: ['./tag-filter.component.css']
})
export class TagFilterComponent implements OnInit {

  availableTagsValue: string[] = [];
  projectsValue!: ProjectInfo[];
  filteredProjects!: ProjectInfo[];
  projectsGroupsValue?: ProjectGroup[];
  lastPreselectedTag?: string;

  @Output('filtered') filtered = new EventEmitter<ProjectInfo[] | undefined>();
  @Output('projectsGroups') projectsGroups = new EventEmitter<ProjectGroup[]>();
  @Output('groupsOnTop') groupsOnTop = new EventEmitter<boolean>();

  @Input()
  set preSelectedTag(tag: string | undefined) {
    if (this.lastPreselectedTag) {
      this.removeIncludedTag(this.lastPreselectedTag);
    }
    if (tag) {
      this.addIncludedTag(tag);
      this.lastPreselectedTag = tag;
    }
    this.updateFilter();
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

  includeTags: string[] = [];
  includeEmpty = false;
  excludeTags: string[] = [];
  excludeEmpty = false;
  groupsOnTopIsChecked!: boolean;


  constructor(private localStorageService: LocalStorageService) {
  }

  ngOnInit() {
    this.groupsOnTopIsChecked = this.localStorageService.getGroupsOnTop() ?? false;
    if (this.localStorageService.getSelectedContext() !== '' && this.localStorageService.getSelectedContext() != null) {
      this.preSelectedTag = 'context::' + this.localStorageService.getSelectedContext();
      if (this.preSelectedTag) {
        this.addIncludedTag(this.preSelectedTag);
      }
    }
    this.updateFilter();
  }

  toggleIncludedTag(tag: string) {
    if (this.includeTags != null) {
      const index = this.includeTags.indexOf(tag);
      if (index < 0) {
        const tags = this.includeTags.map(t => t);
        tags.push(tag);
        this.includeTags = tags; // notify the bindings
      } else {
        const tags = this.includeTags.map(t => t);
        tags.splice(index, 1);
        this.includeTags = tags; // notify the bindings
      }
      this.updateFilter();
    }
  }

  addIncludedTag(tag: string) {
    if (this.includeTags != null && this.includeTags.indexOf(tag) < 0) {
      const tags = this.includeTags.map(t => t);
      tags.push(tag);
      this.includeTags = tags; // notify the bindings
    }
    this.updateFilter();
  }

  removeIncludedTag(tag: string) {
    if (this.includeTags != null) {
      const index = this.includeTags.indexOf(tag);
      const tags = this.includeTags.map(t => t);
      tags.splice(index, 1);
      this.includeTags = tags; // notify the bindings
    }
    this.updateFilter();
  }

  addExcludedTag(tag: string) {
    if (this.excludeTags != null && this.excludeTags.indexOf(tag) < 0) {
      const tags = this.excludeTags.map(t => t);
      tags.push(tag);
      this.excludeTags = tags; // notify the bindings
    }
    this.updateFilter();
  }

  updateFilter() {
    if (this.projectsValue == undefined) {
      this.filtered.emit(undefined);
      return;
    }
    this.filteredProjects = this.getFilteredProjects();
    this.filtered.emit(this.filteredProjects);
  }

  getFilteredProjects() {
    return this.projectsValue.filter(project => {
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

  emitGroups() {
    this.projectsGroups.emit(this.projectsGroupsValue);
    this.groupsOnTop.emit(this.groupsOnTopIsChecked);
  }

}
