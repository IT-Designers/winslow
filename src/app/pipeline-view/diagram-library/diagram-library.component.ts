import {Component, Input, OnInit, Output, EventEmitter, ViewChild} from '@angular/core';
import {DiagramMakerNode} from "diagram-maker";
import {
  StageDefinitionInfo,
  StageDefinitionInfoUnion,
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
        this.savedData = true;
        break;
    }
    this.diagramApiCall.emit({action: action, node: this.selectedNode$});
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
