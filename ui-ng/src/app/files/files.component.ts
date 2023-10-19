import {Component, EventEmitter, Input, OnInit, Output, ViewChild} from '@angular/core';
import {HttpEventType} from '@angular/common/http';
import {FilesApiService, humanReadableFileSize} from '../api/files-api.service';
import {LongLoadingDetector} from '../long-loading-detector';
import {DialogService, InputDefinition} from '../dialog.service';
import {SwalComponent, SwalPortalTargets} from '@sweetalert2/ngx-sweetalert2';
import Swal from 'sweetalert2';
import {StorageApiService} from '../api/storage-api.service';
import {FileInfo} from '../api/winslow-api';
import {lastValueFrom} from "rxjs";

@Component({
  selector: 'app-files',
  templateUrl: './files.component.html',
  styleUrls: ['./files.component.css']
})
export class FilesComponent implements OnInit {

  files: Map<string, FileInfo[]>;
  longLoading = new LongLoadingDetector();
  loadError = null;

  showDirectorySize = false;
  latestPath = '/resources'; // IMPORTANT: starts with a slash, but never ends with one: '/resources/ab/cd/ef'
  @Output('selection') selectedPath = new EventEmitter<string>();

  contextMenuX = 0;
  contextMenuY = 0;
  contextMenuVisible = false;

  dataUpload!: UploadFilesProgress;
  viewHint!: string;
  @ViewChild('swalUpload') swalUpload!: SwalComponent;
  @ViewChild('decompress') decompress!: HTMLInputElement;


  constructor(
    private api: FilesApiService,
    private dialog: DialogService,
    private storage: StorageApiService,
    public readonly swalTargets: SwalPortalTargets
  ) {
    const root = [];
    const directory = true;
    const name = 'resources';
    const path = '/resources';
    const info = new FileInfo({name, path, directory, fileSize: 0, attributes: {}});
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
      .then(() => {
        if (this.navigationTarget != null) {
          return this.navigateDirectlyTo(this.navigationTarget);
        }
      }).catch(error => {
      this.loadError = error;
    }).finally(() => this.longLoading.decrease());
  }

