import {Component, Input, OnInit} from '@angular/core';
import {DiagramMakerNode} from "diagram-maker";
import {StageDefinitionInfo, StageWorkerDefinitionInfo} from "../../api/winslow-api";

@Component({
  selector: 'app-diagram-node',
  templateUrl: './diagram-node.component.html',
  styleUrls: ['./diagram-node.component.css']
})
export class DiagramNodeComponent implements OnInit {

  @Input() nodeTypeName : String;
  node$?: DiagramMakerNode<StageWorkerDefinitionInfo>;
  selected$?: boolean = false;
  containsNode?: boolean = false;
  isPipelineDef: boolean = false;

  constructor() {
  }

  ngOnInit(): void {
    if (this.node$ !== undefined) {
      this.containsNode = true;
    }
    if(this.node$.typeId == "node-start"){
      this.isPipelineDef = true;
    }
  }

  @Input()
  set node(node: DiagramMakerNode<StageWorkerDefinitionInfo>) {
    this.node$ = node;
  }

  @Input()
  set selected(selected: boolean) {
    this.selected$ = selected;
  }
}

