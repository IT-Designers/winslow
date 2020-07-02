import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {PipelineInfo, ResourceInfo, StageInfo} from '../api/pipeline-api.service';
import {MatDialog} from '@angular/material';
import {EnvVariable, HistoryEntry, ImageInfo, ProjectApiService, WorkspaceConfiguration, WorkspaceMode} from '../api/project-api.service';
import {parseArgsStringToArgv} from 'string-argv';

@Component({
  selector: 'app-stage-execution-selection',
  templateUrl: './stage-execution-selection.component.html',
  styleUrls: ['./stage-execution-selection.component.css']
})
export class StageExecutionSelectionComponent implements OnInit {

  WorkspaceMode = WorkspaceMode;

  @Input() pipelines: PipelineInfo[];
  @Input() pipelineSelectionDisabled = false;


  @Output('selectedPipeline') private selectedPipelineEmitter = new EventEmitter<PipelineInfo>();
  @Output('selectedStage') private selectedStageEmitter = new EventEmitter<StageInfo>();

  defaultPipelineIdValue: string;
  executionHistoryValue: HistoryEntry[] = null;

  selectedPipeline: PipelineInfo = null;
  selectedStage: StageInfo = null;
  image = new ImageInfo();
  resources = new ResourceInfo();
  workspaceConfiguration = new WorkspaceConfiguration();
  @Output('valid') private validEmitter = new EventEmitter<boolean>();
  valid = false;

  // env cache
  environmentVariablesValue: Map<string, EnvVariable> = null;
  defaultEnvironmentVariablesValue = new Map<string, string>();
  requiredEnvironmentVariables: string[];
  envSubmitValue: any = null;
  envValid = false;


  static deepClone(image: object) {
    return JSON.parse(JSON.stringify(image));
  }

  constructor(private dialog: MatDialog,
              private api: ProjectApiService) {
    this.updateValid();
  }

  ngOnInit() {
  }

  @Input()
  set defaultPipelineId(id: string) {
    this.defaultPipelineIdValue = id;
    this.loadStagesForPipeline(id);
  }

  @Input()
  set environmentVariables(map: Map<string, EnvVariable>) {
    this.environmentVariablesValue = map;
    this.updateValid();
  }


  @Input()
  set defaultEnvironmentVariables(map: Map<string, string>) {
    this.defaultEnvironmentVariablesValue = map;
    this.updateValid();
  }

  @Input()
  set executionHistory(history: HistoryEntry[]) {
    this.executionHistoryValue = history;
    this.workspaceConfigurationMode = this.workspaceConfiguration?.mode;
  }

  @Input()
  set workspaceConfigurationMode(mode: WorkspaceMode) {
    if (mode != null) {
      let value = null;
      if (mode === WorkspaceMode.CONTINUATION && this.executionHistoryValue != null && this.executionHistoryValue.length > 0) {
        value = this.executionHistoryValue[0].stageId;
      }
      this.workspaceConfiguration = new WorkspaceConfiguration(mode, value);
    } else {
      this.workspaceConfiguration = new WorkspaceConfiguration();
    }
  }

  updateValid() {
    this.validEmitter.emit(this.valid = this.isValid());
  }

  isValid(): boolean {
    return this.selectedPipeline != null && this.selectedStage != null && this.envValid;
  }

  getEnv() {
    return this.envSubmitValue;
  }

  getImage(): ImageInfo {
    return this.image;
  }

  getResourceRequirements(): ResourceInfo {
    return this.resources;
  }

  getWorkspaceConfiguration(): WorkspaceConfiguration {
    return this.workspaceConfiguration;
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
          this.resources = stage.requiredResources != null ? StageExecutionSelectionComponent.deepClone(stage.requiredResources) : null;

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

  updateValidEnv(valid: boolean) {
    this.envValid = valid;
    this.updateValid();
  }

  toNumber(value: string): number {
    return Number(value);
  }

  setWorkspaceMode(checked: boolean, mode: WorkspaceMode, value: string = null) {
    if (checked) {
      this.workspaceConfiguration = new WorkspaceConfiguration(mode, value);
      console.log(JSON.stringify(this.workspaceConfiguration));
    }
  }

  tryParseStageNumber(stageId: string, alt: number): number {
    return this.api.tryParseStageNumber(stageId, alt);
  }

  hasStageId(e: HistoryEntry): boolean {
    return e.stageId != null;
  }

}
