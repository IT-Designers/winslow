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
  errorMessage = '';

  constructor(
    public dialogRef: MatDialogRef<GroupAddNameDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: string,
    private groupApi: GroupApiService
  ) { }

  ngOnInit(): void {
  }

  createClicked() {
    if (!this.nameInput.includes(' ')) {
      this.groupApi.getGroupNameAvailable(this.nameInput)
        .then(
          () => {
            this.dialogRef.close(this.nameInput);
          }
        ).catch(() => {
        this.errorMessage = 'Error: Name is already taken, please choose another';
        console.log('Name rejected');
      });
    } else if (this.nameInput.includes(' ')) {
      this.errorMessage = 'Error: Name cannot include empty spaces';
    }
  }
}
