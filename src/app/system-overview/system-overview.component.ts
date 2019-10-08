import {Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {NodeInfo, NodesApiService} from '../api/nodes-api.service';
import {NotificationService} from '../notification.service';
import {StorageApiService} from '../api/storage-api.service';

@Component({
  selector: 'app-system-overview',
  templateUrl: './system-overview.component.html',
  styleUrls: ['./system-overview.component.css']
})
export class SystemOverviewComponent implements OnInit, OnDestroy {

  nodes: NodeInfo[] = null;
  storages: any[] = [];
  interval = null;

  constructor(private api: NodesApiService, private storageApi: StorageApiService, private notification: NotificationService) {
  }

  ngOnInit() {
    this.api.getAllNodeInfo().toPromise()
      .then(result => this.nodes = result)
      .catch(error => this.notification.error(error));
    this.updateStorages();
    this.interval = setInterval(() => this.updateStorages(), 10_000);
  }

  ngOnDestroy(): void {
    if (this.interval) {
      clearInterval(this.interval);
      this.interval = null;
    }
  }

  private updateStorages() {
    this.storageApi.getAll().toPromise()
      .then(result => this.storages = SystemOverviewComponent.convertToNgxDataset(result))
      .catch(error => this.notification.error(error));
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
