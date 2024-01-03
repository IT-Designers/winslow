import {Component, EventEmitter, Input, Output} from '@angular/core';
import { CommonModule } from '@angular/common';
import {StageWorkerDefinitionInfo} from "../../../api/winslow-api";
import {MatTabsModule} from "@angular/material/tabs";

@Component({
  selector: 'app-worker-settings',
  templateUrl: './worker-settings.component.html',
  styleUrl: './worker-settings.component.css'
})
export class WorkerSettingsComponent {

  @Input() selectedNodeData: StageWorkerDefinitionInfo = {} as StageWorkerDefinitionInfo;
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

  setImageName(event: any) {
    if (this.selectedNodeData instanceof StageWorkerDefinitionInfo) {
      if (this.selectedNodeData.image) {
        this.selectedNodeData.image.name = event.target.value;
        this.editNode.emit(this.selectedNodeData);
      }
    }
  }
  setImageArgs(event: any) {
    if (this.selectedNodeData instanceof StageWorkerDefinitionInfo) {
      if (this.selectedNodeData.image) {
        this.selectedNodeData.image.args = event.target.value;
        this.editNode.emit(this.selectedNodeData);
      }
    }
  }
  setImageMegabytes(event: any) {
    if (this.selectedNodeData instanceof StageWorkerDefinitionInfo) {
      if (this.selectedNodeData.image) {
        this.selectedNodeData.image.shmMegabytes = event.target.value;
        this.editNode.emit(this.selectedNodeData);
      }
    }
  }

  setRequiredCPUs(event: any) {
    if (this.selectedNodeData instanceof StageWorkerDefinitionInfo) {
      if (this.selectedNodeData.requiredResources) {
        this.selectedNodeData.requiredResources.cpus = event.target.value;
        this.editNode.emit(this.selectedNodeData);
      }
    }
  }
  setRequiredRAM(event: any) {
    if (this.selectedNodeData instanceof StageWorkerDefinitionInfo) {
      if (this.selectedNodeData.requiredResources) {
        this.selectedNodeData.requiredResources.megabytesOfRam = event.target.value;
        this.editNode.emit(this.selectedNodeData);
      }
    }
  }
  setRequiredGPUs(event: any) {
    if (this.selectedNodeData instanceof StageWorkerDefinitionInfo) {
      if (this.selectedNodeData.requiredResources) {
        this.selectedNodeData.requiredResources.gpu.count = event.target.value;
        this.editNode.emit(this.selectedNodeData);
      }
    }
  }
}
