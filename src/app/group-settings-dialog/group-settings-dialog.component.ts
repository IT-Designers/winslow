import {Component, Inject, OnInit} from '@angular/core';
import {Project} from '../api/project-api.service';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material';

@Component({
  selector: 'app-group-action-dialog',
  templateUrl: './group-settings-dialog.component.html',
  styleUrls: ['./group-settings-dialog.component.css']
})
export class GroupSettingsDialogComponent implements OnInit {

  availableTags: string[];
  projects: Project[];
  filtered: Project[];

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
  projects: Project[];
  availableTags: string[];
}
