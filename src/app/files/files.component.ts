import {Component, EventEmitter, Inject, Input, OnInit, Output} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialog, MatDialogRef} from '@angular/material';
import {HttpEventType} from '@angular/common/http';
import {FileInfo, FilesApiService} from '../api/files-api.service';
import {LongLoadingDetector} from '../long-loading-detector';

@Component({
  selector: 'app-files',
  templateUrl: './files.component.html',
  styleUrls: ['./files.component.css']
})
export class FilesComponent implements OnInit {
  @Input() additionalRoot?: string;
  @Input() navigateToAdditionalRoot = true;
  @Input() navigationTarget?: string;

  files: Map<string, FileInfo[]> = null;
  longLoading = new LongLoadingDetector();
  loadError = null;

  latestPath = '/resources'; // IMPORTANT: starts with a slash, but never ends with one: '/resources/ab/cd/ef'
  @Output('selection') selectedPath = new EventEmitter<string>();

  contextMenuX = 0;
  contextMenuY = 0;
  contextMenuVisible = false;

  constructor(
    private api: FilesApiService,
    private createDialog: MatDialog
  ) {
  }

  ngOnInit() {
    let additionalPath: string = null;
    this.longLoading.increase();
    this.api
      .listFiles('/resources')
      .toPromise()
      .then(res => {
        this.files = new Map<string, FileInfo[]>();
        this.files.set('/', (() => {
          const root = [];
          const info = new FileInfo();
          info.directory = true;
          info.name = 'resources';
          info.path = '/resources';
          this.files.set('/resources', res);
          root.push(info);

          if (this.additionalRoot != null) {
            const additional = new FileInfo();
            additional.directory = true;
            additional.name = this.additionalRoot.split(';')[0];
            additional.path = `/${this.additionalRoot.split(';')[1]}`;
            additionalPath = additional.path;
            this.files.set(additional.path, []);
            root.push(additional);
          }
          return root;
        })());
        return this.loadDirectory(this.latestPath);
      }).then(result => {
      if (additionalPath) {
        return this.navigateDirectlyTo(additionalPath);
      }
    }).then(result => {
      if (this.navigationTarget != null) {
        return this.navigateDirectlyTo(this.navigationTarget);
      }
    }).catch(error => {
      this.loadError = error;
    }).finally(() => this.longLoading.decrease());
  }

  updateAdditionalRoot(root: string, view: boolean) {
    this.additionalRoot = root;
    const files = this.files.get('/');
    if (files.length < 2) {
      files.push(null);
    } else {
      this.files.delete(files[0].path);
    }
    files[1] = new FileInfo();
    files[1].directory = true;
    files[1].name = this.additionalRoot.split(';')[0];
    files[1].path = `/${this.additionalRoot.split(';')[1]}`;
    this.files.set(files[1].path, []);
    if (view) {
      this.loadDirectory(files[1].path);
    }
  }

  directories(path: string) {
    const files = this.files.get(path);
    return files != null ? files.filter(f => f.directory) : null;
  }

  private removeCachedRecursively(path: string) {
    const info = this.files.get(path);
    if (info != null) {
      for (const i of info) {
        this.removeCachedRecursively(i.path);
      }
    }
    this.files.delete(path);
  }

  toggleLoadDirectory(path: string) {
    if (this.files.has(path)) {
      this.removeCachedRecursively(path);
    } else {
      this.loadDirectory(path).finally(() => {
      });
    }
  }

  private insertListResourceResult(path: string, res: FileInfo[]) {
    this.files.set(
      path,
      res
        .sort((a, b) => a.name > b.name ? 1 : -1) // sort by name
        .sort((a, b) => a.directory < b.directory ? 1 : -1) // directories first
    );
  }

  private loadDirectory(path: string) {
    this.longLoading.increase();
    return this.api.listFiles(path).toPromise().then(res => {
      this.insertListResourceResult(path, res);
    }).finally(() => this.longLoading.decrease());
  }

