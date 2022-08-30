import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {ProjectGroup, ProjectInfo} from '../../../api/project-api.service';
import {LocalStorageService} from '../../../api/local-storage.service';

@Component({
  selector: 'app-projects-group-builder',
  templateUrl: './projects-group-builder.component.html',
  styleUrls: ['./projects-group-builder.component.css']
})
export class ProjectsGroupBuilderComponent implements OnInit {

  CONTEXT_PREFIX = 'context::';
  groupsActivated = true;
  availableTagsValue: string[];
  projectsValue: ProjectInfo[];

  @Output('projectsGroups') projectsGroups = new EventEmitter<ProjectGroup[]>();
  @Output('groupsOnTop') groupsOnTop = new EventEmitter<boolean>();
  groupsOnTopIsChecked = false;

  GROUPS_ON_TOP_SETTING = 'GROUPS_ON_TOP';
  GROUPS_ACTIVATED = 'GROUPS_ACTIVATED';

  constructor(private localStorageService: LocalStorageService) {
  }

  ngOnInit(): void {
    this.localStorageService.getSettings(this.GROUPS_ON_TOP_SETTING) ?
      this.groupsOnTopIsChecked = this.localStorageService.getSettings(this.GROUPS_ON_TOP_SETTING) :
      this.groupsOnTopIsChecked = false;
    this.groupsOnTop.emit(this.groupsOnTopIsChecked);
    this.localStorageService.getSettings(this.GROUPS_ACTIVATED) ?
      this.groupsActivated = this.localStorageService.getSettings(this.GROUPS_ACTIVATED) :
      this.groupsActivated = true;
    this.updateGroups();
  }

  @Input('projects')
  set projects(projects: ProjectInfo[]) {
    this.projectsValue = projects;
    this.updateGroups();
  }

  @Input('availableTags')
  set availableTags(tags: string[]) {
    this.availableTagsValue = tags
      .filter(tag => !tag.startsWith(this.CONTEXT_PREFIX));
    this.updateGroups();
  }

  updateGroups() {
    if (this.projectsValue.length == null || this.groupsActivated === false || !this.availableTagsValue) {
      this.projectsGroups.emit(null);
      return;
    }
    let projectGroups: ProjectGroup[] = [];
    for (const tag of this.availableTagsValue) {
      const projectsForTag: ProjectInfo[] = [];
      for (const project of this.projectsValue) {
        for (const tagOfProject of project.tags) {
          if (tag === tagOfProject) {
            projectsForTag.push(project);
          }
        }
        if (project.tags.length <= 0) {
          if (!this.isProjectForGroupExisting(projectGroups, project)) {
            projectGroups.push(this.buildGroup(project.name, [project]));
          }
        }
      }
      if (projectsForTag[0] !== undefined) {
        if (projectsForTag[1] === undefined) {
          if (!this.isProjectForGroupExisting(projectGroups, projectsForTag[0])) {
            projectGroups.push(this.buildGroup(projectsForTag[0].name, projectsForTag));
          }
        } else {
          if (!projectGroups.includes(this.buildGroup(tag, projectsForTag))) {
            projectGroups.push(this.buildGroup(tag, projectsForTag));
          }
        }
      }
    }
    projectGroups = this.sortGroups(projectGroups);
    if (projectGroups.length <= 0) {
      this.projectsGroups.emit(null);
    } else {
      this.projectsGroups.emit(projectGroups);
    }
  }

  private isProjectForGroupExisting(projectGroups: ProjectGroup[], project: ProjectInfo) {
    for (const group of projectGroups) {
      if (group.projects[0].id === project.id) {
        return true;
      }
    }
    return false;
  }

  private sortGroups(groups: ProjectGroup[]) {
    groups.sort((a, b) => a.name.localeCompare(b.name));
    if (this.groupsOnTopIsChecked) {
      const newGroups: ProjectGroup[] = [];
      for (const group of groups) {
        if (group.projects.length > 1) {
          newGroups.push(group);
        }
      }
      for (const group of groups) {
        if (group.projects.length <= 1) {
          newGroups.push(group);
        }
      }
      groups = newGroups;
    }
    return groups;
  }

  private buildGroup(tag: string, projectsForTag: ProjectInfo[]) {
    const group: ProjectGroup = new ProjectGroup();
    group.name = tag;
    group.projects = projectsForTag;
    return group;
  }

  updateLocalStorage(key: string, value: boolean) {
    this.localStorageService.setSettings(key, value);
  }
}
