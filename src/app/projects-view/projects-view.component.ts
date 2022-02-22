import {Component, Input, OnInit} from '@angular/core';
import {ProjectGroup, ProjectInfo, StateInfo} from '../api/project-api.service';
import {TagFilterComponent} from '../tag-filter/tag-filter.component';
import {ActivatedRoute, Router} from '@angular/router';

@Component({
  selector: 'app-projects-view',
  templateUrl: './projects-view.component.html',
  styleUrls: ['./projects-view.component.css']
})
export class ProjectsViewComponent implements OnInit {

  @Input() projects: ProjectInfo[];
  @Input() projectsFiltered: ProjectInfo[];
  @Input() projectsGroups: ProjectGroup[];
  @Input() selectedProject: ProjectInfo;
  @Input() stateInfo: Map<string, StateInfo>;
  @Input() filter: TagFilterComponent;
  @Input() router: Router;
  @Input() route: ActivatedRoute;

  constructor() {}

  ngOnInit(): void {
  }

  selectProject(project: ProjectInfo) {
    this.router.navigate([project.id], {
      relativeTo: this.route.parent
    });
  }

}
