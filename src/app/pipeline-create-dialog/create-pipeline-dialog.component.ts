import {Component, OnInit} from '@angular/core';
import {UntypedFormBuilder, UntypedFormGroup} from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';

@Component({
  selector: 'app-dialog-create-pipeline',
  templateUrl: './create-pipeline-dialog.component.html',
  styleUrls: ['./create-pipeline-dialog.component.css']
})
export class CreatePipelineDialogComponent implements OnInit {

  form: UntypedFormGroup;

  constructor(
    private formBuilder: UntypedFormBuilder,
    private dialogRef: MatDialogRef<CreatePipelineDialogComponent, CreatePipelineResult>
  ) {
    this.form = this.formBuilder.group(new CreatePipelineResult());
  }

  ngOnInit() {
  }

  submit() {
    if (this.form.valid) {
      this.dialogRef.close(this.form.value as CreatePipelineResult);
    }
  }
}
export class CreatePipelineResult {
  name: string | undefined = undefined;
}
