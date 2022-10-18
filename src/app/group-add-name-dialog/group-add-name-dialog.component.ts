import {Component, Inject, OnInit} from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

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

  constructor(
    public dialogRef: MatDialogRef<GroupAddNameDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: string
  ) { }

  ngOnInit(): void {
  }

}
