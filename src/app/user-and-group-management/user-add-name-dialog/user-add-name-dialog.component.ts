import {Component, Inject, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {UserApiService} from '../../api/user-api.service';

@Component({
  selector: 'app-user-add-name-dialog',
  templateUrl: './user-add-name-dialog.component.html',
  styleUrls: ['./user-add-name-dialog.component.css']
})
export class UserAddNameDialogComponent implements OnInit {

  nameInput: string;
  errorMessage = ' ';
  isLoading = false;

  constructor(
    public dialogRef: MatDialogRef<UserAddNameDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: string,
    private userApi: UserApiService
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
      this.userApi.getUserNameAvailable(encodeURIComponent(this.nameInput))
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
