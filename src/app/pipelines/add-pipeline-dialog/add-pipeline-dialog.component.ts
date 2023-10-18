import {Component, Inject, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";
import {PipelineApiService} from "../../api/pipeline-api.service";

@Component({
  selector: 'app-add-pipeline-dialog',
  templateUrl: './add-pipeline-dialog.component.html',
  styleUrls: ['./add-pipeline-dialog.component.css']
})
export class AddPipelineDialogComponent implements OnInit {

  nameInput: string = '';
  errorMessage = '';
  isLoading = false;
  constructor(
    public dialogRef: MatDialogRef<AddPipelineDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: string,
    private pipelineApi: PipelineApiService
  ) { }

  ngOnInit(): void {
  }

  createClicked() {
    this.dialogRef.close(this.nameInput);
  }

  checkName() {

  }

}
