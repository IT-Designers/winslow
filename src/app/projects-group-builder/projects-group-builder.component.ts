import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {ProjectGroup, ProjectInfo} from '../api/project-api.service';

@Component({
  selector: 'app-projects-group-builder',
  templateUrl: './projects-group-builder.component.html',
  styleUrls: ['./projects-group-builder.component.css']
})
export class ProjectsGroupBuilderComponent implements OnInit {

  groupsActivated = true;
  availableTagsValue: string[];
  projectsValue: ProjectInfo[];

  @Output('projectsGroups') projectsGroups = new EventEmitter<ProjectGroup[]>();

  constructor() {
  }

  ngOnInit(): void {
    this.updateGroups();
  }

  @Input('projects')
  set projects(projects: ProjectInfo[]) {
    this.projectsValue = projects;
    this.updateGroups();
  }

  @Input('availableTags')
  set availableTags(tags: string[]) {
    this.availableTagsValue = tags;
    this.updateGroups();
  }

  updateGroups() {
    if (this.projectsValue == null || this.groupsActivated === false || !this.availableTagsValue) {
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
          if (!this.isProjectForGroupExisting(projectGroups, projectsForTag[0])){
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
    this.projectsGroups.emit(projectGroups);
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
    return groups;
  }

  private buildGroup(tag: string, projectsForTag: ProjectInfo[]) {
    const group: ProjectGroup = new ProjectGroup();
    group.name = tag;
    group.projects = projectsForTag;
    return group;
  }
}
