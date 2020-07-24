import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {PipelineInfo, ResourceInfo} from '../api/pipeline-api.service';
import {MatDialog} from '@angular/material';
import {
  EnvVariable,
  ExecutionGroupInfo,
  ImageInfo,
  ProjectApiService,
  RangedWithStepSize,
  StageDefinitionInfo,
  WorkspaceConfiguration,
  WorkspaceMode
} from '../api/project-api.service';
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
  @Output('selectedStage') private selectedStageEmitter = new EventEmitter<StageDefinitionInfo>();
  @Output('valid') private validEmitter = new EventEmitter<boolean>();

  defaultPipelineIdValue: string;
  executionHistoryValue: ExecutionGroupInfo[] = null;

  selectedPipeline: PipelineInfo = null;
  selectedStage: StageDefinitionInfo = null;
  image = new ImageInfo();
  resources = new ResourceInfo();
  workspaceConfiguration = new WorkspaceConfiguration();
  valid = false;

  // env cache
  environmentVariablesValue: Map<string, EnvVariable> = null;
  defaultEnvironmentVariablesValue = new Map<string, string>();
  requiredEnvironmentVariables: string[];
  envSubmitValue: any = null;
  envValid = false;
  rangedEnvironmentVariablesValue: Map<string, RangedWithStepSize> = null;
  rangedEnvironmentVariablesUpdated: Map<string, RangedWithStepSize> = null;


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
  set rangedEnvironmentVariables(map: Map<string, RangedWithStepSize>) {
    this.rangedEnvironmentVariablesValue = map;
    this.rangedEnvironmentVariablesUpdated = new Map();
    this.updateValid();
  }


  @Input()
  set defaultEnvironmentVariables(map: Map<string, string>) {
    this.defaultEnvironmentVariablesValue = map;
    this.updateValid();
  }

  @Input()
  set executionHistory(history: ExecutionGroupInfo[]) {
    this.executionHistoryValue = history;
    this.workspaceConfigurationMode = this.workspaceConfiguration?.mode;
  }

  @Input()
  set workspaceConfigurationMode(mode: WorkspaceMode) {
    if (mode != null) {
      let value = null;
      if (mode === WorkspaceMode.CONTINUATION && this.executionHistoryValue != null && this.executionHistoryValue.length > 0) {
        value = this.executionHistoryValue[0].id;
      }
      this.workspaceConfiguration = new WorkspaceConfiguration(mode, value, this.workspaceConfiguration?.sharedWithinGroup);
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

  getRangedEnv() {
    const fakeMap = {};
    if (this.rangedEnvironmentVariablesValue != null) {
      for (const key of this.rangedEnvironmentVariablesValue.keys()) {
        fakeMap[key] = this.rangedEnvironmentVariablesValue.get(key);
      }
    }
    if (this.rangedEnvironmentVariablesUpdated != null) {
      for (const key of this.rangedEnvironmentVariablesUpdated.keys()) {
        fakeMap[key] = this.rangedEnvironmentVariablesUpdated.get(key);
      }
    }
    return fakeMap;
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

  setWorkspaceMode(update: boolean, mode: WorkspaceMode, value: string = null, sharedWithinGroup: boolean = null) {
    if (update) {
      this.workspaceConfiguration = new WorkspaceConfiguration(mode, value, sharedWithinGroup ?? this.workspaceConfiguration.sharedWithinGroup);
    }
  }

  tryParseStageNumber(stageId: string, alt: number): number {
    return this.api.tryParseGroupNumber(stageId, alt);
  }

  addRangedEnvironmentVariable(
    name: HTMLInputElement,
    rangeStart: HTMLInputElement,
    rangeEnd: HTMLInputElement,
    stepSize: HTMLInputElement
  ) {
    if (this.rangedEnvironmentVariablesValue == null) {
      this.rangedEnvironmentVariablesValue = new Map();
    }
    const range = new RangedWithStepSize();
    range.min = Number(rangeStart.value.trim());
    range.max = Number(rangeEnd.value.trim());
    range.stepSize = Number(stepSize.value.trim());
    this.rangedEnvironmentVariablesValue.set(name.value.trim(), range);

    name.value = null;
    name.focus();
  }

  removeRangeDEnvironmentVariable(key: string) {
    if (this.rangedEnvironmentVariablesValue != null) {
      this.rangedEnvironmentVariablesValue.delete(key);
      this.rangedEnvironmentVariablesUpdated.delete(key);
    }
  }

  updateRangedEnv(key: string, min: string, max: string, stepSize: string) {
    const range = new RangedWithStepSize();
    range.min = Number(min);
    range.max = Number(max);
    range.stepSize = Number(stepSize);
    this.rangedEnvironmentVariablesUpdated.set(key, range);
  }

  expectedNumberOfStages(): number {
    let counter = 1;
    if (this.rangedEnvironmentVariablesUpdated != null) {
      for (const value of this.rangedEnvironmentVariablesUpdated.values()) {
        counter *= this.getStageCount(value);
      }
    }
    if (this.rangedEnvironmentVariablesValue != null) {
      for (const entry of this.rangedEnvironmentVariablesValue.entries()) {
        if (this.rangedEnvironmentVariablesUpdated == null || !this.rangedEnvironmentVariablesUpdated.has(entry[0])) {
          counter *= this.getStageCount(entry[1]);
        }
      }
    }
    return Math.max(1, counter);
  }

  private getStageCount(value: RangedWithStepSize): number {
    const min = Math.min(value.min, value.max);
    const max = Math.max(value.min, value.max);
    const stp = Math.abs(value.stepSize);
    const dist = (max - min);
    return Math.ceil(dist / stp) + 1;
  }
}
