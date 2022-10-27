import {Component, Inject, OnInit} from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import {GroupApiService} from '../api/group-api.service';

/*export interface GroupNameDialogData {
  name: string;
}*/


@Component({
  selector: 'app-group-add-name-dialog',
  templateUrl: './group-add-name-dialog.component.html',
  styleUrls: ['./group-add-name-dialog.component.css']
})
export class GroupAddNameDialogComponent implements OnInit {

  nameInput: string;
  errorMessage = ' ';

  constructor(
    public dialogRef: MatDialogRef<GroupAddNameDialogComponent>,
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
    if (this.nameInput.length < 20) {
      this.groupApi.getGroupNameAvailable(this.nameInput)
        .then()
        .catch((error) => {
          console.log(error.error);
          this.errorMessage = 'Error: ' + error.error;
        });
    } else if (this.nameInput.length >= 20) {
      this.errorMessage = 'Error: Name too long';
    }
  }
}