  @Input()
  public set additionalRoot(value: string) {
    const directory = true;
    const name = value.split(';')[0];
    const path = `/${value.split(';')[1]}`;
    const additional = new FileInfo({name, directory, path, fileSize: 0, attributes: {}});
    this.files.get('/')?.splice(1);
    this.files.get('/')?.push(additional);
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
        this.viewHint = humanReadableFileSize(info.bytesFree) + ' free';
      }
    });
  }

  currentDirectory(): FileInfo[] {
    return this.files.get(this.latestPath) ?? [];
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
      async name => {
        if (name != null && name.length > 0) {
          const path = this.absoluteDirectoryPath(this.latestPath + '/' + name);
          await this.api.createDirectory(path)
          await this.navigateDirectlyTo(this.latestPath);
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
      elements.item(i)?.classList.remove(CLASS_NAME_SELECTED);
    }
    const directories = document.getElementsByClassName(CLASS_NAME);
    for (let i = 0; i < directories.length; ++i) {
      const value = directories.item(i)?.attributes.getNamedItem(DATA_ATTRIBUTE)?.value;
      if (value === this.latestPath) {
        directories.item(i)?.classList.add(CLASS_NAME_SELECTED);
        break;
      }
    }
  }

  uploadFile(files: FileList, decompress = false) {
    if (this.swalUpload == undefined) {
      this.dialog.error('Failed to upload file: swalUpload is not initialized!')
      return;
    }

    this.prepareDataUpload(files);

    const instance = {
      swal: this.swalUpload.fire(),
      uploader: null as any,
      updater: null as any,
    };

    instance.uploader = (index: number): Promise<void> => {
      if (index >= files.length) {
        return Promise.resolve();
      }
      const file = files.item(index);
      if (file == undefined) {
        return Promise.reject();
      }
      const upload = this.api.uploadFile(this.latestPath, file, decompress);
      upload.subscribe(event => {
        if (event.type !== HttpEventType.UploadProgress && event.type !== HttpEventType.ResponseHeader) {
          return;
        }
        const now = new Date();
        if (event.type !== HttpEventType.UploadProgress || event.total == undefined) {
          this.dataUpload.uploads[index].completed = true;
        } else {
          const MOVING_AVERAGE_SAMPLES = 20;
          const timeDiff = now.getTime() - this.dataUpload.uploads[index].currentUploadSpeedLastUpdate.getTime();
          const byteDiff = event.loaded - this.dataUpload.uploads[index].loaded;
          const byteSec = byteDiff / (timeDiff / 1000);

          this.dataUpload.uploads[index].loaded = event.loaded;
          this.dataUpload.uploads[index].total = event.total;
          this.dataUpload.uploads[index].currentUploadSpeed = (
            (MOVING_AVERAGE_SAMPLES - 1 + this.dataUpload.uploads[index].currentUploadSpeedLastTimeDiff)
            * this.dataUpload.uploads[index].currentUploadSpeed
            + (byteSec * timeDiff)
          ) / (MOVING_AVERAGE_SAMPLES + this.dataUpload.uploads[index].currentUploadSpeedLastTimeDiff + timeDiff);
          this.dataUpload.uploads[index].currentUploadSpeedLastUpdate = now;
          this.dataUpload.uploads[index].currentUploadSpeedLastTimeDiff = timeDiff;
        }
        this.updateProgress(now, this.dataUpload.uploads[index]);
      });
      return lastValueFrom(upload)
        .then(_r => this.loadDirectory(this.latestPath))
        .then(_r => instance.uploader(index + 1))
        .catch(err => {
          this.dataUpload.err = '' + err;
          return Promise.reject();
        });
    };

    setTimeout(() => Swal.getConfirmButton()?.setAttribute('disabled', ''));

    instance.updater = setInterval(() => this.updateOverall(), 1000);
    instance
      .uploader(0)
      .then(() => this.updateViewHint())
      .finally(() => {
        this.dataUpload.closable = true;
        this.swalUpload.showConfirmButton = true;
        clearInterval(instance.updater);
        Swal.getConfirmButton()?.removeAttribute('disabled');
      });
  }

  private updateOverall(now: Date = new Date()) {
    for (const progress of this.dataUpload.uploads) {
      if (progress.loaded > 0 && !progress.completed) {
        this.updateProgress(now, progress);
      }
    }
  }

  private updateProgress(now: Date, progress: UploadProgress) {
    const timeSinceStart = now.getTime() - progress.overallUploadStarted.getTime();
    progress.overallUploadSpeed = progress.loaded / (timeSinceStart / 1000);
  }

  private prepareDataUpload(files: FileList) {
    this.dataUpload = {
      uploads: [],
      closable: false,
      err: null,
    };
    for (let i = 0; i < files.length; ++i) {
      const file = files.item(i);
      if (file != null) {
        this.dataUpload.uploads.push({
          name: file.name,
          loaded: 0,
          total: 1,
          currentUploadSpeed: 0,
          currentUploadSpeedLastUpdate: new Date(),
          currentUploadSpeedLastTimeDiff: 0,
          overallUploadSpeed: 0,
          overallUploadStarted: new Date(),
          completed: false
        });
      }
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
      () => this.api.delete(file.path).then(() => this.loadDirectory(this.latestPath))
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
      async name => {
        if (name != null && name.length > 0) {
          await this.api.renameTopLevelPath(file.path, name).then(() => this.navigateDirectlyTo(this.latestPath));
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
        return this.api.cloneGitRepo(this.latestPath, url[0], url[1]).then(() => {
          return this.loadDirectory(this.latestPath);
        });
      }
    );
  }

  pullGitRepo() {
    this.dialog.openLoadingIndicator(
      this.api.pullGitRepo(this.latestPath).then(() => {
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
      [new InputDefinition(`Branch`, undefined, fileInfo?.getGitBranch())],
      async inputs => {
        const branch = inputs[0];
        await this.api.checkoutGitRepo(this.latestPath, branch)
        fileInfo?.setGitBranch(branch);
        return await this.loadDirectory(this.latestPath);
      }
    );
  }

  getCachedFileInfo(path: string): FileInfo | undefined {
    if (path.endsWith('/')) {
      path = path.substring(0, path.length - 1);
    }
    const index = path.lastIndexOf('/');
    if (index > 0) {
      const files = this.files.get(path.substring(0, index));
      return files?.find(file => file.name === path.substring(index + 1));
    }
    return undefined;
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
    return humanReadableFileSize(bytesPerSecond) + '/s';
  }

  getRemaining(bytesPerSecond: number, current: number, total: number): string {
    const remaining = total - current;
    const remainingSeconds = remaining / bytesPerSecond;
    return this.toHumanTimeEstimate(remainingSeconds);
  }

  toHumanTimeEstimate(seconds: number) {
    const minutes = seconds / 60;
    const hours = minutes / 60;
    const days = hours / 24;

    if (days > 1) {
      const daysD = Math.round(days);
      return daysD + 'd ' + Math.round((days - daysD) * 24) + 'h';

    } else if (hours > 1) {
      const hoursD = Math.round(hours);
      return hoursD + 'h ' + Math.round((hours - hoursD) * 60) + 'm';

    } else if (minutes > 1) {
      const minutesD = Math.round(minutes);
      return minutesD + 'm ' + Math.round((minutes - minutesD) * 60) + 's';

    } else {
      return Math.max(1, Math.round(seconds)) + 's';
    }
  }

  private addLeadingZero(input: number): string {
    if (input < 10) {
      return '0' + input;
    } else {
      return '' + input;
    }
  }

  timeStampToDate(timeStamp: number) {
    const noFormat = new Date(timeStamp);
    return noFormat.getFullYear()
      + '/' + this.addLeadingZero(noFormat.getMonth())
      + '/' + this.addLeadingZero(noFormat.getDay())
      + ', ' + this.addLeadingZero(noFormat.getHours())
      + ':' + this.addLeadingZero(noFormat.getMinutes());
  }

  onFileInputChange(event: Event) : void{
    const target = event.target;
    if (target != null && 'files' in target && target.files instanceof FileList) {
      this.uploadFile(target.files, this.decompress.value.toLowerCase() == 'true')
    } else {
      console.error("Cannot upload file as fileUpload target has no file list.");
    }
  }
}

export interface UploadProgress {
  name: string;
  loaded: number;
  total: number;
  currentUploadSpeed: number;
  currentUploadSpeedLastUpdate: Date;
  currentUploadSpeedLastTimeDiff: number;
  overallUploadSpeed: number;
  overallUploadStarted: Date;
  completed: boolean;
}

export interface UploadFilesProgress {
  uploads: UploadProgress[];
  closable: boolean;
  err: any;
}
