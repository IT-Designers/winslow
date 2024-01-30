import {Component, EventEmitter, Input, Output} from '@angular/core';
import {LogParserInfo, StageWorkerDefinitionInfo} from "../../../api/winslow-api";

@Component({
  selector: 'app-worker-settings',
  templateUrl: './worker-settings.component.html',
  styleUrl: './worker-settings.component.css'
})
export class WorkerSettingsComponent {

  @Input() selectedNodeData: StageWorkerDefinitionInfo = {} as StageWorkerDefinitionInfo;
  @Output() editNode = new EventEmitter();

  emitSave() {
    this.editNode.emit(this.selectedNodeData);
  }

  setName(event: any) {
    if (this.selectedNodeData?.name) {
      this.selectedNodeData.name = event.target.value;
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
    if (this.selectedNodeData.image) {
      this.selectedNodeData.image.name = event.target.value;
      this.editNode.emit(this.selectedNodeData);
    }
}
  setImageArgs(event: any) {
    this.selectedNodeData.image.args = event
    this.editNode.emit(this.selectedNodeData);
  }
  setImageMegabytes(event: any) {
    if (this.selectedNodeData.image) {
      this.selectedNodeData.image.shmMegabytes = event.target.value;
      this.editNode.emit(this.selectedNodeData);
    }
  }

  setRequiredCPUs(event: any) {
    if (this.selectedNodeData.requiredResources) {
      this.selectedNodeData.requiredResources.cpus = event.target.value;
      this.editNode.emit(this.selectedNodeData);
    }
  }
  setRequiredRAM(event: any) {
    if (this.selectedNodeData.requiredResources) {
      this.selectedNodeData.requiredResources.megabytesOfRam = event.target.value;
      this.editNode.emit(this.selectedNodeData);
    }
  }
  setRequiredGPUs(event: any) {
    if (this.selectedNodeData.requiredResources) {
      this.selectedNodeData.requiredResources.gpu.count = event.target.value;
      this.editNode.emit(this.selectedNodeData);
    }
  }

  addNewLogParser(matcher: string, destination: string, formatter: string, type: string) {
    const newLogParser = new LogParserInfo({
      matcher: matcher,
      destination: destination,
      formatter: formatter,
      type: type
    })
    this.selectedNodeData.logParsers.push(newLogParser);
    this.emitSave();
  }

  protected readonly console = console;
}
