import {Component, EventEmitter, Input, Output} from '@angular/core';
import { CommonModule } from '@angular/common';
import {PipelineDefinitionInfo, StageWorkerDefinitionInfo} from "../../../api/winslow-api";

@Component({
  selector: 'app-pipeline-head-settings',
  templateUrl: './pipeline-head-settings.component.html',
  styleUrl: './pipeline-head-settings.component.css'
})
export class PipelineHeadSettingsComponent {
  @Input() selectedNodeData: PipelineDefinitionInfo = {} as PipelineDefinitionInfo;
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
