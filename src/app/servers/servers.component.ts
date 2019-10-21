import {Component, OnDestroy, OnInit} from '@angular/core';
import {NodeInfo, NodesApiService} from '../api/nodes-api.service';

@Component({
  selector: 'app-servers',
  templateUrl: './servers.component.html',
  styleUrls: ['./servers.component.css']
})
export class ServersComponent implements OnInit, OnDestroy {

  nodes: NodeInfo[] = [];
  loadError = null;
  interval = null;

  constructor(private api: NodesApiService) {
  }

  ngOnInit() {
    this.updateNodes()
      .then(success => this.interval = setInterval(() => this.updateNodes(), 1_000));
  }

  ngOnDestroy(): void {
    if (this.interval) {
      clearInterval(this.interval);
      this.interval = null;
    }
  }

  updateNodes() {
    return this.api
      .getAllNodeInfo()
      .toPromise()
      .then(result => {
        const nodes = result.sort((a, b) => a.name > b.name ? 1 : -1); // sort by name
        // remove missing
        for (let i = 0; i < this.nodes.length; ++i) {
          let found = false;
          for (const node of nodes) {
            if (node.name === this.nodes[i].name) {
              found = true;
              break;
            }
          }
          if (!found) {
            this.nodes.splice(i, 1);
            i -= 1;
          }
        }
        // add new
        for (let i = 0; i < nodes.length; ++i) {
          if (this.nodes.length <= i) {
            this.nodes.push(nodes[i]);
          } else if (this.nodes[i].name !== nodes[i].name) {
            this.nodes.splice(i, 0, nodes[i]);
          } else if (this.nodes[i].update !== null) {
            this.nodes[i].update(nodes[i]);
          }
        }
      })
      .catch(e => {
        this.nodes = null;
        this.loadError = e;
      });
  }
}
