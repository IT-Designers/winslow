import {Component, OnInit} from '@angular/core';
import {FormBuilder, FormGroup} from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';

@Component({
  selector: 'app-dialog-create-pipeline',
  templateUrl: './create-pipeline-dialog.component.html',
  styleUrls: ['./create-pipeline-dialog.component.css']
})
export class CreatePipelineDialogComponent implements OnInit {

  form: FormGroup;

  constructor(
    private formBuilder: FormBuilder,
    private dialogRef: MatDialogRef<CreatePipelineDialogComponent, CreatePipelineResult>
  ) {
  }

  ngOnInit() {
    this.form = this.formBuilder.group(new CreatePipelineResult());
  }

  submit() {
    if (this.form.valid) {
      this.dialogRef.close(this.form.value as CreatePipelineResult);
    }
  }
}
export class CreatePipelineResult {
  name: string = undefined;
}
