import {Component, Inject, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";
import {PipelineApiService} from "../../api/pipeline-api.service";

@Component({
  selector: 'app-add-pipeline-dialog',
  templateUrl: './add-pipeline-dialog.component.html',
  styleUrls: ['./add-pipeline-dialog.component.css']
})
export class AddPipelineDialogComponent implements OnInit {

  nameInput: string;
  errorMessage = ' ';
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
    this.errorMessage = '';
    this.isLoading = true;
    if (!(this.nameInput.includes('%') || this.nameInput.includes('/') || this.nameInput.includes('\\'))) {
      this.pipelineApi.getPipelineDefinitionAvailable(this.nameInput)
        .then(() => this.isLoading = false)
        .catch((error) => {
          console.log(error);
          this.errorMessage = 'Error: ' + error.error;
          this.isLoading = false;
        })
    } else {
      this.isLoading = false;
      this.errorMessage = 'Error: Invalid name. Name must contain only: English letters, numbers, underscore, minus and dot and not exceed 20 characters';
    }
  }

}
