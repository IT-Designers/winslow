import {Component, Input, OnInit} from '@angular/core';
import {ProjectApiService} from '../api/project-api.service';
import {DialogService} from '../dialog.service';
import {ProjectInfo} from '../api/winslow-api';

@Component({
  selector: 'app-stop-button',
  templateUrl: './stop-button.component.html',
  styleUrls: ['./stop-button.component.css']
})
export class StopButtonComponent implements OnInit {

  @Input() project?: ProjectInfo;
  @Input() disabled: boolean = false;
  @Input() showText: boolean = true;

  constructor(private api: ProjectApiService,
              private dialog: DialogService) {
  }

  ngOnInit(): void {
  }

  stop(pause: boolean) {
    const project = this.project;
    if (project) {
      this.dialog.openAreYouSure(
        `Halt stage of ${project.name}`,
        () => this.api.stopStage(project.id, pause).then()
      );
    }
  }

  kill() {
    const project = this.project;
    if (project) {
      this.dialog.openAreYouSure(
        `Kill stage of ${project.name}`,
        () => this.api.killStage(project.id).then()
      );
    }
  }
}
