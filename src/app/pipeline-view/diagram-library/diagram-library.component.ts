import {Component, Input, OnInit, Output, EventEmitter} from '@angular/core';
import {DiagramMakerNode} from "diagram-maker";
import {StageDefinitionInfo} from "../../api/project-api.service";


@Component({
  selector: 'app-diagram-library',
  templateUrl: './diagram-library.component.html',
  styleUrls: ['./diagram-library.component.css']
})
export class DiagramLibraryComponent implements OnInit {

  selectedNode$?: DiagramMakerNode<StageDefinitionInfo>;

  constructor() {
  }

  ngOnInit(): void {
  }

  cancelEdit(){
    this.selectedNode$ = undefined;
    this.resetSelectedNode.emit();
  }

  @Input()
  set selectedNode(selectedNode: DiagramMakerNode<StageDefinitionInfo>) {
    this.selectedNode$ = selectedNode;
  }
  @Output() resetSelectedNode = new EventEmitter();


}
