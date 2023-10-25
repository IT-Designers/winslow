import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {createRangedList, createRangeWithStepSize, createWorkspaceConfiguration, ProjectApiService,} from '../api/project-api.service';

import {
  EnvVariable,
  ExecutionGroupInfo,
  ImageInfo, isRangedList, isRangeWithStepSize,
  PipelineDefinitionInfo,
  RangedValueUnion,
  ResourceInfo,
  StageDefinitionInfo,
  StageWorkerDefinitionInfo,
  WorkspaceConfiguration,
  WorkspaceMode
} from '../api/winslow-api';
import {parseArgsStringToArgv} from "string-argv";


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

  @Input() pipelines: PipelineDefinitionInfo[] = [];
  @Input() pipelineSelectionDisabled = false;

  @Output('selectedPipeline') private selectedPipelineEmitter = new EventEmitter<PipelineDefinitionInfo>();
  @Output('selectedStage') private selectedStageEmitter = new EventEmitter<StageDefinitionInfo>();
  @Output('valid') private validEmitter = new EventEmitter<boolean>();

  defaultPipelineIdValue?: string;
  executionHistoryValue?: ExecutionGroupInfo[];

  selectedPipeline?: PipelineDefinitionInfo;
  selectedStage?: StageWorkerDefinitionInfo;
  image = new ImageInfo({name: '', args: [], shmMegabytes: 0});

  resources = new ResourceInfo({cpus: 0, gpus: 0, megabytesOfRam: 100});

  workspaceConfiguration = createWorkspaceConfiguration();
  comment: string = "";
  valid = false;

  // env cache
  environmentVariablesValue?: Map<string, EnvVariable>;
  defaultEnvironmentVariablesValue = new Map<string, string>();
  requiredEnvironmentVariables?: string[];
  envSubmitValue: any = null;
  envValid = false;
  rangedEnvironmentVariablesValue?: Map<string, RangedValueUnion>;
  rangedEnvironmentVariablesUpdated?: Map<string, RangedValueUnion>;


  static deepClone(image: object) {
    return JSON.parse(JSON.stringify(image));
  }

  constructor(private api: ProjectApiService) {
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
  set environmentVariables(map: Map<string, EnvVariable> | undefined) {
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
      let value = undefined;
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
    const fakeMap: Record<string, RangedValueUnion | undefined> = {};
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
    return this.comment ?? "";
  }

  loadStagesForPipeline(pipelineId: string) {
    this.selectedPipeline = undefined;
    this.selectedPipelineEmitter.emit(undefined);
    this.selectedStage = undefined;
    this.selectedStageEmitter.emit(undefined);
    this.environmentVariables = undefined;
    this.comment = "";

    for (const pipeline of this.pipelines) {
      if (pipeline.id === pipelineId) {
        this.selectedPipeline = pipeline;
        this.selectedPipelineEmitter.emit(pipeline);
        this.selectedStage = undefined;
        this.selectedStageEmitter.emit(undefined);
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

            const requiredEnvironmentVariables: string[] = [];
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

  updateValidEnv(valid: boolean) {
    this.envValid = valid;
    this.updateValid();
  }

  setWorkspaceMode(
    update: boolean,
    mode?: WorkspaceMode,
    value?: string,
    sharedWithinGroup?: boolean,
    nestedWithinGroup?: boolean
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

    name.value = '';
    name.focus();
  }

  addRangedList(
    name: HTMLInputElement,
    values: HTMLInputElement,
  ) {
    this.setRangedList(name.value.trim(), values.value.trim());
    values.value = '';
    name.value = '';
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
      return '';
    }
  }

  argvToStringComma(args?: string[]): string {
    if (args != null) {
      return args.join(', ');
    } else {
      return '';
    }
  }


  onImageValueChanged(event: Event) {
    const target = event.target;
    if (target instanceof HTMLInputElement) {
      this.image.name = target.value
    }
    this.updateValid();
  }

  onImageArgsChanged(event: Event) {
    const target = event.target;
    if (target instanceof HTMLInputElement) {
      this.image.args = parseArgsStringToArgv(target.value);
    }
    this.updateValid();
  }

  onCommentChanged(event: Event) {
    const target = event.target;
    if (target instanceof HTMLInputElement) {
      this.comment = target.value;
    }
  }

  onCpusChanged(event: Event) {
    const target = event.target;
    if (target instanceof HTMLInputElement) {
      this.resources.cpus = Number(target.value);
    }
    this.updateValid();
  }

  onRamChanged(event: Event) {
    const target = event.target;
    if (target instanceof HTMLInputElement) {
      this.resources.megabytesOfRam = Number(target.value);
    }
    this.updateValid();
  }

  onGpusChanged(event: Event) {
    const target = event.target;
    if (target instanceof HTMLInputElement) {
      this.resources.gpus = Number(target.value);
    }
    this.updateValid();
  }

  getZerothExecutionHistoryValue() {
    return this.executionHistoryValue ? this.executionHistoryValue[0] : undefined;
  }

  protected readonly isRangeWithStepSize = isRangeWithStepSize;
  protected readonly isRangedList = isRangedList;
}
