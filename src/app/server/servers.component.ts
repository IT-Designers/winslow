import { Component, OnInit } from '@angular/core';
import {NodeInfo, NodesApiService} from '../api/nodes-api.service';
import {StorageApiService} from '../api/storage-api.service';
import {NotificationService} from '../notification.service';

@Component({
  selector: 'app-server',
  templateUrl: './servers.component.html',
  styleUrls: ['./servers.component.css']
})
export class ServersComponent implements OnInit {

  nodes: NodeInfo[] = null;

  constructor(private api: NodesApiService, private notification: NotificationService) { }

  ngOnInit() {
    this.api.getAllNodeInfo().toPromise()
      .then(result => this.nodes = result)
      .catch(error => this.notification.error(error));
  }

}
