import {Component, Input, OnInit} from '@angular/core';
import {DiagramMakerNode} from "diagram-maker";
import {StageDefinitionInfo} from "../../api/project-api.service";

@Component({
  selector: 'app-diagram-node',
  templateUrl: './diagram-node.component.html',
  styleUrls: ['./diagram-node.component.css']
})
export class DiagramNodeComponent implements OnInit {

  @Input() nodeTypeName : String;
  node$?: DiagramMakerNode<StageDefinitionInfo>;
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
    ){
      this.splitter = true;
    }
    if (this.nodeTypeName == "ALL Merger" || this.nodeTypeName == "ANY Merger"||
      this.node$?.typeId == "node-all-merger" || this.node$?.typeId == "node-any-merger"
    ){
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

