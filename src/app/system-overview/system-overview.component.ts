import {Component, OnInit, ViewChild} from '@angular/core';
import {NodeInfo, NodesApiService} from '../nodes-api.service';
import {NotificationService} from '../notification.service';

@Component({
  selector: 'app-system-overview',
  templateUrl: './system-overview.component.html',
  styleUrls: ['./system-overview.component.css']
})
export class SystemOverviewComponent implements OnInit {

  nodes: NodeInfo[] = null;

  constructor(private api: NodesApiService, private notification: NotificationService) {
  }

  ngOnInit() {
    this.api.getAllNodeInfo().toPromise()
      .then(result => this.nodes = result)
      .catch(error => this.notification.error(error));
  }
}
