import {Component, OnInit} from '@angular/core';
import {PipelineApiService} from '../api/pipeline-api.service';
import {NotificationService} from '../notification.service';
import {LongLoadingDetector} from '../long-loading-detector';
import {MatDialog, MatDialogRef} from '@angular/material/dialog';
import {CreatePipelineDialogComponent, CreatePipelineResult} from '../pipeline-create-dialog/create-pipeline-dialog.component';
import {ParseError, PipelineDefinitionInfo} from '../api/winslow-api';
import {DialogService} from "../dialog.service";
import {AddPipelineDialogComponent} from "./add-pipeline-dialog/add-pipeline-dialog.component";

@Component({
  selector: 'app-pipelines',
  templateUrl: './pipelines.component.html',
  styleUrls: ['./pipelines.component.css']
})
export class PipelinesComponent implements OnInit {
  pipelines: PipelineDefinitionInfo[] = null;
  loadError = null;

  raw: Map<string, string> = new Map();
  parseError: Map<string, ParseError[]> = new Map();
  error: Map<string, string> = new Map();
  success: Map<string, string> = new Map();

  longLoading = new LongLoadingDetector();

  selectedPipeline: PipelineDefinitionInfo = null;


  constructor(
    private api: PipelineApiService,
    private notification: NotificationService,
    private createDialog: MatDialog,
    private dialog: DialogService) {
  }

  ngOnInit() {
    this.api
      .getPipelineDefinitions()
      .then(r => {
        this.pipelines = r;
        this.pipelines.forEach(p => this.loadRaw(p.id));
      })
      .catch(error => this.loadError = error);
  }

  loadRaw(pipeline: string) {
    return this.api
      .getRawPipelineDefinition(pipeline)
      .then(raw => {
        this.raw.set(pipeline, raw);
      });
  }

  check(pipeline: string, value: string) {
    this.longLoading.increase();
    this.api
      .checkPipelineDefinition(value)
      .then(result => {
        if (result != null) {
          this.success.delete(pipeline);
          if (typeof result === typeof '') {
            this.error.set(pipeline, result as string);
          } else {
            this.error.set(pipeline, 'There is at least one error!');
            this.parseError.set(pipeline, [result as ParseError]);
          }
        } else {
          this.error.delete(pipeline);
          this.parseError.delete(pipeline);
          this.success.set(pipeline, 'Looks fine!');
        }
      })
      .catch(error => this.notification.error('Request failed: ' + error))
      .finally(() => this.longLoading.decrease());
  }

  update(pipeline: string, value: string) {
    this.longLoading.increase();
    this.api
      .updatePipelineDefinition(pipelineId, value)
      .then(result => {
        this.success.delete(pipelineId);
        this.error.delete(pipelineId);
        this.parseError.set(pipelineId, []);

        if (result != null) {
          if (typeof result === typeof '') {
            this.error.set(pipelineId, result as string);
          } else {
            this.error.set(pipelineId, 'There is at least one error!');
            this.parseError.set(pipelineId, [result as ParseError]);
          }
        } else {
          this.success.set(pipelineId, 'Saved!');
          return this.api
            .getPipelineDefinition(pipelineId)
            .then(source => {
              const target = this.pipelines.find(def => def.id == pipelineId)
              if (target != undefined) {
                // migrate values without replacing the object to avoid the list of pipelines to be rebuilt
                Object.assign(source, target);
                this.raw.set(pipelineId, value);
              }
            });
        }
      })
      .catch(error => this.notification.error('Request failed: ' + error))
      .finally(() => this.longLoading.decrease());
  }

  onPipelineClicked(pipeline) {
    this.selectedPipeline = pipeline;
  }

  openCreatePipelineDialog(): void {
    this.createDialog.open(AddPipelineDialogComponent, {
      data: {} as string
    })
      .afterClosed()
      .subscribe((name) => {
        this.dialog.openLoadingIndicator(this.api.createPipelineDefinition(name)
            .then((newPipeline) => {
              this.pipelines.push(newPipeline);
              this.pipelines = this.pipelines.concat([]);
              this.selectedPipeline = newPipeline;
            }),
          'Creating Pipeline');
      });
  }

  onDeletePipeline(event) {
    this.dialog.openAreYouSure(`Pipeline being deleted ${this.selectedPipeline.name}`,
      () => this.api.deletePipeline(this.selectedPipeline.id)
        .then(() => {
          let delIndex = this.pipelines.findIndex((tempPipeline) => tempPipeline.id === this.selectedPipeline.id);
          this.pipelines.splice(delIndex, 1);
          this.pipelines = this.pipelines.concat([]);
          this.selectedPipeline = null;
        }
        ));
  }

}

