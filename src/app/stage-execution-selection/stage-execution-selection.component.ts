import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {PipelineInfo, StageInfo} from '../api/pipeline-api.service';
import {FileBrowseDialog} from '../file-browse-dialog/file-browse-dialog.component';
import {MatDialog} from '@angular/material';
import {FormControl, FormGroup} from '@angular/forms';

@Component({
  selector: 'app-stage-execution-selection',
  templateUrl: './stage-execution-selection.component.html',
  styleUrls: ['./stage-execution-selection.component.css']
})
export class StageExecutionSelectionComponent implements OnInit {

  @Input() pipelines: PipelineInfo[];

  @Output('selectedPipeline') private selectedPipelineEmitter = new EventEmitter<PipelineInfo>();
  @Output('selectedStage') private selectedStageEmitter = new EventEmitter<StageInfo>();

  defaultPipelineIdValue: string;

  selectedPipeline: PipelineInfo = null;
  selectedStage: StageInfo = null;
  environmentVariables: Map<string, [boolean, string]> = null;

  defaultEnvironmentVariablesValue = new Map<string, string>();

  formGroupEnv = new FormGroup({});


  constructor(private dialog: MatDialog) {

  }

  ngOnInit() {
  }

  isValid(): boolean {
    return this.selectedPipeline != null && this.selectedStage != null && this.formGroupEnv && this.formGroupEnv.valid;
  }

  @Input()
  set defaultPipelineId(id: string) {
    this.defaultPipelineIdValue = id;
    this.loadStagesForPipeline(id);
  }

  @Input()
  set defaultEnvironmentVariables(map: Map<string, string>) {
    this.defaultEnvironmentVariablesValue = map;
    if (map != null) {
      map.forEach((value, key) => this.setEnvValue(key, value));
      setTimeout(() => this.formGroupEnv.markAllAsTouched());
    }
  }

  setEnvValue(key: string, value: string) {
    this.prepareEnvFormControl(key, value);
    const current = this.environmentVariables.get(key);
    if (current != null) {
      this.environmentVariables.set(key, [current[0], value]);
    } else {
      this.environmentVariables.set(key, [false, value]);
    }
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

          this.formGroupEnv = new FormGroup({});
          this.environmentVariables = new Map();
          this.selectedPipeline.requiredEnvVariables.forEach(key => this.setEnvRequired(key));
          this.selectedStage.requiredEnvVariables.forEach(key => this.setEnvRequired(key));
          setTimeout(() => this.formGroupEnv.markAllAsTouched());

          break;
        }
      }
    }
  }

  setEnvRequired(key: string) {
    this.prepareEnvFormControl(key, null);
    const value = this.environmentVariables.get(key);
    if (value != null) {
      this.environmentVariables.set(key, [true, value[1]]);
    } else {
      this.environmentVariables.set(key, [true, null]);
    }
  }

  prepareEnvFormControl(key: string, value: string) {
    const control = this.formGroupEnv.get(key);
    if (control == null) {
      this.formGroupEnv.setControl(key, new FormControl(value));
    } else {
      control.setValue(value);
      control.updateValueAndValidity();
    }
  }

  getSelectedImageArgs(): string {
    return this.selectedStage.image.args.join(' ');
  }

  browseForValue(valueReceiver: HTMLInputElement) {
    this.dialog.open(FileBrowseDialog, {
      data: {
        preselectedPath: valueReceiver.value.trim().length > 0 ? valueReceiver.value.trim() : null
      }
    })
      .afterClosed()
      .toPromise()
      .then(result => {
        if (result) {
          valueReceiver.value = result;
        }
      });
  }
}
