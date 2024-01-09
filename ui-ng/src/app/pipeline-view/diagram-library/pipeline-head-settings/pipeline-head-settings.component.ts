import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {PipelineDefinitionInfo} from "../../../api/winslow-api";

@Component({
  selector: 'app-pipeline-head-settings',
  templateUrl: './pipeline-head-settings.component.html',
  styleUrl: './pipeline-head-settings.component.css'
})
export class PipelineHeadSettingsComponent implements OnInit {
  @Input() selectedNodeData: PipelineDefinitionInfo = {} as PipelineDefinitionInfo;
  @Output() editNode = new EventEmitter();

  ngOnInit() {
    console.dir(this.selectedNodeData.environment);
  }

  emitSave() {
    this.editNode.emit(this.selectedNodeData);
  }

  protected readonly Number = Number;
}
