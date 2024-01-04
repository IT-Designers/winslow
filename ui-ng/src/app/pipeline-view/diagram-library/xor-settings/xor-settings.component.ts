import {Component, EventEmitter, Input, Output} from '@angular/core';
import { CommonModule } from '@angular/common';
import {StageWorkerDefinitionInfo, StageXOrGatewayDefinitionInfo} from "../../../api/winslow-api";

@Component({
  selector: 'app-xor-settings',
  templateUrl: './xor-settings.component.html',
  styleUrl: './xor-settings.component.css'
})
export class XorSettingsComponent {
  @Input() selectedNodeData: StageXOrGatewayDefinitionInfo = {} as StageXOrGatewayDefinitionInfo;
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
