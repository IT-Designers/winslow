import {Component, ElementRef, EventEmitter, Input, OnInit, Output, ViewChild} from '@angular/core';
import {ParseError} from '../api/winslow-api';

@Component({
  selector: 'app-pipeline-editor',
  templateUrl: './pipeline-editor.component.html',
  styleUrls: ['./pipeline-editor.component.css']
})
export class PipelineEditorComponent implements OnInit {

  // what an ugly hack
  State = EditorState;


  @ViewChild('editorContainer') container: ElementRef<HTMLDivElement>;

  @Input() pipelineId: string;
  @Input() rawV: string;
  @Input() errorV: string = null;
  @Input() successV: string = null;
  @Input() enableOnOthers = false;

  @Output() others = new EventEmitter<string>();
  @Output() update = new EventEmitter<string>();
  @Output() check = new EventEmitter<string>();

  @Output() state?: EditorState = null;

  editor = null;
  original = '';


  editorOptions = {
    theme: 'vs',
    language: 'yaml'
  };

  constructor() {
  }

  ngOnInit() {
  }

  @Input()
  set raw(raw: string) {
    this.original = raw;
    this.rawV = raw;
    this.updateState();
  }

  @Input()
  set error(error: string) {
    this.errorV = error;
    this.updateState();
    if (this.editor != null) {
      setTimeout(() => {
        this.editor.layout();
      });
    }
  }

  @Input()
  set success(success: string) {
    this.successV = success;
    this.updateState();
    if (this.editor != null) {
      setTimeout(() => {
        this.editor.layout();
      });
    }
  }

  @Input()
  set parseError(parseErrors: ParseError[]) {
    if (this.editor != null && parseErrors) {
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
  }

  onInit(editor: monaco.editor.IStandaloneCodeEditor) {
    this.editor = editor;
    this.editor.layout();
    this.updateState();
  }

  layout(width: number = null, height: number = null) {
    if (this.container && (width == null || height == null)) {
      width = this.container.nativeElement.getBoundingClientRect().width;
      height = this.container.nativeElement.getBoundingClientRect().height;
    }
    if (this.editor) {
      let dimension = null;
      if (width != null && height != null) {
        dimension = {
          width,
          height
        };
      }
      this.editor.layout(dimension);
    }
  }

  save(rawV: string) {
    this.raw = rawV;
    this.updateState();
    this.update.emit(rawV);
  }

  updateState() {
    if (this.errorV != null) {
      this.state = EditorState.Failure;
    } else if (this.rawV !== this.original) {
      this.state = EditorState.UnsavedChanges;
    } else if (this.successV != null) {
      this.state = EditorState.Success;
    } else {
      this.state = null;
    }
  }
}

export enum EditorState {
  UnsavedChanges,
  Success,
  Failure
}
