import {Injectable} from '@angular/core';
import {environment} from '../../environments/environment';
import {HttpClient} from '@angular/common/http';
import {IFileInfo} from './winslow-api';

@Injectable({
  providedIn: 'root'
})
export class FilesApiService {

  constructor(private client: HttpClient) {
  }

  static getUrl(more?: string) {
    if (more != null) {
      while (more.startsWith('/')) {
        more = more.substr(1);
      }
      more = more.split(';').join('%3B');
    }
    return `${environment.apiLocation}files${more != null ? `/${more}` : ''}`;
  }

  listFiles(path: string, aggregateSizeForDirectories: boolean = false) {
    return this.client
      .options<IFileInfo[]>(FilesApiService.getUrl(path) + (aggregateSizeForDirectories ? '?aggregateSizeForDirectories=true' : ''))
      .toPromise()
      .then(files => files.map(f => new IFileInfoExt(f)));
  }

  createDirectory(path: string): Promise<any> {
    return this
      .client
      .post(FilesApiService.getUrl(path), null)
      .toPromise();
  }

  uploadFile(pathToDirectory: string, file: File, decompress = false) {
    if (!pathToDirectory.endsWith('/')) {
      pathToDirectory += '/';
    }

    let params = '?';

    if (decompress) {
      params += 'decompressArchive=true';
    }

    const form = new FormData();
    form.append('file', file);

    return this.client.put(
      FilesApiService.getUrl(pathToDirectory + file.name + params),
      form,
      {reportProgress: true, observe: 'events'}
    );
  }

  getFile(pathToFile: string, compress = false) {
    let params = '?';

    if (compress) {
      params += 'compressToArchive=true';
    }

    return this.client.get(
      FilesApiService.getUrl(pathToFile + params), {responseType: 'text'}
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
    return this
      .client
      .delete(FilesApiService.getUrl(path))
      .toPromise();
  }

  renameTopLevelPath(path: string, newName: string): Promise<string> {
    return this.client
      .patch<string>(FilesApiService.getUrl(path), {
        'rename-to': newName,
      })
      .toPromise();
  }

  cloneGitRepo(path: string, gitUrl: string, gitBranch?: string): Promise<string> {
    return this.client.patch<string>(FilesApiService.getUrl(path), {
      'git-clone': gitUrl,
      'git-branch': gitBranch,
    }).toPromise();
  }

  pullGitRepo(path: string): Promise<string> {
    return this.client.patch<string>(FilesApiService.getUrl(path), {
      'git-pull': ``
    }).toPromise();
  }

  checkoutGitRepo(path: string, branch: string): Promise<void> {
    return this.client.patch<void>(FilesApiService.getUrl(path), {
      'git-checkout': branch
    }).toPromise();
  }
}

export class IFileInfoExt extends IFileInfo {

  fileSizeHumanReadableCached?: string;

  public static toFileSizeHumanReadable(fileSize?: number): string {
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

  public getAttribute(key: FileInfoAttribute): unknown {
    return this.attributes != null ? this.attributes[key] : null;
  }

  public hasAttribute(key: FileInfoAttribute): boolean {
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

  public setGitBranch(branch: string) {
    if (this.attributes == null) {
      this.attributes = new Map<string, unknown>();
    }
    this.attributes['git-branch'] = branch;
  }

  public getFileSizeHumanReadable(): string {
    if (this.fileSizeHumanReadableCached == null) {
      this.fileSizeHumanReadableCached = IFileInfoExt.toFileSizeHumanReadable(this.fileSize);
    }
    return this.fileSizeHumanReadableCached;
  }
}

export type FileInfoAttribute = 'git-branch';
