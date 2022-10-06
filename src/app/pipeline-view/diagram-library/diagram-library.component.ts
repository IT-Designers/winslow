import {Component, Input, OnInit} from '@angular/core';
import {MatExpansionModule} from '@angular/material/expansion';
import {DiagramMakerNode} from "diagram-maker";
import {StageDefinitionInfo} from "../../api/project-api.service";

@Component({
  selector: 'app-diagram-library',
  templateUrl: './diagram-library.component.html',
  styleUrls: ['./diagram-library.component.css']
})
export class DiagramLibraryComponent implements OnInit {

  selectedNode$? : DiagramMakerNode<StageDefinitionInfo>;

  constructor() { }

  ngOnInit(): void {
  }
  @Input()
  set selectedNode(selectedNode: DiagramMakerNode<StageDefinitionInfo>) {
    this.selectedNode$ = selectedNode;
  }

}
