import {Component, Input, OnInit, Output, ViewChild} from '@angular/core';
import {Project} from '../api/project-api.service';
import {StateIconComponent} from '../state-icon/state-icon.component';

@Component({
  selector: 'app-project-view-header',
  templateUrl: './project-view-header.component.html',
  styleUrls: ['./project-view-header.component.css']
})
export class ProjectViewHeaderComponent implements OnInit {

  @Input() project: Project;
  @Input() pauseReason: string = null;
  @Input() progress: number = null;
  @Input() running = false;

  @Output()
  @ViewChild('icon', {static: false})
  icon: StateIconComponent;

  constructor() {
  }

  ngOnInit() {
  }

}
