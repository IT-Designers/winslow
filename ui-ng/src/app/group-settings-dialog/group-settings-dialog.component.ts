import {Component, Inject, OnInit} from '@angular/core';
import { MAT_LEGACY_DIALOG_DATA as MAT_DIALOG_DATA, MatLegacyDialogRef as MatDialogRef } from '@angular/material/legacy-dialog';
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
    this.dialogRef.close(this.filtered.map(f => f.id));
  }

  cancel() {
    this.dialogRef.close(null);
  }
}

export class GroupSettingsDialogData {
  projects: ProjectInfo[];
  availableTags: string[];
}