  currentDirectoryFilesOnly(): FileInfo[] {
    return this.currentDirectory().filter(info => !info.directory);
  }

  currentDirectory(): FileInfo[] {
    return this.files.has(this.latestPath) ? this.files.get(this.latestPath) : [];
  }

  viewDirectory(path: string) {
    this.loadDirectory(path).then(_ => {
      this.selectedPath.emit(this.latestPath = path);
      this.updateSelection();
    });
  }

  navigateDirectlyTo(path: string): Promise<void> {
    const current = this.latestPath.split('/').filter(d => d.length > 0);
    const split = path.split('/').filter(d => d.length > 0);
    let index = 0;
    let combined = '';

    for (let i = 0; i < current.length && i < split.length; ++i) {
      if (current[i] === split[i]) {
        if (i > 0) {
          index = i;
          combined += '/' + current[i - 1];
        }
      } else {
        break;
      }
    }

    this.longLoading.increase();
    return this.recursivelyLoadDirectoriesOfPath(split, index, combined).then(p => {
      this.selectedPath.emit(this.latestPath = p);
      this.updateSelection();
    }).finally(() => this.longLoading.decrease());
  }

  private recursivelyLoadDirectoriesOfPath(pathSplit: string[], currentIndex = 0, combined = ''): Promise<string> {
    if (currentIndex < pathSplit.length) {
      const before = combined;
      combined += '/' + pathSplit[currentIndex];
      return this.api.listFiles(combined).toPromise().then(res => {
        if (res != null) {

          this.insertListResourceResult(combined, res);
          this.selectedPath.emit(this.latestPath = combined);

          for (const r of res) {
            if (r.name === pathSplit[currentIndex + 1] && r.directory) {
              return this.recursivelyLoadDirectoriesOfPath(pathSplit, currentIndex + 1, combined);
            }
          }
          return Promise.resolve(combined);
        }
        return Promise.resolve(before);
      });
    } else {
      return Promise.resolve(combined);
    }
  }

  absoluteDirectoryPath(path: string, directory = true): string {
    if (!path.endsWith('/') && directory) {
      path += '/';
    }
    if (!path.startsWith('/')) {
      path = '/' + path;
    }
    return path;
  }

  viewContextMenu($event: MouseEvent) {
    $event.preventDefault();
    this.contextMenuX = $event.x;
    this.contextMenuY = $event.y;
    this.contextMenuVisible = true;
  }

  createDirectory() {
    this.createDialog.open(CreateDirectoryDialog, {
      width: '20em',
      data: {}
    }).afterClosed().subscribe(result => {
      if (result) {
        const path = this.absoluteDirectoryPath(this.latestPath + '/' + result);
        this.longLoading.increase();
        this
          .api
          .createDirectory(path)
          .then(p => this.navigateDirectlyTo(this.latestPath))
          .finally(() => this.longLoading.decrease());
      }
    });
  }

  updateSelection() {
    const CLASS_NAME = 'directory';
    const CLASS_NAME_SELECTED = 'directory-selected';
    const DATA_ATTRIBUTE = 'data-path';

    const elements = document.getElementsByClassName(CLASS_NAME_SELECTED);
    for (let i = 0; i < elements.length; ++i) {
      elements.item(i).classList.remove(CLASS_NAME_SELECTED);
    }
    const directories = document.getElementsByClassName(CLASS_NAME);
    for (let i = 0; i < directories.length; ++i) {
      const value = directories.item(i).attributes.getNamedItem(DATA_ATTRIBUTE).value;
      if (value === this.latestPath) {
        directories.item(i).classList.add(CLASS_NAME_SELECTED);
        break;
      }
    }
  }

