import {Component, Input, OnInit, Output, EventEmitter, ViewChild} from '@angular/core';
import {DiagramMakerNode} from "diagram-maker";
import {
  PipelineDefinitionInfo,
  StageAndGatewayDefinitionInfo,
  StageDefinitionInfo,
  StageDefinitionInfoUnion, StageWorkerDefinitionInfo, StageXOrGatewayDefinitionInfo,
} from "../../api/winslow-api";
import {EditFormsComponent} from "./edit-forms/edit-forms.component";

@Component({
  selector: 'app-diagram-library',
  templateUrl: './diagram-library.component.html',
  styleUrls: ['./diagram-library.component.css'],
})
export class DiagramLibraryComponent implements OnInit {

  @Output() resetSelectedNode = new EventEmitter();
  @Output() editNode = new EventEmitter();
  @Output() diagramApiCall = new EventEmitter();
  selectedNode$?: DiagramMakerNode<StageDefinitionInfoUnion>;
  selectedNodeData: PipelineDefinitionInfo   | StageWorkerDefinitionInfo | StageAndGatewayDefinitionInfo | StageXOrGatewayDefinitionInfo | undefined = undefined;
  savedData: boolean = true;
  selectedNodeDataEdit: PipelineDefinitionInfo   | StageWorkerDefinitionInfo | StageAndGatewayDefinitionInfo | StageXOrGatewayDefinitionInfo | undefined = undefined;
  formObj: Object = {};
  pipelineDef: PipelineDefinitionInfo | undefined;

  @ViewChild('form') childForm?: EditFormsComponent;

  constructor() {
  }

  ngOnInit(): void {
    //console.log("Board Init")
  }

  @Input()
  set selectedNode(selectedNode: DiagramMakerNode<StageDefinitionInfo>) {
    this.selectedNode$ = selectedNode as DiagramMakerNode<StageDefinitionInfoUnion>;
    this.selectedNodeData = this.selectedNode$.consumerData;
    this.selectedNodeDataEdit = this.selectedNode$.consumerData;
    this.formObj = {} as StageDefinitionInfoUnion;
    this.formObj = JSON.parse(JSON.stringify(this.selectedNode$.consumerData));
  }

  @Input()
  set saveStatus(saveStatus: boolean) {
    this.savedData = saveStatus;
  };

  onApiCall(action: String) {       //used when clicking on the function icons e.g. save, undo...
    switch (action) {
      case 'save':
        this.savedData = true;
        break;
    }
    this.diagramApiCall.emit({action: action, node: this.selectedNode$});
  }

  startSave() {          //starts the save on top level of the recursion of edit-forms
    this.childForm?.sendFormData();
  }

  saveEdit(savedForm: Object) {   //receives the chaneged data from the edit-forms and saves it in the board and in the node
    if (1 in savedForm) {
      this.formObj = savedForm[1] as StageDefinitionInfoUnion; //todo unsafe
    }
    this.editNode.emit(this.formObj);
  }

  emitSave(editedNodeData: StageWorkerDefinitionInfo | PipelineDefinitionInfo | StageAndGatewayDefinitionInfo | StageXOrGatewayDefinitionInfo | undefined) {
    this.selectedNodeDataEdit = editedNodeData;
    this.editNode.emit(editedNodeData);
  }

  //These next 4 functions make the typing of selectedNodeData definitive and never undefined
  //If object is of the wrong type it is empty
  //Used to display different settings depending on the type of the selected node
  isPipelineDefinition(selectedNodeData: PipelineDefinitionInfo   | StageWorkerDefinitionInfo | StageAndGatewayDefinitionInfo | StageXOrGatewayDefinitionInfo | undefined): PipelineDefinitionInfo {
    if (selectedNodeData instanceof PipelineDefinitionInfo) {
      return selectedNodeData;
    } else {
      return {} as PipelineDefinitionInfo;
    }
  }

  isStageWorker(selectedNodeData: PipelineDefinitionInfo   | StageWorkerDefinitionInfo | StageAndGatewayDefinitionInfo | StageXOrGatewayDefinitionInfo | undefined): StageWorkerDefinitionInfo {
    if (selectedNodeData instanceof StageWorkerDefinitionInfo) {
      return selectedNodeData;
    } else {
      return {} as StageWorkerDefinitionInfo;
    }
  }

  isAndGateway(selectedNodeData: PipelineDefinitionInfo   | StageWorkerDefinitionInfo | StageAndGatewayDefinitionInfo | StageXOrGatewayDefinitionInfo | undefined): StageAndGatewayDefinitionInfo {
    if (selectedNodeData instanceof StageAndGatewayDefinitionInfo) {
      return selectedNodeData;
    } else {
      return {} as StageAndGatewayDefinitionInfo;
    }
  }

  isXOrGateway(selectedNodeData: PipelineDefinitionInfo   | StageWorkerDefinitionInfo | StageAndGatewayDefinitionInfo | StageXOrGatewayDefinitionInfo | undefined): StageXOrGatewayDefinitionInfo {
    if (selectedNodeData instanceof StageXOrGatewayDefinitionInfo) {
      return selectedNodeData;
    } else {
      return {} as StageXOrGatewayDefinitionInfo;
    }
  }

  cancelEdit() {          //unloads the edit-node when clicking on the X-Icon
    this.selectedNode$ = undefined;
    this.selectedNodeData = undefined;
    this.selectedNodeDataEdit = undefined;
    this.resetSelectedNode.emit();
  }


  protected readonly undefined = undefined;
}
