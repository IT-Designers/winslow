import {Component, Input, OnInit} from '@angular/core';
import {ProjectInfoExt} from '../api/project-api.service';
import {StateInfo} from '../api/winslow-api';

@Component({
  selector: 'app-project-list',
  templateUrl: './project-list.component.html',
  styleUrls: ['./project-list.component.css']
})
export class ProjectListComponent implements OnInit {

  @Input() projects: ProjectInfoExt[];
  @Input() stateInfo: Map<string, StateInfo>;

  constructor() {
  }

  ngOnInit() {
  }

  isRunning(project: ProjectInfoExt) {
    return this.stateInfoCheck(false, project, state => state.state === 'Running');
  }

  getProgress(project: ProjectInfoExt) {
    return this.stateInfoCheck(null, project, state => state.stageProgress);
  }

  getPauseReason(project: ProjectInfoExt) {
    return this.stateInfoCheck(null, project, state => state.pauseReason);
  }

  stateInfoCheck<T>(df: T, project: ProjectInfoExt, callback: (stateInfo: StateInfo) => T) {
    if (project != null && this.stateInfo != null) {
      const info = this.stateInfo.get(project.id);
      if (info != null) {
        return callback(info);
      }
    }
    return df;
  }
}
