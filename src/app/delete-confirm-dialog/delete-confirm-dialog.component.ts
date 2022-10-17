import { Component, OnInit } from '@angular/core';
import {  MatDialogRef } from '@angular/material/dialog';

export interface ResponseData {
  delete: boolean;
}

@Component({
  selector: 'app-delete-confirm-dialog',
  templateUrl: './delete-confirm-dialog.component.html',
  styleUrls: ['./delete-confirm-dialog.component.css']
})
export class DeleteConfirmDialogComponent implements OnInit {

  constructor(
    public dialogRef: MatDialogRef<DeleteConfirmDialogComponent>,
    ) { }

  ngOnInit(): void {
  }

}
