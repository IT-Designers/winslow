import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {Link, PipelineDefinitionInfo} from "../../api/winslow-api";
import {DialogService} from "../../dialog.service";
import {PipelineApiService} from "../../api/pipeline-api.service";
import {LongLoadingDetector} from "../../long-loading-detector";

@Component({
  selector: 'app-pipeline-details',
  templateUrl: './pipeline-details.component.html',
  styleUrls: ['./pipeline-details.component.css']
})
export class PipelineDetailsComponent implements OnInit {

  @Input() selectedPipeline: PipelineDefinitionInfo = null;
  @Input() myUser: Link;

  @Output() pipelineDeleteEmitter = new EventEmitter();

  longLoading = new LongLoadingDetector();
  constructor(private dialog: DialogService, private pipelineApi: PipelineApiService) { }

  ngOnInit(): void {
  }

  isLongLoading() {
    return this.longLoading.isLongLoading();
  }

  setName(name) {
    console.log('Change Name to: ' + name);
  }

  delete() {
    console.log('Delete');
  }

  onUpdatePipeline() {
    console.log('Update the pipeline!');
  }

}
