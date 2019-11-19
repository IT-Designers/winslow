import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {ParseError} from '../api/project-api.service';


@Component({
  selector: 'app-pipeline-editor',
  templateUrl: './pipeline-editor.component.html',
  styleUrls: ['./pipeline-editor.component.css']
})
export class PipelineEditorComponent implements OnInit {

  @Input() pipelineId: string;
  @Input() raw: string;
  @Input() errorV: string = null;
  @Input() successV: string = null;
  @Input() enableOnOthers = false;

  @Output() others = new EventEmitter<string>();
  @Output() update = new EventEmitter<string>();
  @Output() check = new EventEmitter<string>();

  editor = null;


  editorOptions = {
    theme: 'vs',
    language: 'yaml'
  };

  constructor() { }

  ngOnInit() {
  }

  @Input()
  set error(error: string) {
    this.errorV = error;
    if (this.editor != null) {
      setTimeout(() => {
        this.editor.layout();
      });
    }
  }

  @Input()
  set success(success: string) {
    this.successV = success;
    if (this.editor != null) {
      setTimeout(() => {
        this.editor.layout();
      });
    }
  }

  @Input()
  set parseError(parseErrors: ParseError[]) {
    monaco.editor.setModelMarkers(
      this.editor.getModel(),
      'parse-editor',
      parseErrors.map(e => {
        return {
          startLineNumber: e.line,
          startColumn: e.column,
          endLineNumber: e.line,
          endColumn: e.column + 100,
          message: e.message,
          severity: monaco.MarkerSeverity.Error
        } as monaco.editor.IMarkerData;
      })
    );
  }

  onInit(editor: monaco.editor.IStandaloneCodeEditor) {
    this.editor = editor;
    this.editor.layout();
  }
}
