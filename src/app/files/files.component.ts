import { Component, OnInit } from '@angular/core';
import {ApiService, FileInfo} from '../api.service';

@Component({
  selector: 'app-files',
  templateUrl: './files.component.html',
  styleUrls: ['./files.component.css']
})
export class FilesComponent implements OnInit {
  files: Map<string, FileInfo[]> = new Map();

  constructor(private api: ApiService) {
    this.files.set('/', []);
  }

  ngOnInit() {
    this.api.listResources('/resources/').toPromise().then(res => this.files.get('/').push((() => {
      const info = new FileInfo();
      info.directory = true;
      info.name = 'resources';
      info.path = '/resources/';
      this.files.set('/resources/', res);
      return info;
    })()));
  }

  directories(path: string) {
    const files = this.files.get(path);
    const result = [];
    if (files != null) {
      for (const v of files) {
        if (v.directory) {
          result.push(v);
        }
      }
    }
    return result;
  }

  deleteRecursively(path: string) {
    const info = this.files.get(path);
    if (info != null) {
      for (const i of info) {
        this.deleteRecursively(i.path);
      }
    }
    this.files.delete(path);
  }

  toggleLoadDirectory(path: string) {
    console.log(path);
    if (this.files.has(path)) {
      this.deleteRecursively(path);
    } else {
      this.api.listResources(path).toPromise().then(res => {
        this.files.set(path, res);
        console.log(JSON.stringify(res));
      });
    }
  }
}
