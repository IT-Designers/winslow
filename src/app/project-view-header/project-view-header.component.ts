import {AfterViewInit, Component, EventEmitter, Input, OnInit, Output, ViewChild} from '@angular/core';
import {ProjectInfo, State} from '../api/project-api.service';
import {StateIconComponent} from '../state-icon/state-icon.component';
import {FilesApiService} from '../api/files-api.service';
import {ActivatedRoute, Router} from '@angular/router';

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

  @Output() tagActionPrimary = new EventEmitter<string>();
  @Output() tagActionSecondary = new EventEmitter<string>();

  @Output()
  @ViewChild('icon', {static: false})
  icon: StateIconComponent;

  state: State = null;
  stage: string = null;


  constructor(private files: FilesApiService) { }

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

  thumbnailUrl() {
    return this.files.workspaceUrl(`${this.project.id}/thumbnail.png`);
  }
}
