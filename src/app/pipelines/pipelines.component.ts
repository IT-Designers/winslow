import {Component, OnInit} from '@angular/core';
import {PipelineApiService} from '../api/pipeline-api.service';
import {NotificationService} from '../notification.service';
import {LongLoadingDetector} from '../long-loading-detector';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import {CreatePipelineDialogComponent, CreatePipelineResult} from '../pipeline-create-dialog/create-pipeline-dialog.component';
import {ParseError} from '../api/project-api.service';
import {IPipelineInfo} from '../api/winslow-api';

@Component({
  selector: 'app-pipelines',
  templateUrl: './pipelines.component.html',
  styleUrls: ['./pipelines.component.css']
})
export class PipelinesComponent implements OnInit {
  pipelines: IPipelineInfo[] = null;
  loadError = null;

  raw: Map<string, string> = new Map();
  parseError: Map<string, ParseError[]> = new Map();
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
    return this.api
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
      .updatePipelineDefinition(pipeline, value)
      .then(result => {
        this.success.delete(pipeline);
        this.error.delete(pipeline);
        this.parseError.set(pipeline, []);

        if (result != null) {
          if (ParseError.canShadow(result)) {
            this.parseError.set(pipeline, [result as ParseError]);
          } else {
            this.error.set(pipeline, result as string);
          }
        } else {
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
          this.longLoading.increase();
          return this.api
            .createPipelineDefinition(result.name)
            .then(info => {
              if (info) {
                return this.loadRaw(info.id)
                  .then(loaded => this.pipelines.push(info));
              } else {
                this.notification.error('Request failed');
              }
            })
            .finally(() => this.longLoading.decrease());
        }
      });
  }
}
