import {Component, Input, OnInit} from '@angular/core';
import {DiagramMakerNode} from "diagram-maker";
import {StageDefinitionInfo} from "../../api/project-api.service";

@Component({
  selector: 'app-diagram-gateway',
  templateUrl: './diagram-gateway.component.html',
  styleUrls: ['./diagram-gateway.component.css']
})
export class DiagramGatewayComponent implements OnInit {

  @Input() nodeTypeName: String;
  node$?: DiagramMakerNode<{}>;
  selected$?: boolean = false;
  containsNode?: boolean = false;
  splitter: boolean = false;
  combiner: boolean = false;

  constructor() {
  }

  ngOnInit(): void {
    if (this.node$ !== undefined) {
      this.containsNode = true;
    }
    if (this.nodeTypeName == "AND Splitter" || this.nodeTypeName == "IF Splitter" ||
      this.node$?.typeId == "node-and-splitter" || this.node$?.typeId == "node-if-splitter"
    ) {
      this.splitter = true;
    }
    if (this.nodeTypeName == "All Combiner" || this.nodeTypeName == "Priority Combiner" ||
      this.node$?.typeId == "node-all-combiner" || this.node$?.typeId == "node-prio-combiner"
    ) {
      this.combiner = true;
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


