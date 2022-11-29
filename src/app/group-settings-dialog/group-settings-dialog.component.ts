import {Component, Inject, OnInit} from '@angular/core';
import {IProjectInfoExt} from '../api/project-api.service';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

@Component({
  selector: 'app-group-action-dialog',
  templateUrl: './group-settings-dialog.component.html',
  styleUrls: ['./group-settings-dialog.component.css']
})
export class GroupSettingsDialogComponent implements OnInit {

  availableTags: string[];
  projects: IProjectInfoExt[];
  filtered: IProjectInfoExt[];

  constructor(
    public dialogRef: MatDialogRef<GroupSettingsDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: GroupSettingsDialogData) {
    this.projects = data.projects.sort((a, b) => a.name.localeCompare(b.name));
    this.availableTags = data.availableTags;
  }

  ngOnInit() {
  }

  apply() {
    this.dialogRef.close(this.filtered.map(f => f.id));
  }

  cancel() {
    this.dialogRef.close(null);
  }
}

export class GroupSettingsDialogData {
  projects: IProjectInfoExt[];
  availableTags: string[];
}
