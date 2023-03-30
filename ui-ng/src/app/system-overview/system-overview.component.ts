import {Component, OnDestroy, OnInit} from '@angular/core';
import {NodesApiService} from '../api/nodes-api.service';
import {StorageApiService} from '../api/storage-api.service';

@Component({
  selector: 'app-system-overview',
  templateUrl: './system-overview.component.html',
  styleUrls: ['./system-overview.component.css']
})
export class SystemOverviewComponent implements OnInit, OnDestroy {

  storages: any[] = null;
  loadError = null;
  interval = null;

  constructor(private api: NodesApiService, private storageApi: StorageApiService) {
  }

  ngOnInit() {
    this
      .updateStorages()
      .then(success => {
        this.interval = setInterval(() => this.updateStorages(), 10_000);
      });
  }

  ngOnDestroy(): void {
    if (this.interval) {
      clearInterval(this.interval);
      this.interval = null;
    }
  }

  private updateStorages() {
    return this.storageApi
      .getAll()
      .toPromise()
      .then(result => this.storages = SystemOverviewComponent.convertToNgxDataset(result))
      .catch(error => {
        this.storages = null;
        this.loadError = error;
      });
  }

  private static convertToNgxDataset(result) {
    return result.map(entry => {
      return {
        name: entry.name,
        series: [
          {
            name: 'used',
            value: entry.bytesUsed
          },
          {
            name: 'free',
            value: entry.bytesFree
          }
        ]
      };
    });
  }
}
