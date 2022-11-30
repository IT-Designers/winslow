import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {ProjectInfoExt, ProjectGroup} from '../api/project-api.service';
import {TagFilterComponent} from './tag-filter/tag-filter.component';
import {ActivatedRoute, Router} from '@angular/router';
import {FilesApiService} from '../api/files-api.service';
import {DialogService} from '../dialog.service';
import {StateInfo} from '../api/winslow-api';

@Component({
  selector: 'app-projects-view',
  templateUrl: './projects-view.component.html',
  styleUrls: ['./projects-view.component.css']
})
export class ProjectsViewComponent implements OnInit {

  @Input() projects: ProjectInfoExt[];
  @Input() projectsFiltered: ProjectInfoExt[];
  @Input() projectsGroups: ProjectGroup[];
  @Input() selectedProject: ProjectInfoExt;
  @Input() stateInfo: Map<string, StateInfo>;
  @Input() filter: TagFilterComponent;
  @Input() groupsOnTop: boolean;
  documentGet = document;

  @Output() tagActionPrimary = new EventEmitter<string>();
  @Output() tagActionSecondary = new EventEmitter<string>();

  constructor(public route: ActivatedRoute,
              public router: Router,
              private files: FilesApiService,
              private dialog: DialogService) {
  }

  ngOnInit(): void {
  }

  selectProject(project: ProjectInfoExt) {
    this.router.navigate([project.id], {
      relativeTo: this.route.parent
    });
  }

  thumbnailUrl(project: ProjectInfoExt) {
    return this.files.workspaceUrl(`${project.id}/output/thumbnail.jpg`);
  }

  makeImageBigger(imageUrl: string, image: MouseEvent) {
    if (image.target[`currentSrc`].includes('favicon.png')) {
      return;
    }
    this.dialog.image(imageUrl);
  }
}
