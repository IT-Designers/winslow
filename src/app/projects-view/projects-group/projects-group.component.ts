import {Component, Input, OnInit} from '@angular/core';
import {MatMenuTrigger} from '@angular/material/menu';
import {ProjectGroup, ProjectInfo, StateInfo} from '../../api/project-api.service';
import {TagFilterComponent} from '../tag-filter/tag-filter.component';
import {ActivatedRoute, Router} from '@angular/router';
import {FilesApiService} from '../../api/files-api.service';
import {DialogService} from '../../dialog.service';
import {GroupActionsComponent} from '../../group-actions/group-actions.component';
import {MatDialog} from '@angular/material/dialog';

@Component({
  selector: 'app-projects-group',
  templateUrl: './projects-group.component.html',
  styleUrls: ['./projects-group.component.css']
})
export class ProjectsGroupComponent implements OnInit {

  @Input() projectGroup: ProjectGroup;
  @Input() filter: TagFilterComponent;
  menuPosition: { x: number; y: number } = {x: 0, y: 0};
  @Input() selectedProject: ProjectInfo;
  @Input() stateInfo: Map<string, StateInfo>;

  constructor(public route: ActivatedRoute,
              public router: Router,
              private files: FilesApiService,
              private createDialog: MatDialog,
              private dialog: DialogService) {
    this.menuPosition.x = this.menuPosition.y = 0;
  }

  ngOnInit(): void {
  }

  selectProject(project: ProjectInfo) {
    this.router.navigate([project.id], {
      relativeTo: this.route.parent
    });
  }

  thumbnailUrl(project: ProjectInfo) {
    return this.files.workspaceUrl(`${project.id}/output/thumbnail.jpg`);
  }

  makeImageBigger(imageUrl: string, image: MouseEvent) {
    if (image.target[`currentSrc`].includes('favicon.png')) {
      return;
    }
    this.dialog.image(imageUrl);
  }

  rightClickAction(matMenuTrigger: MatMenuTrigger, event: MouseEvent) {
    event.preventDefault();
    this.menuPosition.x = event.x;
    this.menuPosition.y = event.y;
    matMenuTrigger.openMenu();
  }

  openGroupAction(name: string) {
    this.createDialog
      .open(GroupActionsComponent, {
        data: {tag: name}
      });
  }

}
