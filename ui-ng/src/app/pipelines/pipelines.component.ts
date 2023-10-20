import {Component, OnInit} from '@angular/core';
import {PipelineApiService} from '../api/pipeline-api.service';
import {NotificationService} from '../notification.service';
import {LongLoadingDetector} from '../long-loading-detector';
import {ParseError, PipelineDefinitionInfo} from '../api/winslow-api';
import {DialogService} from "../dialog.service";

@Component({
  selector: 'app-pipelines',
  templateUrl: './pipelines.component.html',
  styleUrls: ['./pipelines.component.css']
})
export class PipelinesComponent implements OnInit {
  pipelines: PipelineDefinitionInfo[] = [];
  loadError = null;

  raw: Map<string, string> = new Map();
  parseError: Map<string, ParseError[]> = new Map();
  error: Map<string, string> = new Map();
  success: Map<string, string> = new Map();

  longLoading = new LongLoadingDetector();

  selectedPipeline: PipelineDefinitionInfo | undefined;


  constructor(
    private api: PipelineApiService,
    private notification: NotificationService,
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

  loadRaw(pipeline: string): Promise<void> {
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

  onPipelineClicked(pipeline: PipelineDefinitionInfo): void {
    this.selectedPipeline = pipeline;
  }

  onAddPipeline(name: string): void {
    if (name) {
      return this.dialog.openLoadingIndicator(
        this.api.createPipelineDefinition(name)
          .then((newPipeline) => {
            this.pipelines.push(newPipeline);
            this.pipelines = this.pipelines.concat([]);
            this.selectedPipeline = newPipeline;
          }),
        'Creating Pipeline'
      );
    }
  }

  onDeletePipeline(pipeline: PipelineDefinitionInfo) {
    this.dialog.openAreYouSure(
      `Pipeline being deleted ${pipeline.name}`,
      () => this.api.deletePipeline(pipeline.id)
        .then(() => {
            let delIndex = this.pipelines.findIndex(tempPipeline => tempPipeline.id === pipeline.id);
            this.pipelines.splice(delIndex, 1);
            this.pipelines = this.pipelines.concat([]);
            this.selectedPipeline = undefined;
          }
        )
    );
  }
}
