import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {PipelineInfo, StageInfo} from '../api/pipeline-api.service';
import {MatDialog} from '@angular/material';
import {ImageInfo} from '../api/project-api.service';
import {parseArgsStringToArgv} from 'string-argv';

@Component({
  selector: 'app-stage-execution-selection',
  templateUrl: './stage-execution-selection.component.html',
  styleUrls: ['./stage-execution-selection.component.css']
})
export class StageExecutionSelectionComponent implements OnInit {

  @Input() pipelines: PipelineInfo[];
  @Input() pipelineSelectionDisabled = false;

  @Output('selectedPipeline') private selectedPipelineEmitter = new EventEmitter<PipelineInfo>();
  @Output('selectedStage') private selectedStageEmitter = new EventEmitter<StageInfo>();

  defaultPipelineIdValue: string;

  selectedPipeline: PipelineInfo = null;
  selectedStage: StageInfo = null;
  image = new ImageInfo();
  valid = false;

  // env cache
  environmentVariables: Map<string, [boolean, string]> = null;
  defaultEnvironmentVariablesValue = new Map<string, string>();
  requiredEnvironmentVariables: string[];
  envSubmitValue: any = null;


  static deepClone(image: ImageInfo) {
    return JSON.parse(JSON.stringify(image));
  }

  constructor(private dialog: MatDialog) {
  }

  ngOnInit() {
  }

  @Input()
  set defaultPipelineId(id: string) {
    this.defaultPipelineIdValue = id;
    this.loadStagesForPipeline(id);
  }

  @Input()
  set defaultEnvironmentVariables(map: Map<string, string>) {
    this.defaultEnvironmentVariablesValue = map;
  }

  updateValid(envValid: boolean) {
    this.valid = this.isValid() && envValid;
  }

  isValid(): boolean {
    return this.selectedPipeline != null && this.selectedStage != null;
  }

  getEnv() {
    return this.envSubmitValue;
  }

  getImage(): ImageInfo {
    return this.image;
  }

  loadStagesForPipeline(pipelineId: string) {
    this.selectedPipeline = null;
    this.selectedPipelineEmitter.emit(null);
    this.selectedStage = null;
    this.selectedStageEmitter.emit(null);
    this.environmentVariables = null;

    for (const pipeline of this.pipelines) {
      if (pipeline.id === pipelineId) {
        this.selectedPipeline = pipeline;
        this.selectedPipelineEmitter.emit(pipeline);
        this.selectedStage = null;
        this.selectedStageEmitter.emit(null);
        break;
      }
    }
  }

  loadEnvForStageName(stageName: string) {
    if (this.selectedPipeline != null) {
      for (const stage of this.selectedPipeline.stages) {
        if (stage.name === stageName) {
          this.selectedStage = stage;
          this.selectedStageEmitter.emit(stage);
          this.image = StageExecutionSelectionComponent.deepClone(stage.image);

          const requiredEnvironmentVariables = [];
          this.selectedPipeline.requiredEnvVariables.forEach(key => requiredEnvironmentVariables.push(key));
          this.selectedStage.requiredEnvVariables.forEach(key => requiredEnvironmentVariables.push(key));

          this.environmentVariables = new Map();
          this.requiredEnvironmentVariables = requiredEnvironmentVariables;
          break;
        }
      }
    }
  }

  updateImageArgs(value: string) {
    this.image.args = parseArgsStringToArgv(value);
    this.updateValid();
  }
}
