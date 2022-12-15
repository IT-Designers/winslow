import {AfterViewInit, Component, EventEmitter, Input, OnInit, Output, ViewChild} from '@angular/core';
import {StateIconComponent} from '../../state-icon/state-icon.component';
import {TagFilterComponent} from '../tag-filter/tag-filter.component';
import {MatMenuTrigger} from '@angular/material/menu';
import {MatDialog} from '@angular/material/dialog';
import {AddToContextPopupComponent} from '../add-to-context-popup/add-to-context-popup.component';
import {ProjectInfo, State} from '../../api/winslow-api';

@Component({
  selector: 'app-project-view-header',
  templateUrl: './project-view-header.component.html',
  styleUrls: ['./project-view-header.component.css']
})
export class ProjectViewHeaderComponent implements OnInit, AfterViewInit {

  @Input() project: ProjectInfo;
  @Input() pauseReason: string = null;
  @Input() progress: number = null;
  @Input() running = false;
  @Input() filter: TagFilterComponent;

  @Output() tagActionPrimary = new EventEmitter<string>();
  @Output() tagActionSecondary = new EventEmitter<string>();

  @Output()
  @ViewChild('icon')
  icon: StateIconComponent;

  state: State = null;
  stage: string = null;
  menuPosition: { x: number; y: number } = {x: 0, y: 0};

  constructor(public dialog: MatDialog) { }

  @Input()
  set iconState(value: State) {
    this.state = value;
    if (this.icon != null) {
      this.icon.state = value;
    }
  }

  @Input()
  set mostRecentStage(stage: string) {
    this.stage = stage;
  }

  ngOnInit() {
  }

  ngAfterViewInit(): void {
    // the ViewChild('icon') might not be set when receiving the state
    // so to ensure the state is passed to the ViewChild('icon'), set it again after init
    this.iconState = this.state;
  }

  rightClickAction(matMenuTrigger: MatMenuTrigger, event: MouseEvent) {
    event.preventDefault();
    this.menuPosition.x = event.x;
    this.menuPosition.y = event.y;
    matMenuTrigger.openMenu();
  }

  excludeTags(project: ProjectInfo) {
    project.tags.forEach( tag => { this.filter.addExcludedTag(tag); });
  }

  includeTags(project: ProjectInfo) {
    project.tags.forEach( tag => { this.filter.addIncludedTag(tag); });
  }

  openAddToContext() {
    this.dialog.open(AddToContextPopupComponent, {
      position: {top: `${this.menuPosition.y + 20}px`, left: `${this.menuPosition.x}px`},
      data: {project: this.project},
    });
  }

}
