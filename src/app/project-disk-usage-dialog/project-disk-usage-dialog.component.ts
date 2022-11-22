import {Component, Inject, OnInit} from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import {IFileInfoExt, FilesApiService} from '../api/files-api.service';
import {ProjectInfo} from '../api/project-api.service';
import {DialogService} from '../dialog.service';

export interface ProjectDiskUsageDialogData {
  projects: ProjectInfo[];
}

@Component({
  selector: 'app-project-disk-usage-dialog',
  templateUrl: './project-disk-usage-dialog.component.html',
  styleUrls: ['./project-disk-usage-dialog.component.css']
})
export class ProjectDiskUsageDialogComponent implements OnInit {

  projects: [IFileInfoExt, IFileInfoExt[]][] = [];

  constructor(
    public dialogRef: MatDialogRef<ProjectDiskUsageDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ProjectDiskUsageDialogData,
    private files: FilesApiService,
    private dialog: DialogService) {
  }

  ngOnInit() {
    const projects = [];
    const promises = [];

    for (const project of this.data.projects) {
      const path = `/workspaces/${project.id}`;
      promises.push(this.files
        .listFiles(path, true)
        .then(r => {
          const info = new IFileInfoExt();
          info.name = project.name;
          info.fileSize = r.map(f => f.fileSize).reduce((s1, s2) => s1 + s2, 0);
          info.directory = true;
          info.path = path;
          projects.push([info, r.sort((a, b) => a.fileSize < b.fileSize ? 1 : -1)]);
        }));
    }

    this.dialog.openLoadingIndicator(
      Promise.all(promises).then(r => {
        this.projects = projects;
        this.sortProjects();
      }),
      `Measuring Projects`,
      false
    );
  }

  sortProjects() {
    this.projects.sort((a, b) => a[0].fileSize < b[0].fileSize ? 1 : -1);
  }
}
