import {Injectable} from '@angular/core';
import {environment} from '../../environments/environment';
import {HttpClient} from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class FilesApiService {

  constructor(private client: HttpClient) {
  }

  static getUrl(more?: string) {
    return `${environment.apiLocation}files${more != null ? `/${more}` : ''}`;
  }

  listFiles(path: string, aggregateSizeForDirectories: boolean = false) {
    if (path.startsWith('/')) {
      path = path.substr(1);
    }
    return this.client
      .options<FileInfo[]>(FilesApiService.getUrl(path) + (aggregateSizeForDirectories ? '?aggregateSizeForDirectories=true' : ''))
      .toPromise()
      .then(files => files.map(f => new FileInfo(f)));
  }

  createDirectory(path: string): Promise<any> {
    if (path.startsWith('/')) {
      path = path.substr(1);
    }
    return this
      .client
      .put(FilesApiService.getUrl(path), null)
      .toPromise();
  }

  uploadFile(pathToDirectory: string, file: File, decompress = false) {
    if (!pathToDirectory.endsWith('/')) {
      pathToDirectory += '/';
    }
    if (pathToDirectory.startsWith('/')) {
      pathToDirectory = pathToDirectory.substr(1);
    }

    let params = '?';

    if (decompress) {
      params += 'decompressArchive=true';
    }

    const form = new FormData();
    form.append('file', file);

    return this
      .client
      .post(
        FilesApiService.getUrl(pathToDirectory + file.name + params),
        form,
        {reportProgress: true, observe: 'events'}
      );
  }

  filesUrl(pathToFile: string): string {
    while (pathToFile.startsWith('/')) {
      pathToFile = pathToFile.substr(1);
    }
    return FilesApiService.getUrl(pathToFile);
  }

  workspaceUrl(pathToFile: string): string {
    return this.filesUrl(`workspaces/${pathToFile}`);
  }

  downloadUrl(pathToFile: string) {
    return this.filesUrl(pathToFile);
  }

  downloadFile(pathToFile: string) {
    window.location.href = this.downloadUrl(pathToFile);
  }

  delete(path: string) {
    if (path.startsWith('/')) {
      path = path.substr(1);
    }
    return this
      .client
      .delete(FilesApiService.getUrl(path))
      .toPromise();
  }

  renameTopLevelPath(path: string, newName: string): Promise<string> {
    if (path.startsWith('/')) {
      path = path.substr(1);
    }
    return this.client
      .patch<string>(FilesApiService.getUrl(path), {
        'rename-to': newName,
      })
      .toPromise();
  }

  cloneGitRepo(path: string, gitUrl: string, gitBranch?: string): Promise<string> {
    if (path.startsWith('/')) {
      path = path.substr(1);
    }
    return this.client.patch<string>(FilesApiService.getUrl(path), {
      'git-clone': gitUrl,
      'git-branch': gitBranch,
    }).toPromise();
  }

  pullGitRepo(path: string): Promise<string> {
    if (path.startsWith('/')) {
      path = path.substr(1);
    }
    return this.client.patch<string>(FilesApiService.getUrl(path), {
      'git-pull': ``
    }).toPromise();
  }
}


export class FileInfo {
  name: string;
  directory: boolean;
  path: string;
  fileSize?: number;
  attributes: Map<string, unknown>;
  // local only
  fileSizeHumanReadableCached?: string;

  constructor(info: FileInfo = null) {
    if (info != null) {
      this.name = info.name;
      this.directory = info.directory;
      this.path = info.path;
      this.fileSize = info.fileSize;
      this.attributes = info.attributes;
      this.fileSizeHumanReadableCached = FileInfo.toFileSizeHumanReadable(info.fileSize);
    }
  }


  static toFileSizeHumanReadable(fileSize?: number): string {
    if (fileSize != null) {
      let suffix = 0;
      let value = fileSize;

      while (value >= 1024) {
        suffix += 1;
        value /= 1024;
      }
      const prefix = value.toFixed(1);
      switch (suffix) {
        case 0:
          return prefix + ' bytes';
        case 1:
          return prefix + ' KiB';
        case 2:
          return prefix + ' MiB';
        case 3:
          return prefix + ' GiB';
        case 4:
          return prefix + ' TiB';
        case 5:
          return prefix + ' PiB';
        default:
          return fileSize + ' bytes';
      }
    } else {
      return null;
    }
  }

  public getAttribute(key: string): unknown {
    return this.attributes != null ? this.attributes[key] : null;
  }

  public hasAttribute(key: string): boolean {
    return this.attributes != null && this.attributes[key] != null;
  }

  public isGitRepository(): boolean {
    return this.hasAttribute('git-branch');
  }

  public getGitBranch(): string {
    const attr = this.getAttribute('git-branch');
    if (typeof attr === typeof '') {
      return attr as string;
    } else {
      return null;
    }
  }

  public getFileSizeHumanReadable(): string {
    if (this.fileSizeHumanReadableCached == null) {
      this.fileSizeHumanReadableCached = FileInfo.toFileSizeHumanReadable(this.fileSize);
    }
    return this.fileSizeHumanReadableCached;
  }
}
