import { Component, OnDestroy, OnInit } from "@angular/core";
import { GpuInfo, NodeInfo, NodesApiService } from "../api/nodes-api.service";
import { Subscription } from "rxjs";
import { ChangeType } from "../api/api.service";

@Component({
  selector: "app-servers",
  templateUrl: "./servers.component.html",
  styleUrls: ["./servers.component.css"],
})
export class ServersComponent implements OnInit, OnDestroy {

  // needed for access static readonly in template
  public ServersComponentClass = ServersComponent;

  // max amount of server to display without scrollbar
  static readonly MAX_SERVERS = 7;

  nodes: NodeInfo[] = [];
  node: NodeInfo;
  selectedNodeIndex: number = null;
  loadError = null;
  subscription: Subscription = null;
  lastTimestamp: number;

  constructor(private api: NodesApiService) {
  }

  ngOnInit() {
    this.subscription = this.api.watchNodes((update) => {
      switch (update.type) {
        case ChangeType.CREATE:
        case ChangeType.UPDATE:
          if (update.value != null) {
            const indexUpdate = this.nodes.findIndex(
              (value) => value.name === update.identifier
            );
            if (indexUpdate >= 0) {
              if (this.nodes[indexUpdate]?.update != null) {
                this.nodes[indexUpdate]?.update(update.value);
              }
            } else {
              this.nodes.push(update.value);
              this.sortNodesByName();
            }

            if(this.selectedNodeIndex == null) {
              this.selectedNodeIndex = 0;
              this.node = this.nodes[0];
            } else {
              this.node = this.nodes[this.selectedNodeIndex];
            }

            // save last timestamp
            if(!this.lastTimestamp) {
              this.lastTimestamp = this.node.time;
            }

            // check if new timestamp is different
            // if yes, update diagrams
            if (this.lastTimestamp != this.node.time) {
              this.lastTimestamp = this.node.time;
            }
          }
          break;
        case ChangeType.DELETE:
          const indexDelete = this.nodes.findIndex(
            (value) => value.name === update.identifier
          );
          if (indexDelete >= 0) {
            this.nodes.splice(indexDelete, 1);
          }
          break;
      }
    });
  }

  sortNodesByName() {
    this.nodes = this.nodes.sort((a, b) => (a.name > b.name ? 1 : -1));
  }

  ngOnDestroy(): void {
    if (this.subscription) {
      this.subscription.unsubscribe();
      this.subscription = null;
    }
  }

  public trackNodeInfo(nodeInfo: NodeInfo): string {
    return nodeInfo?.name;
  }

  setNode(index: number, ) {
    this.selectedNodeIndex = index;
  }

}
