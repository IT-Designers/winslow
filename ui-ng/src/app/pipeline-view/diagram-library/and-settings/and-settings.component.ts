import {Component, EventEmitter, Input, Output} from '@angular/core';
import { CommonModule } from '@angular/common';
import {StageAndGatewayDefinitionInfo, StageWorkerDefinitionInfo} from "../../../api/winslow-api";

@Component({
  selector: 'app-and-settings',
  templateUrl: './and-settings.component.html',
  styleUrl: './and-settings.component.css'
})
export class AndSettingsComponent {
  @Input() selectedNodeData: StageAndGatewayDefinitionInfo = {} as StageAndGatewayDefinitionInfo;
  @Output() editNode = new EventEmitter();

  setName(event: any) {
    /*console.dir(event.target.value);*/
    //console.dir(event.target.value);
    if (this.selectedNodeData?.name != undefined) {
      this.selectedNodeData.name = event.target.value;
      //console.dir(this.selectedNodeData);
      this.editNode.emit(this.selectedNodeData);
    }
  }

  setDescription(event: any) {
    if (this.selectedNodeData?.description) {
      this.selectedNodeData.description = event.target.value;
      this.editNode.emit(this.selectedNodeData);
    }
  }
}
