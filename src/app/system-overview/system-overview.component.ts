import {Component, OnInit, ViewChild} from '@angular/core';
import {NodeInfo, NodesApiService} from '../nodes-api.service';

@Component({
  selector: 'app-system-overview',
  templateUrl: './system-overview.component.html',
  styleUrls: ['./system-overview.component.css']
})
export class SystemOverviewComponent implements OnInit {

  node0: NodeInfo = null;

  constructor(private nodes: NodesApiService) {
  }

  ngOnInit() {
    this.nodes.getNodeInfo('node0').toPromise().then(result => this.node0 = result);
  }
}
