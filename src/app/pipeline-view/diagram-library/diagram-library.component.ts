import {Component, Input, OnInit, Output, EventEmitter, ViewChild} from '@angular/core';
import {DiagramMakerNode} from "diagram-maker";
import {StageDefinitionInfo} from "../../api/winslow-api";

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
  formHtmlMap : Map<string, object> = new Map();
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
      this.formHtmlMap = new Map();
      for (const key of Object.keys(this.selectedNode$.consumerData)) {
        this.formHtmlMap.set(key, this.selectedNode$.consumerData[key]);
      }
      //console.log(this.formHtmlMap);
      this.formObj = {};
      this.formObj['id'] = this.selectedNode$.id;
      for (const key of Object.keys(this.selectedNode$.consumerData)) {
        this.formObj[key] = this.selectedNode$.consumerData[key];
      }
      //console.log(this.formObj);
      //console.log(this.editForm.value);
  }
  @Input()
  set saveStatus(saveStatus : boolean){
    this.savedData = saveStatus;
  };
  onApiCall(action : String){
    if (action == "save"){this.savedData = true;}
    this.diagramApiCall.emit(action);
  }
  startSave(){
    this.childForm.sendFormData();
  }
  saveEdit(savedForm : Object){
    this.formObj = savedForm;
    this.editNode.emit(savedForm[1]);
  }
  cancelEdit() {
    this.selectedNode$ = undefined;
    this.resetSelectedNode.emit();
  }


}
