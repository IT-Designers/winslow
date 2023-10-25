import {Component, OnDestroy, OnInit} from '@angular/core';
import {NodesApiService} from '../api/nodes-api.service';
import {StorageApiService} from '../api/storage-api.service';
import {lastValueFrom} from "rxjs";
import {StorageInfo} from "../api/winslow-api";

@Component({
  selector: 'app-system-overview',
  templateUrl: './system-overview.component.html',
  styleUrls: ['./system-overview.component.css']
})
export class SystemOverviewComponent implements OnInit, OnDestroy {

  storageGraphData?: StorageGraphData[];
  loadError = null;
  interval?: ReturnType<typeof setInterval>;

  constructor(private api: NodesApiService, private storageApi: StorageApiService) {
  }

  ngOnInit() {
    this
      .updateStorages()
      .then(() => {
        this.interval = setInterval(() => this.updateStorages(), 10_000);
      });
  }

  ngOnDestroy(): void {
    if (this.interval) {
      clearInterval(this.interval);
      this.interval = undefined;
    }
  }

  private updateStorages() {
    return lastValueFrom(this.storageApi.getAll())
      .then(result => this.storageGraphData = this.toStorageGraphData(result))
      .catch(error => {
        this.storageGraphData = undefined;
        this.loadError = error;
      });
  }

  private toStorageGraphData(result: StorageInfo[]): StorageGraphData[] {
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

interface StorageGraphData {
  name: string,
  series: [
    {
      name: 'used',
      value: number
    },
    {
      name: 'free',
      value: number
    },
  ]
}
