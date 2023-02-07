import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {MatDialog} from '@angular/material/dialog';
import {createRangedList, createRangeWithStepSize, createWorkspaceConfiguration, ProjectApiService,} from '../api/project-api.service';
import {parseArgsStringToArgv} from 'string-argv';
import {
  EnvVariable,
  ExecutionGroupInfo,
  ImageInfo,
  PipelineDefinitionInfo,
  RangedValueUnion,
  ResourceInfo,
  StageDefinitionInfo,
  StageWorkerDefinitionInfo,
  WorkspaceConfiguration,
  WorkspaceMode
} from '../api/winslow-api';


@Component({
  selector: 'app-stage-execution-selection',
  templateUrl: './stage-execution-selection.component.html',
  styleUrls: ['./stage-execution-selection.component.css']
})
export class StageExecutionSelectionComponent implements OnInit {

  rangeTypeRange = 'Range';
  rangeTypeList = 'List';
  rangeTypes: string[] = [this.rangeTypeRange, this.rangeTypeList];
  rangeType: string = this.rangeTypeRange;

  @Input() pipelines: PipelineDefinitionInfo[];
  @Input() pipelineSelectionDisabled = false;

  @Output('selectedPipeline') private selectedPipelineEmitter = new EventEmitter<PipelineDefinitionInfo>();
  @Output('selectedStage') private selectedStageEmitter = new EventEmitter<StageDefinitionInfo>();
  @Output('valid') private validEmitter = new EventEmitter<boolean>();

  defaultPipelineIdValue: string;
  executionHistoryValue: ExecutionGroupInfo[] = null;

  selectedPipeline: PipelineDefinitionInfo = null;
  selectedStage: StageWorkerDefinitionInfo = null;
  image = new ImageInfo({name: '', args: [], shmMegabytes: 0});

  resources = new ResourceInfo({cpus: 0, gpus: 0, megabytesOfRam: 100});

  workspaceConfiguration = createWorkspaceConfiguration();
  comment = null;
  valid = false;

  // env cache
  environmentVariablesValue: Map<string, EnvVariable> = null;
  defaultEnvironmentVariablesValue = new Map<string, string>();
  requiredEnvironmentVariables: string[];
  envSubmitValue: any = null;
  envValid = false;
  rangedEnvironmentVariablesValue: Map<string, RangedValueUnion> = null;
  rangedEnvironmentVariablesUpdated: Map<string, RangedValueUnion> = null;


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
  set rangedEnvironmentVariables(value: { [index: string]: any }) {
    this.rangedEnvironmentVariablesValue = new Map(value != null ? Object.entries(value) : []);
    this.rangedEnvironmentVariablesUpdated = new Map();
    this.updateValid();
  }


