import {Injectable} from '@angular/core';
import {environment} from '../../environments/environment';
import {HttpClient} from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class FilesApiService {

  constructor(private client: HttpClient) {
  }


  listFiles(path: string) {
    if (path.startsWith('/')) {
      path = path.substr(1);
    }
    return this.client
      .options<FileInfo[]>(environment.apiLocation + 'files/' + path)
      .toPromise()
      .then(files => files.map(f => new FileInfo(f)));
  }

  createDirectory(path: string): Promise<any> {
    if (path.startsWith('/')) {
      path = path.substr(1);
    }
    return this
      .client
      .put(environment.apiLocation + 'files/' + path, null)
      .toPromise();
  }

  uploadFile(pathToDirectory: string, file: File) {
    if (!pathToDirectory.endsWith('/')) {
      pathToDirectory += '/';
    }
    if (pathToDirectory.startsWith('/')) {
      pathToDirectory = pathToDirectory.substr(1);
    }

    const form = new FormData();
    form.append('file', file);

    return this
      .client
      .post(
        environment.apiLocation + 'files/' + pathToDirectory + file.name,
        form,
        {reportProgress: true, observe: 'events'}
      );
  }

  filesUrl(pathToFile: string): string {
    while (pathToFile.startsWith('/')) {
      pathToFile = pathToFile.substr(1);
    }
    return `${environment.apiLocation}files/${pathToFile}`;
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
      .delete(environment.apiLocation + 'files/' + path);
  }

}


export class FileInfo {
  name: string;
  directory: boolean;
  path: string;
  fileSize?: number;
  // local only
  fileSizeHumanReadable?: string;

  constructor(info: FileInfo = null) {
    if (info != null) {
      this.name = info.name;
      this.directory = info.directory;
      this.path = info.path;
      this.fileSize = info.fileSize;
      this.fileSizeHumanReadable = FileInfo.getFileSizeHumanReadable(info.fileSize);
    }
  }

  private static getFileSizeHumanReadable(fileSize?: number): string {
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
}
