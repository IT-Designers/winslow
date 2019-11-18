import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';

@Component({
  selector: 'app-pipeline-editor',
  templateUrl: './pipeline-editor.component.html',
  styleUrls: ['./pipeline-editor.component.css']
})
export class PipelineEditorComponent implements OnInit {

  @Input() pipelineId: string;
  @Input() raw: string;
  @Input() error: string = null;
  @Input() success: string = null;

  @Output() update = new EventEmitter<string>();
  @Output() check = new EventEmitter<string>();

  constructor() { }

  ngOnInit() {
  }

}
