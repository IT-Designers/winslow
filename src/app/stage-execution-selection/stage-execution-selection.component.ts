import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {PipelineInfo, StageInfo} from '../api/pipeline-api.service';
import {FileBrowseDialog} from '../file-browse-dialog/file-browse-dialog.component';
import {MatDialog} from '@angular/material';
import {AbstractControl, FormBuilder, FormControl, FormGroup, Validators} from '@angular/forms';

@Component({
  selector: 'app-stage-execution-selection',
  templateUrl: './stage-execution-selection.component.html',
  styleUrls: ['./stage-execution-selection.component.css']
})
export class StageExecutionSelectionComponent implements OnInit {

  @Input() pipelines: PipelineInfo[];

  @Output('selectedPipeline') private selectedPipelineEmitter = new EventEmitter<PipelineInfo>();
  @Output('selectedStage') private selectedStageEmitter = new EventEmitter<StageInfo>();

  selectedPipeline: PipelineInfo = null;
  selectedStage: StageInfo = null;
  environmentVariables: Map<string, [boolean, string]> = null;

  formGroup: FormGroup;

  constructor(private dialog: MatDialog) {
    class DynamicFormGroup extends FormGroup {
      get(path: Array<string | number> | string): AbstractControl | null {
        let control = super.get(path);
        if (control == null) {
          super.addControl(String(path), new FormControl(null, Validators.required));
          control = super.get(path);
          control.markAllAsTouched();
        }
        return control;
      }
    }
    this.formGroup = new DynamicFormGroup({});
  }

  ngOnInit() {
  }

  get valid(): boolean {
    return this.formGroup.valid;
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

          this.environmentVariables = new Map();
          this.selectedPipeline.requiredEnvVariables.forEach(env => this.environmentVariables.set(env, [true, null]));
          this.selectedStage.requiredEnvVariables.forEach(env => this.environmentVariables.set(env, [true, null]));

          break;
        }
      }
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