  @Input()
  set defaultEnvironmentVariables(value: { [index: string]: string }) {
    this.defaultEnvironmentVariablesValue = new Map(value != null ? Object.entries(value) : []);
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
      if (mode === 'CONTINUATION' && this.executionHistoryValue != null && this.executionHistoryValue.length > 0) {
        value = this.executionHistoryValue[0].id;
      }
      this.workspaceConfiguration = createWorkspaceConfiguration(
        mode,
        value,
        this.workspaceConfiguration?.sharedWithinGroup,
        this.workspaceConfiguration?.nestedWithinGroup
      );
    } else {

      this.workspaceConfiguration = createWorkspaceConfiguration();
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

  getComment(): string {
    return this.comment;
  }

  loadStagesForPipeline(pipelineId: string) {
    this.selectedPipeline = null;
    this.selectedPipelineEmitter.emit(null);
    this.selectedStage = null;
    this.selectedStageEmitter.emit(null);
    this.environmentVariables = null;
    this.comment = null;

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

  loadEnvForStageName(stageId: string) {
    if (this.selectedPipeline != null) {
      for (const stage of this.selectedPipeline.stages) {
        if (stage instanceof StageWorkerDefinitionInfo) {
          if (stage.id === stageId) {
            this.selectedStage = stage;
            this.selectedStageEmitter.emit(stage);
            this.image = StageExecutionSelectionComponent.deepClone(stage.image);
            this.resources = stage.requiredResources != null ? StageExecutionSelectionComponent.deepClone(stage.requiredResources) : null;

            const requiredEnvironmentVariables = [];
            this.selectedPipeline.userInput.requiredEnvVariables.forEach(key => requiredEnvironmentVariables.push(key));
            stage.userInput.requiredEnvVariables.forEach(key => requiredEnvironmentVariables.push(key));

            this.environmentVariables = new Map();
            this.requiredEnvironmentVariables = requiredEnvironmentVariables;
            break;
          }
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

  setWorkspaceMode(
    update: boolean,
    mode: WorkspaceMode,
    value: string = null,
    sharedWithinGroup: boolean = null,
    nestedWithinGroup: boolean = null
  ) {
    if (update) {
      this.workspaceConfiguration = createWorkspaceConfiguration(
        mode,
        value,
        sharedWithinGroup ?? this.workspaceConfiguration.sharedWithinGroup,
        nestedWithinGroup ?? this.workspaceConfiguration.nestedWithinGroup
      );
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
    this.setRangedWithStepSize(
      name.value.trim(),
      rangeStart.value.trim(),
      rangeEnd.value.trim(),
      stepSize.value.trim()
    );

    name.value = null;
    name.focus();
  }

  addRangedList(
    name: HTMLInputElement,
    values: HTMLInputElement,
  ) {
    this.setRangedList(name.value.trim(), values.value.trim());
    values.value = null;
    name.value = null;
    name.focus();
  }

  removeRangedEnvironmentVariable(key: string) {
    this.rangedEnvironmentVariablesValue?.delete(key);
    this.rangedEnvironmentVariablesUpdated?.delete(key);
  }

  setRangedWithStepSize(key: string, min: string, max: string, stepSize: string) {
    this.setRangedValue(key, createRangeWithStepSize(
      Number(min),
      Number(max),
      Number(stepSize)
    ));
  }

  setRangedList(key: string, listStringToParse: string) {
    this.setRangedValue(key, createRangedList(
      listStringToParse.split(',').map(v => v.trim())
    ));
  }

  setRangedValue(key: string, value: RangedValueUnion) {
    if (this.rangedEnvironmentVariablesUpdated == null) {
      this.rangedEnvironmentVariablesUpdated = new Map();
    }
    this.rangedEnvironmentVariablesUpdated.set(key, value);

    if (this.rangedEnvironmentVariablesValue == null) {
      this.rangedEnvironmentVariablesValue = new Map();
    }
    this.rangedEnvironmentVariablesValue.set(key, value);
  }

  expectedNumberOfStages(): number {
    let counter = 1;
    if (this.rangedEnvironmentVariablesUpdated != null) {
      for (const value of this.rangedEnvironmentVariablesUpdated.values()) {
        counter *= value.stepCount;
      }
    }
    if (this.rangedEnvironmentVariablesValue != null) {
      for (const entry of this.rangedEnvironmentVariablesValue.entries()) {
        if (this.rangedEnvironmentVariablesUpdated == null || !this.rangedEnvironmentVariablesUpdated.has(entry[0])) {
          counter *= entry[1].stepCount;
        }
      }
    }
    return Math.max(1, counter);
  }

  argvToString(args?: string[]): string {
    if (args != null) {
      return args
        .map(arg => {
          if (arg.indexOf(' ') >= 0) {
            return `"${arg}"`;
          } else {
            return arg;
          }
        })
        .join(' ');
    } else {
      return null;
    }
  }

  argvToStringComma(args?: string[]): string {
    if (args != null) {
      return args.join(', ');
    } else {
      return null;
    }
  }


}
