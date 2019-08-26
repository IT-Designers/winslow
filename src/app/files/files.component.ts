import {ChangeDetectorRef, Component, OnInit} from '@angular/core';
import {ApiService, FileInfo} from '../api.service';

@Component({
  selector: 'app-files',
  templateUrl: './files.component.html',
  styleUrls: ['./files.component.css']
})
export class FilesComponent implements OnInit {
  files: Map<string, FileInfo[]> = new Map();
  latestPath = 'resources/';

  contextMenuX = 0;
  contextMenuY = 0;
  contextMenuVisible = false;

  constructor(private api: ApiService, private changeDetector: ChangeDetectorRef) {
    this.files.set('/', []);
  }

  ngOnInit() {
    this.api.listResources('/resources/').toPromise().then(res => this.files.get('/').push((() => {
      const info = new FileInfo();
      info.directory = true;
      info.name = 'resources';
      info.path = 'resources/';
      this.files.set('/resources/', res);
      return info;
    })()));
  }

  directories(path: string) {
    const files = this.files.get(path);
    return files != null ? files.filter(f => f.directory) : [];
  }

  private deleteRecursively(path: string) {
    const info = this.files.get(path);
    if (info != null) {
      for (const i of info) {
        this.deleteRecursively(i.path);
      }
    }
    this.files.delete(path);
  }

  toggleLoadDirectory(path: string) {
    path = this.absoluteDirectoryPath(path);
    this.latestPath = path;
    if (this.files.has(path)) {
      this.deleteRecursively(path);
    } else {
      this.loadDirectory(path);
    }
  }

  private insertListResourceResult(path: string, res: FileInfo[]) {
    this.files.set(
      this.absoluteDirectoryPath(path),
      res
        .sort((a, b) => a.name > b.name ? 1 : -1)
        .map(file => {
          file.path = this.absoluteDirectoryPath(file.path, file.directory);
          return file;
        })
    );
  }

  private loadDirectory(path: string) {
    this.api.listResources(path).toPromise().then(res => {
      this.insertListResourceResult(path, res);
    });
    this.latestPath = path;
  }

  currentDirectoryFilesOnly(): FileInfo[] {
    return this.currentDirectory().filter(info => !info.directory);
  }

  currentDirectory(): FileInfo[] {
    return this.files.has(this.latestPath) ? this.files.get(this.latestPath) : [];
  }

  viewDirectory(path: string) {
    this.latestPath = this.absoluteDirectoryPath(path);
    this.loadDirectory(this.latestPath);
  }

  updateCurrentDirectory(value: string) {
    value = this.absoluteDirectoryPath(value);
    if (this.latestPath !== value) {
      this.recursivelyLoadDirectoriesOfPath(value.split('/'));
    }
  }

  private recursivelyLoadDirectoriesOfPath(pathSplit: string[], currentIndex = 0, combined = '') {
    if (currentIndex < pathSplit.length - 1) {
      combined += pathSplit[currentIndex] + '/';
      this.api.listResources(combined).toPromise().then(res => {
        if (res != null && res.length > 0) {

          this.insertListResourceResult(combined, res);
          this.latestPath = combined;

          for (const r of res) {
            if (r.name === pathSplit[currentIndex + 1]) {
              this.recursivelyLoadDirectoriesOfPath(pathSplit, currentIndex + 1, combined);
              break;
            }
          }
        }
      });
    }
  }

  absoluteDirectoryPath(path: string, directory = true): string {
    if (!path.endsWith('/') && directory) {
      path += '/';
    }
    if (path.startsWith('/')) {
      path = path.substr(1);
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
    console.log('clicked');
  }

  uploadFile($event) {
    console.log('upload');
  }
}
