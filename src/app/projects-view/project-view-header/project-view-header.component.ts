import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {TagFilterComponent} from '../tag-filter/tag-filter.component';
import {MatMenuTrigger} from '@angular/material/menu';
import {MatDialog} from '@angular/material/dialog';
import {AddToContextPopupComponent} from '../add-to-context-popup/add-to-context-popup.component';
import {ProjectInfo, State} from '../../api/winslow-api';
import {FilesApiService} from "../../api/files-api.service";
import {DialogService} from "../../dialog.service";

@Component({
  selector: 'app-project-view-header',
  templateUrl: './project-view-header.component.html',
  styleUrls: ['./project-view-header.component.css']
})
export class ProjectViewHeaderComponent implements OnInit {

  @Input() project!: ProjectInfo;
  @Input() pauseReason?: string;
  @Input() progress?: number;
  @Input() running = false;
  @Input() filter!: TagFilterComponent;

  @Input() state?: State;
  @Input() stage?: string;

  @Output() tagActionPrimary = new EventEmitter<string>();
  @Output() tagActionSecondary = new EventEmitter<string>();

  menuPosition: { x: number; y: number } = {x: 0, y: 0};

  constructor(
    public dialog: MatDialog,
    private files: FilesApiService,
    private customDialog: DialogService,
  ) {
  }

  ngOnInit() {
  }

  rightClickAction(matMenuTrigger: MatMenuTrigger, event: MouseEvent) {
    event.preventDefault();
    this.menuPosition.x = event.x;
    this.menuPosition.y = event.y;
    matMenuTrigger.openMenu();
  }

  excludeTags(project: ProjectInfo) {
    project.tags.forEach(tag => {
      this.filter.addExcludedTag(tag);
    });
  }

  includeTags(project: ProjectInfo) {
    project.tags.forEach(tag => {
      this.filter.addIncludedTag(tag);
    });
  }

  openAddToContext() {
    this.dialog.open(AddToContextPopupComponent, {
      position: {top: `${this.menuPosition.y + 20}px`, left: `${this.menuPosition.x}px`},
      data: {project: this.project},
    });
  }

  thumbnailUrl(project: ProjectInfo) {
    return this.files.workspaceUrl(`${project.id}/output/thumbnail.jpg`);
  }

  makeImageBigger(imageUrl: string, event: MouseEvent) {
    const target = event.target;
    if (target instanceof HTMLImageElement && target.currentSrc.includes('winslow_quadratic.svg')) {
      return;
    }
    this.customDialog.image(imageUrl);
  }
}
