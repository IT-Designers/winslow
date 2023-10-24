import {Component, Inject, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {FilesApiService} from '../api/files-api.service';
import {DialogService} from '../dialog.service';
import {FileInfo, ProjectInfo} from '../api/winslow-api';

export interface ProjectDiskUsageDialogData {
  projects: ProjectInfo[];
}

@Component({
  selector: 'app-project-disk-usage-dialog',
  templateUrl: './project-disk-usage-dialog.component.html',
  styleUrls: ['./project-disk-usage-dialog.component.css']
})
export class ProjectDiskUsageDialogComponent implements OnInit {

  projects: [FileInfo, FileInfo[]][] = [];

  constructor(
    public dialogRef: MatDialogRef<ProjectDiskUsageDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ProjectDiskUsageDialogData,
    private files: FilesApiService,
    private dialog: DialogService) {
  }

  ngOnInit() {
    const projects: [FileInfo, FileInfo[]][] = [];
    const promises: Promise<any>[] = [];

    for (const project of this.data.projects) {
      const path = `/workspaces/${project.id}`;
      promises.push(this.files
        .listFiles(path, true)
        .then(r => {
          const info = new FileInfo({
            name:  project.name,
            directory: true,
            path,
            fileSize:  r.map(f => f.fileSize).reduce((s1, s2) => (s1 ?? 0) + (s2 ?? 0), 0),
            attributes: {}
          } as FileInfo);
          projects.push([info, r.sort((a, b) => (a.fileSize ?? 0) < (b.fileSize ?? 0) ? 1 : -1)]);
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
    this.projects.sort((a, b) => (a[0].fileSize ?? 0) < (b[0].fileSize ?? 0) ? 1 : -1);
  }
}
