import {Component, EventEmitter, Input, OnChanges, OnInit, Output} from '@angular/core';
import {Link, ParseError, PipelineDefinitionInfo} from "../../api/winslow-api";
import {DialogService} from "../../dialog.service";
import {PipelineApiService} from "../../api/pipeline-api.service";
import {LongLoadingDetector} from "../../long-loading-detector";
import {PipelineEditorComponent} from "../../pipeline-editor/pipeline-editor.component";

@Component({
  selector: 'app-pipeline-details',
  templateUrl: './pipeline-details.component.html',
  styleUrls: ['./pipeline-details.component.css']
})
export class PipelineDetailsComponent implements OnInit, OnChanges {

  @Input() selectedPipeline!: PipelineDefinitionInfo;

  @Output() pipelineDeleteEmitter = new EventEmitter<PipelineDefinitionInfo>();

  mockGroups = [
    {
      name: 'NewGroup1',
      role: 'OWNER'
    },
    {
      name: 'NewGroup2',
      role: 'MEMBER'
    },
    {
      name: 'NewGroup3',
      role: 'OWNER'
    }
  ];

  rawPipelineDefinition: string | null = null;
  rawPipelineDefinitionError: string | null = null;
  rawPipelineDefinitionSuccess: string | null = null;

  longLoading = new LongLoadingDetector();

  constructor(private dialog: DialogService, private pipelinesApi: PipelineApiService) {
  }

  ngOnInit(): void {
    this.loadRawPipelineDefinition();
  }

  ngOnChanges() {
    this.loadRawPipelineDefinition();
  }

  loadRawPipelineDefinition() {
    this.dialog.openLoadingIndicator(
      this.pipelinesApi.getRawPipelineDefinition(this.selectedPipeline.id)
        .then(result => this.rawPipelineDefinition = result),
      `Loading Pipeline Definition`,
      false
    );
  }

  checkPipelineDefinition(raw: string) {
    this.dialog.openLoadingIndicator(
      this.pipelinesApi.checkPipelineDefinition(raw)
        .then(result => {
          if (result instanceof ParseError) {
            this.rawPipelineDefinitionSuccess = null;
            this.rawPipelineDefinitionError = '' + result.message; //TODO Datatype same as in project-view
          } else {
            this.rawPipelineDefinitionSuccess = 'Looks good!';
            this.rawPipelineDefinitionError = null;
          }
        }),
      `Checking Pipeline Definition`,
      false
    );
  }

  updatePipelineDefinition(raw: string, editor: PipelineEditorComponent) {
    this.dialog.openLoadingIndicator(
      this.pipelinesApi.updatePipelineDefinition(this.selectedPipeline.id, raw)
        .catch(e => {
          editor.parseError = [e];
          return Promise.reject('Failed to parse input, see marked area(s) for more details');
        })
        .then(_r => {
          editor.parseError = [];
          return this.pipelinesApi
            .getPipelineDefinition(this.selectedPipeline.id)
            .then(definition => {
              this.selectedPipeline = definition;
            });
        }),
      `Saving Pipeline Definition`,
      true
    );
  }

  isLongLoading() {
    return this.longLoading.isLongLoading();
  }


  setPipelineDefinition(pipeline: PipelineDefinitionInfo) {
    this.dialog.openLoadingIndicator(this.pipelinesApi.setPipelineDefinition(pipeline),
      'Updating Pipeline with new definition');
  }

  onGroupAdd(event: Link): void {
    this.selectedPipeline.groups.push(event);
    this.dialog.openLoadingIndicator(this.pipelinesApi.setPipelineDefinition(this.selectedPipeline),
      'Updating Pipeline with new group')
  }

  onGroupRemove(event: Link): void {
    const delIndex = this.selectedPipeline.groups.findIndex((group) => group.name === event.name)
    this.selectedPipeline.groups.splice(delIndex, 1);
    this.dialog.openLoadingIndicator(this.pipelinesApi.setPipelineDefinition(this.selectedPipeline),
      'Removing group from Pipeline');

  }

  setName(name?: string): void {
    if (name) {
      this.selectedPipeline.name = name;
      this.dialog.openLoadingIndicator(this.pipelinesApi.setPipelineDefinition(this.selectedPipeline),
        'Updating Pipeline name');
    }
  }

  setDescription(description?: string): void {
    if (description) {
      this.selectedPipeline.description = description;
      this.dialog.openLoadingIndicator(this.pipelinesApi.setPipelineDefinition(this.selectedPipeline),
        'Updating Pipeline Description');
    }
  }

  delete() {
    this.pipelineDeleteEmitter.emit(this.selectedPipeline);
  }

  onUpdatePipeline() {
    console.log('Update the pipeline!');
  }

}
