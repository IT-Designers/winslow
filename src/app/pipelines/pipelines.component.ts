import {Component, OnInit} from '@angular/core';
import {ApiService, PipelineDefinition} from '../api.service';

@Component({
  selector: 'app-pipelines',
  templateUrl: './pipelines.component.html',
  styleUrls: ['./pipelines.component.css']
})
export class PipelinesComponent implements OnInit {
  pipelines: PipelineDefinition[] = [];
  stages: Map<string, string[]> = new Map();

  constructor(private api: ApiService) {

  }

  ngOnInit() {
    this.api.getPipelineDefinitions().toPromise().then(r => this.pipelines = r);
  }

  loadStages($event: MouseEvent, pipeline: PipelineDefinition) {
    if (($event.target as HTMLElement).dataset.showStages === 'true') {
      this.api.getStageDefinitions(pipeline).toPromise().then(s => {
        this.stages.set(pipeline.id, s);
        console.log(JSON.stringify(s));
      });
    } else {
      this.stages.delete(pipeline.id);
    }
  }
}
