import {Component, Input, OnInit, Output, EventEmitter, ViewChild} from '@angular/core';
import {DiagramMakerNode} from "diagram-maker";
import {
  PipelineDefinitionInfo,
  StageDefinitionInfo,
  StageDefinitionInfoUnion,
  StageWorkerDefinitionInfo
} from "../../api/winslow-api";

@Component({
  selector: 'app-diagram-library',
  templateUrl: './diagram-library.component.html',
  styleUrls: ['./diagram-library.component.css'],
})
export class DiagramLibraryComponent implements OnInit {

  @Output() resetSelectedNode = new EventEmitter();
  @Output() editNode = new EventEmitter();
  @Output() diagramApiCall = new EventEmitter();
  selectedNode$?: DiagramMakerNode<StageDefinitionInfo>;
  savedData : boolean = true;
  formObj : Object = {};

  @ViewChild('form') childForm;

  constructor() {
  }
  ngOnInit(): void {
    //console.log("Board Init")
  }
  @Input()
  set selectedNode(selectedNode: DiagramMakerNode<StageDefinitionInfo>) {
      this.selectedNode$ = selectedNode;
      this.formObj = {} as StageDefinitionInfoUnion;
      this.formObj = JSON.parse(JSON.stringify(this.selectedNode$.consumerData));
  }
  @Input()
  set saveStatus(saveStatus : boolean){
    this.savedData = saveStatus;
  };
  onApiCall(action : String){       //used when clicking on the function icons e.g. save, undo...
    switch (action) {
      case 'save':
        console.log('Save pressed');
        this.savedData = true;

        break;
      case 'fit':
        console.log('fit pressed');
        break;
      case 'layout':
        console.log('layout pressed');
        break;
    }
    /*if (action == "save"){this.savedData = true;}*/
    this.diagramApiCall.emit(action);
  }
  startSave(){          //starts the save on top level of the recursion of edit-forms
    this.childForm.sendFormData();
  }
  saveEdit(savedForm : Object){   //receives the chaneged data from the edit-forms and saves it in the board and in the node
    this.formObj = savedForm[1] as StageDefinitionInfoUnion;
    this.editNode.emit(this.formObj);
  }
  cancelEdit() {          //unloads the edit-node when clicking on the X-Icon
    this.selectedNode$ = undefined;
    this.resetSelectedNode.emit();
  }


}
