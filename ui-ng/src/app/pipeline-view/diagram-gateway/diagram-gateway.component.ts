import {Component, Input, OnInit} from '@angular/core';
import {DiagramMakerNode} from "diagram-maker";
import {StageDefinitionInfo} from "../../api/winslow-api";

@Component({
  selector: 'app-diagram-gateway',
  templateUrl: './diagram-gateway.component.html',
  styleUrls: ['./diagram-gateway.component.css']
})
export class DiagramGatewayComponent implements OnInit {

  @Input() nodeTypeName: String = "";
  gatewayName: String = "";
  node$?: DiagramMakerNode<{}>;
  selected$?: boolean = false;
  containsNode?: boolean = false;
  splitter: boolean = false;
  merger: boolean = false;

  constructor() {
  }

  ngOnInit(): void {
    if (this.node$ !== undefined) {
      this.containsNode = true;
    }
    if (this.nodeTypeName == "AND Splitter" || this.nodeTypeName == "IF Splitter" ||
      this.node$?.typeId == "node-and-splitter" || this.node$?.typeId == "node-if-splitter"
    ){
      this.splitter = true;
      if (this.node$?.typeId == "node-and-splitter" || this.nodeTypeName == "AND Splitter"){ this.gatewayName = "AND Splitter"}
      else {this.gatewayName = "IF Splitter"}
    }
    if (this.nodeTypeName == "ALL Merger" || this.nodeTypeName == "ANY Merger"||
      this.node$?.typeId == "node-all-merger" || this.node$?.typeId == "node-any-merger"
    ){
      this.merger = true;
      if (this.node$?.typeId == "node-all-merger" || this.nodeTypeName == "ALL Merger"){ this.gatewayName = "ALL Merger"}
      else {this.gatewayName = "ANY Merger"}
    }

  }

  @Input()
  set node(node: DiagramMakerNode<StageDefinitionInfo>) {
    this.node$ = node;
  }

  @Input()
  set selected(selected: boolean) {
    this.selected$ = selected;
  }
}


