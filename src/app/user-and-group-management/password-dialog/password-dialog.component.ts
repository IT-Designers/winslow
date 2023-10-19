import {Component, Inject, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';

@Component({
  selector: 'app-password-dialog',
  templateUrl: './password-dialog.component.html',
  styleUrls: ['./password-dialog.component.css']
})
export class PasswordDialogComponent implements OnInit {

  password1 = '';
  password2 = '';

  passwordHintColor = 'red';

  constructor(
    public dialogRef: MatDialogRef<PasswordDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: unknown,
  ) {
  }

  ngOnInit(): void {
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  setPasswordHintColor() {
    if (this.password1.length < 8) {
      this.passwordHintColor = 'red';
    } else if (this.password1.length >= 8 && this.password1.length < 10) {
      this.passwordHintColor = 'orange';
    } else if (this.password1.length >= 10 && this.password1.length < 12) {
      this.passwordHintColor = 'yellow';
    } else if (this.password1.length >= 12) {
      this.passwordHintColor = 'green';
    }
  }

  onSubmit() {
    if (this.password1 === this.password2) {
      this.dialogRef.close(this.password1);
    }
  }
}
