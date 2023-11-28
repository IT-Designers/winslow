import {Component, EventEmitter, Input, OnInit, Output, SimpleChanges, ViewChild} from '@angular/core';
import {DiagramMakerNode} from "diagram-maker";
import {
  ImageInfo,
  PipelineDefinitionInfo, ProjectInfo,
  Raw, ResourceInfo,
  StageDefinitionInfo,
  StageDefinitionInfoUnion,
  StageWorkerDefinitionInfo, WorkspaceConfiguration
} from "../../../api/winslow-api";
import {EditFormsComponent} from "../../../pipeline-view/diagram-library/edit-forms/edit-forms.component";
import {DialogService} from "../../../dialog.service";
import {ProjectApiService} from "../../../api/project-api.service";

@Component({
  selector: 'app-control-view-library',
  templateUrl: './control-view-library.component.html',
  styleUrls: ['./control-view-library.component.css']
})
export class ControlViewLibraryComponent implements OnInit {
  @Output() resetSelectedNode = new EventEmitter();
  @Output() editNode = new EventEmitter();
  @Output() diagramApiCall = new EventEmitter();
  selectedNode$?: DiagramMakerNode<StageDefinitionInfo | StageWorkerDefinitionInfo>;
  savedData : boolean = true;
  pipelineDef: PipelineDefinitionInfo | undefined;
  projectDef: ProjectInfo | undefined = undefined;
  formObj : Object = {};

  @ViewChild('form') childForm?: EditFormsComponent;

  constructor(
    private dialog: DialogService,
    private projectApi: ProjectApiService
  ) {
  }
  ngOnInit(): void {
    //console.log("Board Init")
  }

  isNodeStageWorker(node: StageWorkerDefinitionInfo | StageDefinitionInfo | undefined): StageWorkerDefinitionInfo | undefined {
    if (node instanceof StageWorkerDefinitionInfo) {
      return node;
    }
  };

  getPipelineDefWithCorrectType(pipeDef: PipelineDefinitionInfo | undefined): PipelineDefinitionInfo[] {
    if (pipeDef != undefined) {
      return [pipeDef];
    } else {
      return [];
    }
  }

  getPipelineIdAsString(pipeDef: PipelineDefinitionInfo | undefined): string {
    if (pipeDef?.id) {
      return pipeDef.id;
    } else {
      return '';
    }
  }

  @Input()
  set selectedNode(selectedNode: DiagramMakerNode<StageDefinitionInfo | StageWorkerDefinitionInfo>) {
    this.selectedNode$ = selectedNode;
    this.formObj = {} as StageDefinitionInfoUnion;
    this.formObj = JSON.parse(JSON.stringify(this.selectedNode$.consumerData));
  }
  @Input()
  set saveStatus(saveStatus : boolean){
    this.savedData = saveStatus;
  };
  @Input()
  set pipelineDefinition(pipelineDefinition: PipelineDefinitionInfo) {
    this.pipelineDef = pipelineDefinition;
  }
  @Input() set project(project: ProjectInfo) {
    this._project = project;
    // todo make stage execution selection able to react to pipeline changes on its own (or replace it entirely)
    // trigger rerender of stage execution selection
    this.pipelineDefinition = project.pipelineDefinition;
  };

  get project() {
    return this._project;
  }

  private _project!: ProjectInfo;

  onApiCall(action : String){       //used when clicking on the function icons e.g. save, undo...
    switch (action) {
      case 'save':
        this.savedData = true;
        break;
    }
    this.diagramApiCall.emit({action: action, node: this.selectedNode$});
  }


  enqueue(
    pipeline: PipelineDefinitionInfo | undefined,
    stageDefinitionInfo: StageDefinitionInfo | StageWorkerDefinitionInfo,
    env: any,
    rangedEnv: any,
    image: ImageInfo,
    requiredResources: ResourceInfo,
    workspaceConfiguration: WorkspaceConfiguration,
    comment: string,
    runSingle: boolean,
    resume: boolean,
  ) {
    if (pipeline == undefined) {
      this.dialog.error('No pipeline selected!');
      return
    }
    if (pipeline.name !== this.project.pipelineDefinition.name) {
      this.dialog.error('Changing the Pipeline is not yet supported!');
      return
    }
    this.dialog.openLoadingIndicator(
      this.projectApi.enqueue(
        this.project.id,
        stageDefinitionInfo.id,
        env,
        rangedEnv,
        image,
        requiredResources,
        workspaceConfiguration,
        comment,
        runSingle,
        resume),
      `Submitting selections`
    );
  }






  startSave(){          //starts the save on top level of the recursion of edit-forms
    this.childForm?.sendFormData();
  }
  saveEdit(savedForm : Object){   //receives the chaneged data from the edit-forms and saves it in the board and in the node
    if (1 in savedForm) {
      this.formObj = savedForm[1] as Raw<StageDefinitionInfoUnion>;
    }
    this.editNode.emit(this.formObj);
  }
  cancelEdit() {          //unloads the edit-node when clicking on the X-Icon
    this.selectedNode$ = undefined;
    this.resetSelectedNode.emit();
  }

    protected readonly PipelineDefinitionInfo = PipelineDefinitionInfo;
}
