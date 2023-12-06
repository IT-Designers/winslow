import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {ProjectGroup} from '../api/project-api.service';
import {ActivatedRoute, Router} from '@angular/router';
import {ProjectInfo, StateInfo} from '../api/winslow-api';
import {ProjectsViewFilterComponent} from "./projects-view-filter/projects-view-filter.component";

@Component({
  selector: 'app-projects-view',
  templateUrl: './projects-view.component.html',
  styleUrls: ['./projects-view.component.css']
})
export class ProjectsViewComponent implements OnInit {

  @Input() stateInfo?: Map<string, StateInfo>;
  @Input() selectedProject?: ProjectInfo;
  @Input() projects!: ProjectInfo[];
  @Input() projectsFiltered?: ProjectInfo[];
  @Input() projectsGroups!: ProjectGroup[];
  @Input() filter!: ProjectsViewFilterComponent;
  @Input() groupsOnTop?: boolean;

  @Output() tagActionPrimary = new EventEmitter<string>();
  @Output() tagActionSecondary = new EventEmitter<string>();

  constructor(
    public route: ActivatedRoute,
    public router: Router,
  ) {
  }

  ngOnInit(): void {
  }

  selectProject(project: ProjectInfo) {
    this.router.navigate([project.id], {
      relativeTo: this.route.parent
    });
  }

}
