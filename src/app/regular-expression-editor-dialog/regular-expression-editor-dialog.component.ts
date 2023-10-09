import {Component, Inject, OnInit} from '@angular/core';
import {MAT_LEGACY_DIALOG_DATA as MAT_DIALOG_DATA} from '@angular/material/legacy-dialog';

@Component({
  selector: 'app-regular-expression-editor-dialog',
  templateUrl: './regular-expression-editor-dialog.component.html',
  styleUrls: ['./regular-expression-editor-dialog.component.css']
})
export class RegularExpressionEditorDialogComponent implements OnInit {
  expression: string;
  textToTest: string;

  constructor(@Inject(MAT_DIALOG_DATA) dialogData: string) {
    this.expression = dialogData ?? ""
    this.textToTest = ""
  }

  ngOnInit(): void {
  }
}
