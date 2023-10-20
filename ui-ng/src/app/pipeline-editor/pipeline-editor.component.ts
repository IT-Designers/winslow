import {Component, ElementRef, EventEmitter, Input, OnInit, Output, ViewChild} from '@angular/core';
import {ParseError} from '../api/winslow-api';
import {editor} from "monaco-editor";
import ICodeEditor = editor.ICodeEditor;

@Component({
  selector: 'app-pipeline-editor',
  templateUrl: './pipeline-editor.component.html',
  styleUrls: ['./pipeline-editor.component.css']
})
export class PipelineEditorComponent implements OnInit {

  // what an ugly hack
  State = EditorState;


  @ViewChild('editorContainer') container!: ElementRef<HTMLDivElement>;

  @Input() pipelineId!: string;
  @Input() rawV: string | null = null;
  @Input() errorV: string | null = null;
  @Input() successV: string | null = null;
  @Input() enableOnOthers = false;

  @Output() others = new EventEmitter<string>();
  @Output() update = new EventEmitter<string>();
  @Output() check = new EventEmitter<string>();

  state?: EditorState;

  editor?: ICodeEditor;
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
  set raw(raw: string | null) {
    if (raw != null) {
      this.original = raw;
      this.rawV = raw;
      this.updateState();
    }
  }

  @Input()
  set error(error: string | null) {
    this.errorV = error;
    this.updateState();
    if (this.editor != undefined) {
      setTimeout(() => {
        this.editor?.layout();
      });
    }
  }

  @Input()
  set success(success: string | null) {
    this.successV = success;
    this.updateState();
    if (this.editor != undefined) {
      setTimeout(() => {
        this.editor?.layout();
      });
    }
  }

  @Input()
  set parseError(parseErrors: ParseError[]) {
    if (this.editor != null && parseErrors) {
      // @ts-ignore
      monaco.editor.setModelMarkers(
        this.editor.getModel(),
      'parse-editor',
        parseErrors.map(e => {
          this.errorV = e.message;
          console.log(e.message)
          return {
            startLineNumber: e.line,
            startColumn: e.column,
            endLineNumber: e.line,
            endColumn: e.column + 100,
            message: e.message,
            // @ts-ignore
            severity: monaco.MarkerSeverity.Error
            // @ts-ignore
          } as monaco.editor.IMarkerData;
        })
      );
    }
  }

  onInit(editor: ICodeEditor) {
    this.editor = editor;
    this.editor.layout();
    this.updateState();
  }

  layout(width?: number, height?: number) {
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
        this.editor.layout(dimension);
      }
    }
  }

  save(rawV: string | null) {
    if (rawV != null) {
      this.raw = rawV;
      this.updateState();
      this.update.emit(rawV);
    }
  }

  updateState() {
    if (this.errorV != null) {
      this.state = EditorState.Failure;
    } else if (this.rawV !== this.original) {
      this.state = EditorState.UnsavedChanges;
    } else if (this.successV != null) {
      this.state = EditorState.Success;
    } else {
      this.state = undefined;
    }
  }
}

export enum EditorState {
  UnsavedChanges,
  Success,
  Failure
}
