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

  constructor() {
  }

  ngOnInit(): void {
    if (this.node$ !== undefined) {
      this.containsNode = true;
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

