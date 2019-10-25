import {Component, Input, OnInit} from '@angular/core';
import {Project, State, StateInfo} from '../api/project-api.service';

@Component({
  selector: 'app-project-list',
  templateUrl: './project-list.component.html',
  styleUrls: ['./project-list.component.css']
})
export class ProjectListComponent implements OnInit {

  @Input() projects: Project[];
  @Input() stateInfo: Map<string, StateInfo>;

  constructor() {
  }

  ngOnInit() {
  }

  isRunning(project: Project) {
    return this.stateInfoCheck(false, project, state => state.state === State.Running);
  }

  getProgress(project: Project) {
    return this.stateInfoCheck(null, project, state => state.stageProgress);
  }

  getPauseReason(project: Project) {
    return this.stateInfoCheck(null, project, state => state.pauseReason);
  }

  stateInfoCheck<T>(df: T, project: Project, callback: (stateInfo: StateInfo) => T) {
    if (project != null && this.stateInfo != null) {
      const info = this.stateInfo.get(project.id);
      if (info != null) {
        return callback(info);
      }
    }
    return df;
  }
}
