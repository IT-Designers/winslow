import {Component, OnInit} from '@angular/core';
import {ApiService, PipelineInfo} from '../api.service';

@Component({
  selector: 'app-pipelines',
  templateUrl: './pipelines.component.html',
  styleUrls: ['./pipelines.component.css']
})
export class PipelinesComponent implements OnInit {
  private pipelines: PipelineInfo[] = [];
  private stages: Map<string, string[]> = new Map();

  constructor(private api: ApiService) {

  }

  ngOnInit() {
    this.api.listPipelines().toPromise().then(r => this.pipelines = r);
  }

  loadStages($event: MouseEvent, pipeline: PipelineInfo) {
    if (($event.target as HTMLElement).dataset.showStages === 'true') {
      this.api.listStages(pipeline).toPromise().then(s => {
        this.stages.set(pipeline.id, s);
        console.log(JSON.stringify(s));
      });
    } else {
      this.stages.delete(pipeline.id);
    }
  }
}