  uploadFile(files: FileList, currentItem = 0, data: UploadFilesProgress = null, dialog: MatDialogRef<UploadFilesProgressDialog> = null) {
    if (data == null || dialog == null) {
      data = {
        uploads: [],
        closable: false,
      };
      for (let i = 0; i < files.length; ++i) {
        data.uploads.push([files.item(i).name, 0, 100]);
      }
      dialog = this.createDialog.open(UploadFilesProgressDialog, {
        width: '60%',
        data,
      });
    }
    if (currentItem < files.length) {
      this.api.uploadFile(this.latestPath, files.item(currentItem)).subscribe(event => {
        if (event.type === HttpEventType.UploadProgress) {
          data.uploads[currentItem][1] = event.loaded;
          data.uploads[currentItem][2] = event.total;
        }
      }).add(() => {
        this.loadDirectory(this.latestPath);
        this.uploadFile(files, currentItem + 1, data, dialog);
      });
    } else {
      data.closable = true;
      setTimeout(() => {
        dialog.close();
      }, 5000);
    }
  }

  downloadFile(file: FileInfo) {
    this.api.downloadFile(file.path);
  }

  delete(file: FileInfo) {
    this
      .createDialog
      .open(DeleteAreYouSureDialog, {width: '40em', data: file})
      .afterClosed()
      .subscribe(result => {
        if (result) {
          this.longLoading.increase();
          this.api
            .delete(file.path)
            .toPromise()
            .finally(() => {
              this.longLoading.decrease();
              this.loadDirectory(this.latestPath);
            });
        }
      });
  }

  onItemSelected(file: FileInfo) {
    this.selectedPath.emit(file.path);
  }
}

export interface CreateDirectoryData {
  name: string;
}

@Component({
  selector: 'dialog-directory-create',
  template: `
      <h1 mat-dialog-title>Creating a new directory</h1>
      <div mat-dialog-content>
          <mat-form-field>
              <input cdkFocusInitial
                     matInput [(ngModel)]="data.name" placeholder="Name of the new directory"
                     (keyup.enter)="dialogRef.close(data.name)"
                     (keydown.escape)="dialogRef.close()"
              >
          </mat-form-field>
      </div>
      <div mat-dialog-actions align="end">
          <button mat-button (click)="dialogRef.close(data.name)">Submit</button>
          <button mat-button (click)="dialogRef.close()">Cancel</button>
      </div>`
})
export class CreateDirectoryDialog {
  constructor(
    public dialogRef: MatDialogRef<CreateDirectoryDialog>,
    @Inject(MAT_DIALOG_DATA) public data: CreateDirectoryData) {
  }
}

export interface UploadFilesProgress {
  uploads: [string, number, number][];
  closable: boolean;
}

@Component({
  selector: 'dialog-upload-files-progress',
  template: `
      <h1 mat-dialog-title>Uploading your files</h1>
      <div mat-dialog-content>
          <mat-list *ngFor="let upload of data.uploads">
              <p>{{upload[0]}}</p>
              <mat-progress-bar mode="determinate" value="{{ upload[1] / upload[2] * 100 }}"></mat-progress-bar>
          </mat-list>
      </div>
      <div mat-dialog-actions align="end">
          <button [disabled]="!data.closable" mat-button (click)="dialogRef.close()">Close</button>
      </div>
  `
})
export class UploadFilesProgressDialog {
  constructor(
    public dialogRef: MatDialogRef<UploadFilesProgressDialog>,
    @Inject(MAT_DIALOG_DATA) public data: UploadFilesProgress) {
  }
}

@Component({
  selector: 'dialog-delete-are-you-sure',
  template: `
      <h1 mat-dialog-title>Are you sure you want to delete this file?</h1>
      <div mat-dialog-content>
          <p>{{file.name}}</p>
      </div>
      <div mat-dialog-actions align="end">
          <button mat-button (click)="dialogRef.close(true)">Delete</button>
          <button mat-button (click)="dialogRef.close(false)">Cancel</button>
      </div>`
})
export class DeleteAreYouSureDialog {
  constructor(
    public dialogRef: MatDialogRef<DeleteAreYouSureDialog>,
    @Inject(MAT_DIALOG_DATA) public file: FileInfo) {
  }
}
