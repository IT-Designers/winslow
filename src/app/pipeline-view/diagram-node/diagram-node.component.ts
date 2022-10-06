import {Component, Input, OnInit} from '@angular/core';
import {DiagramMakerNode} from "diagram-maker";
import {StageDefinitionInfo} from "../../api/project-api.service";
import {MatCardModule} from '@angular/material/card';

@Component({
  selector: 'app-diagram-node',
  templateUrl: './diagram-node.component.html',
  styleUrls: ['./diagram-node.component.css']
})
export class DiagramNodeComponent implements OnInit {

  node$?: DiagramMakerNode<StageDefinitionInfo>;
  selected$?: boolean = false;

  constructor() {
  }

  ngOnInit(): void {
  }

  @Input()
  set node(node: DiagramMakerNode<StageDefinitionInfo>) {
    this.node$ = node;
  }
  @Input()
  set selected(selected : boolean) {
    this.selected$ = selected;
  }
}

