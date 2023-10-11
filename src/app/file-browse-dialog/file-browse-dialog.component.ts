import {AfterViewInit, Component, Inject, OnInit, ViewChild} from '@angular/core';
import {ApiService} from '../api/api.service';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import {FilesComponent} from '../files/files.component';

interface FileBrowseData {
  additionalRoot?: string;
  preselectedPath?: string;
}

@Component({
  selector: 'app-file-dialog',
  templateUrl: './file-browse-dialog.component.html',
  styleUrls: ['./file-browse-dialog.component.css']
})
export class FileBrowseDialog implements OnInit, AfterViewInit {

  @ViewChild('files') files: FilesComponent;

  constructor(
    public dialogRef: MatDialogRef<FileBrowseDialog>,
    @Inject(MAT_DIALOG_DATA) public data: FileBrowseData,
    private api: ApiService) {
  }

  ngOnInit() {

  }

  ngAfterViewInit() {
    if (this.data.additionalRoot != null) {
      this.files.additionalRoot = this.data.additionalRoot;
    }
    if (this.data.preselectedPath != null) {
      this.files.navigateDirectlyTo(this.data.preselectedPath);
    }
  }

  onCancel() {
    this.dialogRef.close();
  }

  onOk(value: string) {
    this.dialogRef.close(value);
  }
}
