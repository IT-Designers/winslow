import {Component, Inject, OnInit} from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import {ProjectInfo} from '../api/winslow-api';

@Component({
  selector: 'app-group-action-dialog',
  templateUrl: './group-settings-dialog.component.html',
  styleUrls: ['./group-settings-dialog.component.css']
})
export class GroupSettingsDialogComponent implements OnInit {

  availableTags: string[];
  projects: ProjectInfo[];
  filtered: ProjectInfo[];

  constructor(
    public dialogRef: MatDialogRef<GroupSettingsDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: GroupSettingsDialogData) {
    this.projects = data.projects.sort((a, b) => a.name.localeCompare(b.name));
    this.availableTags = data.availableTags;
  }

  ngOnInit() {
  }

  apply() {
    this.dialogRef.close(this.filtered);
  }

  cancel() {
    this.dialogRef.close(null);
  }
}

export class GroupSettingsDialogData {
  projects: ProjectInfo[];
  availableTags: string[];
}
