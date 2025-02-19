import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {ProjectGroup} from '../../../api/project-api.service';
import {LocalStorageService} from '../../../api/local-storage.service';
import {ProjectInfo} from '../../../api/winslow-api';

@Component({
  selector: 'app-projects-group-builder',
  templateUrl: './projects-group-builder.component.html',
  styleUrls: ['./projects-group-builder.component.css']
})
export class ProjectsGroupBuilderComponent implements OnInit {

  CONTEXT_PREFIX = 'context::';
  groupsActivated = true;
  availableTagsValue!: string[];
  projectsValue!: ProjectInfo[];

  @Output('projectsGroups') projectsGroups = new EventEmitter<ProjectGroup[]>();
  @Output('groupsOnTop') groupsOnTop = new EventEmitter<boolean>();
  groupsOnTopIsChecked = false;

  constructor(private localStorageService: LocalStorageService) {
  }

  ngOnInit(): void {
    this.groupsOnTopIsChecked = this.localStorageService.getGroupsOnTop() ?? false;
    this.groupsOnTop.emit(this.groupsOnTopIsChecked);
    this.groupsActivated = this.localStorageService.getGroupsActivated() ?? true;
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
    if (this.projectsValue.length == null || !this.groupsActivated || !this.availableTagsValue) {
      this.projectsGroups.emit(undefined);
      return;
    }
    let projectGroups: ProjectGroup[] = [];
    for (const tag of this.availableTagsValue) {
      const projectsForTag: ProjectInfo[] = [];
      for (const project of this.projectsValue) {
        for (const tagOfProject of this.filterProjectTag(project)) {
          if (tag === tagOfProject) {
            projectsForTag.push(project);
          }
        }
        if (this.filterProjectTag(project).length <= 0) {
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
      this.projectsGroups.emit(undefined);
    } else {
      this.projectsGroups.emit(projectGroups);
    }
  }

  private filterProjectTag(project: ProjectInfo) {
    return project.tags.filter(tag => !tag.startsWith(this.CONTEXT_PREFIX));
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
    return new ProjectGroup({
      name: tag,
      projects: projectsForTag,
    });
  }

  updateGroupsOnTop(): void {
    this.localStorageService.setGroupsOnTop(this.groupsOnTopIsChecked);
  }

  updateGroupsActivated(): void {
    this.localStorageService.setGroupsActivated(this.groupsActivated);
  }
}
