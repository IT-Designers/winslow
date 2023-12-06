import {Component, Input, OnInit} from '@angular/core';
import {MatMenuTrigger} from '@angular/material/menu';
import {ActivatedRoute, Router} from '@angular/router';
import {GroupActionsComponent} from '../../group-actions/group-actions.component';
import {MatDialog} from '@angular/material/dialog';
import {AddToContextPopupComponent} from '../add-to-context-popup/add-to-context-popup.component';
import {ProjectInfo, StateInfo} from '../../api/winslow-api';
import {ProjectGroup} from '../../api/project-api.service';
import {ProjectsViewFilterComponent} from "../projects-view-filter/projects-view-filter.component";

@Component({
  selector: 'app-projects-group',
  templateUrl: './projects-group.component.html',
  styleUrls: ['./projects-group.component.css']
})
export class ProjectsGroupComponent implements OnInit {

  @Input() projectGroup!: ProjectGroup;
  @Input() filter!: ProjectsViewFilterComponent;
  menuPosition: { x: number; y: number } = {x: 0, y: 0};
  @Input() selectedProject?: ProjectInfo;
  @Input() stateInfo?: Map<string, StateInfo>;

  constructor(
    public route: ActivatedRoute,
    public router: Router,
    private createDialog: MatDialog,
  ) {
    this.menuPosition.x = this.menuPosition.y = 0;
  }

  ngOnInit(): void {
  }

  selectProject(project: ProjectInfo) {
    this.router.navigate([project.id], {
      relativeTo: this.route.parent
    });
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

  openAddToContext() {
    this.createDialog.open(AddToContextPopupComponent, {
      position: {top: `${this.menuPosition.y}px`, left: `${this.menuPosition.x}px`},
      data: {projectGroup: this.projectGroup},
    });
  }

}
