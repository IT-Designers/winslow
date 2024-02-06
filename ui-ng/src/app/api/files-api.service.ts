import {Injectable} from '@angular/core';
import {environment} from '../../environments/environment';
import {HttpClient} from '@angular/common/http';
import {FileInfo} from './winslow-api';
import {lastValueFrom} from "rxjs";

@Injectable({
  providedIn: 'root'
})
export class FilesApiService {

  constructor(private client: HttpClient) {
  }

  static getUrl(more?: string) {
    if (more != undefined) {
      while (more?.startsWith('/')) {
        more = more?.substring(1);
      }
      more = more.split(';').join('%3B');
    }
    return `${environment.apiLocation}files${more != null ? `/${more}` : ''}`;
  }

  listFiles(path: string, aggregateSizeForDirectories: boolean = false) {
    return lastValueFrom(
      this.client
        .options<FileInfo[]>(FilesApiService.getUrl(path) + (aggregateSizeForDirectories ? '?aggregateSizeForDirectories=true' : ''))
    ).then(files => files.map(f => loadFileInfoHelper(f)));
  }

  createDirectory(path: string): Promise<any> {
    return lastValueFrom(
      this.client.post(FilesApiService.getUrl(path), null)
    );
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
      pathToFile = pathToFile.substring(1);
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
    return lastValueFrom(
      this.client.delete(FilesApiService.getUrl(path))
    );
  }

  renameTopLevelPath(path: string, newName: string): Promise<string> {
    return lastValueFrom(
      this.client.patch<string>(FilesApiService.getUrl(path), {
        'rename-to': newName,
      })
    );
  }

  cloneGitRepo(path: string, gitUrl: string, gitBranch?: string): Promise<string> {
    return lastValueFrom(
      this.client.patch<string>(FilesApiService.getUrl(path), {
        'git-clone': gitUrl,
        'git-branch': gitBranch,
      })
    );
  }

  pullGitRepo(path: string): Promise<string> {
    return lastValueFrom(
      this.client.patch<string>(FilesApiService.getUrl(path), {
        'git-pull': ``
      })
    );
  }

  checkoutGitRepo(path: string, branch: string): Promise<void> {
    return lastValueFrom(
      this.client.patch<void>(FilesApiService.getUrl(path), {
        'git-checkout': branch
      })
    );
  }
}

export function humanReadableFileSize(fileSize?: number): string | undefined {
  if (fileSize != undefined) {
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
    return undefined;
  }
}

export type FileInfoAttribute = 'git-branch';

export function loadFileInfoHelper(origin: FileInfo): FileInfoHelper {
  return new FileInfoHelper(origin);
}

export class FileInfoHelper {
  fileInfo: FileInfo;
  fileSizeHumanReadableCached?: string;

  constructor(fileInfo: FileInfo) {
    this.fileInfo = fileInfo;
  }

  getAttribute(key: FileInfoAttribute): any {
    return this.fileInfo.attributes != null ? this.fileInfo.attributes[key] : null;
  }

  getFileSizeHumanReadable(): string | undefined {
    if (this.fileSizeHumanReadableCached == undefined) {
      this.fileSizeHumanReadableCached = humanReadableFileSize(this.fileInfo.fileSize);
    }
    return this.fileSizeHumanReadableCached;
  }

  hasAttribute(key: FileInfoAttribute): boolean {
    return this.fileInfo.attributes != null && this.fileInfo.attributes[key] != null;
  }

  isGitRepository(): boolean {
    return this.hasAttribute('git-branch');
  };

  getGitBranch(): string | undefined {
    const attr = this.getAttribute('git-branch');
    if (typeof attr === typeof '') {
      return attr as string;
    } else {
      return undefined;
    }
  };

  setGitBranch(branch: string): void {
    if (this.fileInfo.attributes == null) {
      this.fileInfo.attributes = new Map<string, unknown>();
    }
    this.fileInfo.attributes['git-branch'] = branch;
  };
}
