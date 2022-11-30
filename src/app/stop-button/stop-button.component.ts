import {Component, Input, OnInit} from '@angular/core';
import {ProjectInfoExt, ProjectApiService} from '../api/project-api.service';
import {DialogService} from '../dialog.service';

@Component({
  selector: 'app-stop-button',
  templateUrl: './stop-button.component.html',
  styleUrls: ['./stop-button.component.css']
})
export class StopButtonComponent implements OnInit {

  @Input() project: ProjectInfoExt = null;
  @Input() disabled: boolean = false;
  @Input() showText: boolean = true;

  constructor(private api: ProjectApiService,
              private dialog: DialogService) {
  }

  ngOnInit(): void {
  }

  stop(pause: boolean, stageId: string = null) {
    if (this.project) {
      this.dialog.openAreYouSure(
        `Halt stage of ${this.project.name}`,
        () => this.api.stopStage(this.project.id, pause, stageId).then()
      );
    }
  }

  kill(stageId: string = null) {
    if (this.project) {
      this.dialog.openAreYouSure(
        `Kill stage of ${this.project.name}`,
        () => this.api.killStage(this.project.id, stageId).then()
      );
    }
  }


}
