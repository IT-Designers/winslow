import {Component, OnInit} from '@angular/core';
import {PipelineApiService, PipelineInfo} from '../api/pipeline-api.service';
import {NotificationService} from '../notification.service';
import {LongLoadingDetector} from '../long-loading-detector';
import {MatDialog, MatDialogRef} from '@angular/material';
import {CreatePipelineDialogComponent, CreatePipelineResult} from '../pipeline-create-dialog/create-pipeline-dialog.component';

@Component({
  selector: 'app-pipelines',
  templateUrl: './pipelines.component.html',
  styleUrls: ['./pipelines.component.css']
})
export class PipelinesComponent implements OnInit {
  pipelines: PipelineInfo[] = null;
  loadError = null;

  raw: Map<string, string> = new Map();
  error: Map<string, string> = new Map();
  success: Map<string, string> = new Map();

  longLoading = new LongLoadingDetector();

  constructor(
    private api: PipelineApiService,
    private notification: NotificationService,
    private dialog: MatDialog) {
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
    this.api
      .getRaw(pipeline)
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
          this.error.set(pipeline, result);
        } else {
          this.error.delete(pipeline);
          this.success.set(pipeline, 'Looks fine!');
        }
      })
      .catch(error => this.notification.error('Request failed: ' + error))
      .finally(() => this.longLoading.decrease());
  }

  update(pipeline: string, value: string) {
    this.longLoading.increase();
    this.api
      .updatePipelineDefinition(pipeline, value)
      .then(result => {
        if (result != null) {
          this.success.delete(pipeline);
          this.error.set(pipeline, result);
        } else {
          this.error.delete(pipeline);
          this.success.set(pipeline, 'Saved!');
          return this.api
            .getPipelineDefinition(pipeline)
            .then(def => {
              for (const pipe of this.pipelines) {
                if (pipeline === pipe.id) {
                  // migrate values without replacing the object to avoid the list of pipelines to be rebuilt
                  Object.keys(def).forEach(key => pipe[key] = def[key]);
                  this.raw.set(pipeline, value);
                  break;
                }
              }
            });
        }
      })
      .catch(error => this.notification.error('Request failed: ' + error))
      .finally(() => this.longLoading.decrease());
  }

  openCreatePipelineDialog() {
    const dialog: MatDialogRef<CreatePipelineDialogComponent, CreatePipelineResult> = this.dialog.open(CreatePipelineDialogComponent, {});
    dialog
      .afterClosed()
      .subscribe(result => {
        if (result) {
          alert(JSON.stringify(result));
        }
      });
  }
}
