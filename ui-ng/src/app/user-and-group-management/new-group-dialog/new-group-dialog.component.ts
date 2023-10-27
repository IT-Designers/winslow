import {Component, Inject, OnInit} from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import {GroupApiService} from '../../api/group-api.service';

/*export interface GroupNameDialogData {
  name: string;
}*/


@Component({
  selector: 'app-new-group-dialog',
  templateUrl: './new-group-dialog.component.html',
  styleUrls: ['./new-group-dialog.component.css']
})
export class NewGroupDialogComponent implements OnInit {

  nameInput: string = '';
  errorMessage = ' ';
  isLoading = false;

  constructor(
    public dialogRef: MatDialogRef<NewGroupDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: string,
    private groupApi: GroupApiService
  ) { }

  ngOnInit(): void {
  }

  createClicked() {
    this.dialogRef.close(this.nameInput);
  }
  checkName() {
    this.errorMessage = '';
    /*if (this.nameInput.length < 20) {
      this.groupApi.getGroupNameAvailable(this.nameInput)
        .then()
        .catch((error) => {
          console.log(error.error);
          this.errorMessage = 'Error: ' + error.error;
        });
    } else if (this.nameInput.length >= 20) {
      this.errorMessage = 'Error: Name too long';
    }*/
    this.isLoading = true;
    if (!(this.nameInput.includes('%') || this.nameInput.includes('/') || this.nameInput.includes('\\'))) {
      this.groupApi.getGroupNameAvailable(encodeURIComponent(this.nameInput))
        .then(() => this.isLoading = false)
        .catch((error) => {
          console.log(error.error);
          this.errorMessage = 'Error: ' + error.error;
          this.isLoading = false;
        });
    } else {
      this.isLoading = false;
      this.errorMessage = 'Error: Invalid name. Name must contain only: English letters, numbers, underscore, minus and dot and not exceed 20 characters';
    }
  }
}
