import {Component, EventEmitter, Input, OnInit, Output, ViewChild} from '@angular/core';
import {HttpEventType} from '@angular/common/http';
import {FileInfo, FilesApiService} from '../api/files-api.service';
import {LongLoadingDetector} from '../long-loading-detector';
import {DialogService, InputDefinition} from '../dialog.service';
import {SwalComponent, SwalPortalTargets} from '@sweetalert2/ngx-sweetalert2';
import Swal from 'sweetalert2';
import {StorageApiService} from '../api/storage-api.service';
import {by} from 'protractor';

@Component({
  selector: 'app-files',
  templateUrl: './files.component.html',
  styleUrls: ['./files.component.css']
})
export class FilesComponent implements OnInit {

  files: Map<string, FileInfo[]> = null;
  longLoading = new LongLoadingDetector();
  loadError = null;

  showDirectorySize = false;
  latestPath = '/resources'; // IMPORTANT: starts with a slash, but never ends with one: '/resources/ab/cd/ef'
  @Output('selection') selectedPath = new EventEmitter<string>();

  contextMenuX = 0;
  contextMenuY = 0;
  contextMenuVisible = false;

  dataUpload: UploadFilesProgress = null;
  @ViewChild('swalUpload') swalUpload: SwalComponent;
  viewHint: string = null;


  constructor(
    private api: FilesApiService,
    private dialog: DialogService,
    private storage: StorageApiService,
    public readonly swalTargets: SwalPortalTargets
  ) {
    const root = [];
    const info = new FileInfo();
    info.directory = true;
    info.name = 'resources';
    info.path = '/resources';
    root.push(info);
    this.files = new Map();
    this.files.set('/', root);
  }

  ngOnInit() {
    this.longLoading.increase();
    this.api
      .listFiles('/resources')
      .then(res => {
        this.insertListResourceResult('/resources', res);
        return this.loadDirectory(this.latestPath);
      })
      .then(result => {
        if (this.navigationTarget != null) {
          return this.navigateDirectlyTo(this.navigationTarget);
        }
      }).catch(error => {
      this.loadError = error;
    }).finally(() => this.longLoading.decrease());
  }

  @Input()
  public set additionalRoot(value: string) {
    const additional = new FileInfo();
    additional.directory = true;
    additional.name = value.split(';')[0];
    additional.path = `/${value.split(';')[1]}`;
    this.files.get('/').splice(1);
    this.files.get('/').push(additional);
    this.files.set(additional.path, []);
    this.navigationTarget = additional.path;
  }

  @Input()
  public set navigationTarget(target: string) {
    if (target != null) {
      this.navigateDirectlyTo(target);
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
    return this.api.listFiles(path, this.showDirectorySize).then(res => {
      this.insertListResourceResult(path, res);
      return this.updateViewHint();
    }).finally(() => this.longLoading.decrease());
  }

  private updateViewHint(): Promise<void> {
    return this.storage.getFilePathInfo(this.latestPath).then(info => {
      if (info != null) {
        this.viewHint = FileInfo.toFileSizeHumanReadable(info.bytesFree) + ' free';
      }
    });
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
      return this.api.listFiles(combined).then(res => {
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
    this.dialog.createAThing(
      'directory',
      'Name of the directory',
      name => {
        if (name != null && name.length > 0) {
          const path = this.absoluteDirectoryPath(this.latestPath + '/' + name);
          return this.api.createDirectory(path).then(r => this.navigateDirectlyTo(this.latestPath));
        }
      }
    );
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

  uploadFile(files: FileList, decompress = false) {
    this.prepareDataUpload(files);

    const instance = {
      swal: this.swalUpload.fire(),
      uploader: null
    };

    instance.uploader = (index: number): Promise<void> => {
      if (index < files.length) {
        const upload = this.api.uploadFile(this.latestPath, files.item(index), decompress);
        upload.subscribe(event => {
          if (event.type === HttpEventType.UploadProgress) {
            const now = new Date();
            const timeDiff = now.getTime() - this.dataUpload.uploads[index][4].getTime();
            const byteDiff = event.loaded - this.dataUpload.uploads[index][1];

            this.dataUpload.uploads[index][1] = event.loaded;
            this.dataUpload.uploads[index][2] = event.total;
            this.dataUpload.uploads[index][3] = byteDiff / (timeDiff / 1000);
            this.dataUpload.uploads[index][4] = now;
          }
        });
        return upload
          .toPromise()
          .then(rr => this.loadDirectory(this.latestPath))
          .then(rr => instance.uploader(index + 1))
          .catch(err => {
            this.dataUpload.err = '' + err;
            return Promise.reject();
          });
      } else {
        return Promise.resolve();
      }
    };

    setTimeout(() => Swal.getConfirmButton().setAttribute('disabled', ''));
    instance
      .uploader(0)
      .then(r => this.updateViewHint())
      .finally(() => {
        this.dataUpload.closable = true;
        this.swalUpload.showConfirmButton = true;
        Swal.getConfirmButton().removeAttribute('disabled');
      });
  }

  private prepareDataUpload(files: FileList) {
    this.dataUpload = {
      uploads: [],
      closable: false,
      err: null,
    };
    for (let i = 0; i < files.length; ++i) {
      this.dataUpload.uploads.push([files.item(i).name, 0, 1, 0, new Date()]);
    }
  }

  downloadFile(file: FileInfo) {
    this.api.downloadFile(file.path);
  }

  downloadUrl(file: FileInfo): string {
    return this.api.downloadUrl(file.path);
  }

  delete(file: FileInfo) {
    this.dialog.openAreYouSure(
      `Deleting ${file.directory ? 'directory' : 'file'} ${file.name}`,
      () => this.api.delete(file.path).then(r => this.loadDirectory(this.latestPath))
    );
  }

  onItemSelected(file: FileInfo) {
    this.selectedPath.emit(file.path);
  }

  toggleShowDirectorySize() {
    this.showDirectorySize = !this.showDirectorySize;
    this.dialog.openLoadingIndicator(
      this.loadDirectory(this.latestPath),
      `Updating directory`,
      false
    );
  }

  rename(file: FileInfo) {
    this.dialog.renameAThing(
      file.name,
      'New name',
      name => {
        if (name != null && name.length > 0) {
          return this.api.renameTopLevelPath(file.path, name).then(r => this.navigateDirectlyTo(this.latestPath));
        }
      }
    );
  }

  cloneGitRepo() {
    this.dialog.multiInput(
      `Clone a Git Repository`,
      [
        new InputDefinition('Repository', 'URL'),
        new InputDefinition('Branch', '', 'master')
      ],
      url => {
        return this.api.cloneGitRepo(this.latestPath, url[0], url[1]).then(r => {
          return this.loadDirectory(this.latestPath);
        });
      }
    );
  }

  pullGitRepo() {
    this.dialog.openLoadingIndicator(
      this.api.pullGitRepo(this.latestPath).then(r => {
        return this.loadDirectory(this.latestPath);
      }),
      `Pulling Git Repo`,
      true
    );
  }

  checkoutGitRepo() {
    const fileInfo = this.getCachedFileInfo(this.latestPath);
    this.dialog.multiInput(
      `Git Checkout`,
      [new InputDefinition(`Branch`, null, fileInfo?.getGitBranch())],
      inputs => {
        const branch = inputs[0];
        return this.api.checkoutGitRepo(this.latestPath, branch).then(r => {
          fileInfo.setGitBranch(branch);
          return this.loadDirectory(this.latestPath);
        });
      });
  }

  getCachedFileInfo(path: string): FileInfo {
    if (path.endsWith('/')) {
      path = path.substr(0, path.length - 1);
    }
    const index = path.lastIndexOf('/');
    if (index > 0) {
      const files = this.files.get(path.substr(0, index));
      return files?.find(f => {
        return f.name === path.substr(index + 1);
      });
    } else {
      return null;
    }
  }

  isGitRepo(path: string): boolean {
    const info = this.getCachedFileInfo(path);
    if (info != null) {
      return info.isGitRepository();
    } else {
      return false;
    }
  }

  formatGitBranch(file: FileInfo) {
    if (file.isGitRepository()) {
      return ` (${file.getGitBranch()})`;
    } else {
      return '';
    }
  }

  getSpeed(bytesPerSecond: number): string {
    return FileInfo.toFileSizeHumanReadable(bytesPerSecond) + '/s';
  }

  getRemaining(bytesPerSecond: number, current: number, total: number): string {
    const remaining = total - current;
    const remainingSeconds = remaining / bytesPerSecond;
    console.log(this.toHumanTimeEstimate(remainingSeconds));
    return this.toHumanTimeEstimate(remainingSeconds);
  }

  toHumanTimeEstimate(time: number) {
    let exponent = 0;
    while (Math.round(time / 60) > 0 && exponent < 2) {
      time = Number(time / 60);
      exponent += 1;
    }
    switch (exponent) {
      case 0:
        return Math.max(1, Math.round(time)) + 's';
      case 1:
        return Math.round(time) + 'm';
      case 2:
        return Math.round(time) + 'h';
    }
  }
}

export interface UploadFilesProgress {
  uploads: [string, number, number, number, Date][];
  closable: boolean;
  err: any;
}
