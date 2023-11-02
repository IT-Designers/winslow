import {
  Component,
  ComponentFactoryResolver,
  ElementRef,
  Input,
  ViewChild,
  ViewContainerRef,
  OnInit,
  AfterViewInit,
  OnChanges,
  OnDestroy
} from '@angular/core';
import {
  PipelineDefinitionInfo,
  ProjectInfo,
  StageDefinitionInfo,
  StageDefinitionInfoUnion,
} from "../../api/winslow-api";
import {
  Action,
  ConnectorPlacement,
  DiagramMaker,
  DiagramMakerActions,
  DiagramMakerConfig,
  DiagramMakerData,
  DiagramMakerNode,
  DiagramMakerPotentialNode, Dispatch, DragPanelAction, Layout, WorkflowLayoutDirection
} from "diagram-maker";
import {DiagramLibraryComponent} from "../../pipeline-view/diagram-library/diagram-library.component";
import {DiagramConfigHelper} from "../../pipeline-view/diagram-config-helper";
import {DiagramInitialData} from "../../pipeline-view/diagram-initial-data";
import {ControlDiagramInitialData} from "./control-diagram-initial-data";
import {DefaultApiServiceService} from "../../api/default-api-service.service";
import {DiagramNodeComponent} from "../../pipeline-view/diagram-node/diagram-node.component";
import {DiagramGatewayComponent} from "../../pipeline-view/diagram-gateway/diagram-gateway.component";
import {HttpClient} from "@angular/common/http";

@Component({
  selector: 'app-project-control-view-tab',
  templateUrl: './project-control-view-tab.component.html',
  styleUrls: ['./project-control-view-tab.component.css']
})
export class ProjectControlViewTabComponent implements OnInit, AfterViewInit, OnChanges, OnDestroy {

  @Input() project: ProjectInfo;
  //@Input() pipelineDefinition: PipelineDefinitionInfo;


  public diagramMaker!: DiagramMaker;
  public initialData!: DiagramMakerData<StageDefinitionInfo, {}>;
  public currentNode?: DiagramMakerNode<StageDefinitionInfo>;
  public componentFactory = this.componentFactoryResolver.resolveComponentFactory(DiagramLibraryComponent);
  public libraryComponent = null;
  public configClass = new DiagramConfigHelper();
  public initClass = new ControlDiagramInitialData();
  public defaultGetter = new DefaultApiServiceService(this.client);
  public saveStatus : boolean = true;


  @ViewChild('diagramEditorContainer')
  diagramEditorContainer!: ElementRef;

  config: DiagramMakerConfig<{}, {}> = {
    options: {
      connectorPlacement: ConnectorPlacement.LEFT_RIGHT,
      showArrowhead: true,
    },
    renderCallbacks: {
      node: (node: DiagramMakerNode<StageDefinitionInfo>, diagramMakerContainer: HTMLElement) => {
        diagramMakerContainer.innerHTML = '';
        let componentInstance = undefined;
        if (node.typeId == 'node-normal' || node.typeId == 'node-start') {
          const componentFactory = this.componentFactoryResolver.resolveComponentFactory(DiagramNodeComponent);
          componentInstance = this.viewContainerRef.createComponent(componentFactory);
        } else {
          const componentFactory = this.componentFactoryResolver.resolveComponentFactory(DiagramGatewayComponent);
          componentInstance = this.viewContainerRef.createComponent(componentFactory);
        }
        componentInstance.instance.node = node;
        diagramMakerContainer.appendChild(componentInstance.location.nativeElement);
        if (node.diagramMakerData.selected) {
          componentInstance.instance.selected = true;
          this.currentNode = node;
        }
      },
      potentialNode: (node: DiagramMakerPotentialNode, diagramMakerContainer: HTMLElement) => {
        diagramMakerContainer.innerHTML = '';
        const componentFactory = this.componentFactoryResolver.resolveComponentFactory(DiagramNodeComponent);
        const componentInstance = this.viewContainerRef.createComponent(componentFactory);
        diagramMakerContainer.appendChild(componentInstance.location.nativeElement);
      },
      destroy: () => {
        this.nodeComponentInstances.forEach(instance => instance.destroy());
        // TODO update nodeComponentInstances
      },
      panels: {
        library: (panel: any, state: any, diagramMakerContainer: HTMLElement) => {  //panel to edit Stages
          if (this.libraryComponent == null) {
            this.libraryComponent = this.viewContainerRef.createComponent(this.componentFactory);
            this.libraryComponent.instance.editNode.subscribe(editForm => { //edit Form is what is stored in Diagram Maker of the definition of the node
              this.editState(editForm);
            });
            this.libraryComponent.instance.resetSelectedNode.subscribe(() => this.currentNode = undefined);
            this.libraryComponent.instance.diagramApiCall.subscribe((event) => {   //these are calls made by the buttons in the node menu
              switch (event.action) {
                case 'fit':
                  this.diagramMaker.api.focusNode(Object.keys(this.diagramMaker.store.getState().nodes)[0]);
                  this.diagramMaker.store.dispatch({
                    type: 'WORKSPACE_DRAG',
                    payload: {
                      position:{
                        x: 0,
                        y: 400
                      }
                    }
                  });
                  break;
                case 'layout':
                  this.diagramMaker.api.layout({
                    direction: WorkflowLayoutDirection.LEFT_RIGHT,
                    distanceMin: 50,
                    layoutType: Layout.WORKFLOW,
                    fixedNodeId: this.diagramMaker.store.getState().nodes[Object.keys(this.diagramMaker.store.getState().nodes)[0]].id,
                  })
                  break;
                case 'zoomIn':
                  this.diagramMaker.api.zoomIn(100);
                  break;
                case 'zoomOut':
                  this.diagramMaker.api.zoomOut(100);
                  break;
                case 'undo':
                  this.diagramMaker.api.undo();
                  break;
                case 'redo':
                  this.diagramMaker.api.redo();
                  break;
                case 'save':
                  this.saveStatus = true;
                  console.log("---------------------------------- SAVE ----------------------------------");
                  break;
                default:
                  break;
              }
              // this.configClass.getApiSwitch(action, this.diagramMaker);   // is the same switch as above
            });
            diagramMakerContainer.appendChild(this.libraryComponent.location.nativeElement);
          }
          this.libraryComponent.instance.saveStatus = this.saveStatus;
          if (this.currentNode) {
            this.libraryComponent.instance.selectedNode = this.currentNode;
          }
        },
      },
    },
    actionInterceptor: (action: Action, dispatch: Dispatch<Action>) => {  //Intercepts actions before diagramMaker saves them into the store
      //console.log(action);
      if (action.type === DiagramMakerActions.PANEL_DRAG) {         //Interceptor to fix a bug where the panel clips at the top edge
        let dragAction : DragPanelAction = JSON.parse(JSON.stringify(action));
        dragAction.payload.viewContainerSize.height = 5000;
        dispatch(dragAction);
      }
      else {      //Default dispatch action for all actions that get not intercepted
        dispatch(action);
      }
    },
    nodeTypeConfig: this.configClass.getNodeTypes(),
  };

  nodeComponentInstances = [];

  constructor(private viewContainerRef: ViewContainerRef,
              private componentFactoryResolver: ComponentFactoryResolver,
              private client:  HttpClient,
  ) {
/*
    this.pipelineDefinitionEdit = this.pipelineDefinition;
*/
  }

  editState(editForm) { //used when saving the edits of a node, dispatching them ito the stor of diagrammaker with the custom Update_node action
    const currentState = this.diagramMaker.store.getState();
    let editNode = currentState.nodes[editForm.id];
    if (editNode) {
      let editData = JSON.parse(JSON.stringify(editNode.consumerData));
      //let i = this.pipelineDefinition.stages.map(function(stage) {
      //  return stage.name;
      //}).indexOf(`${editData.name}`);
      editData = editForm;
      editNode = Object.assign({}, editNode, {
        consumerData: editData,
        diagramMakerData: {
          selected: true,
          size: editNode.diagramMakerData.size,
          position: editNode.diagramMakerData.position,
        },
      });
      this.saveStatus = false;
      this.diagramMaker.store.dispatch({
        type: 'UPDATE_NODE',
        payload: editNode,
      });
    }
  }


  ngOnInit() {
    this.initialData = this.initClass.getInitData(this.project.pipelineDefinition);

  }


  ngOnChanges() {
    console.dir(this.project.pipelineDefinition);
  }


  ngAfterViewInit(): void {
    setTimeout(() => {
      this.diagramMaker = new DiagramMaker(
        this.diagramEditorContainer.nativeElement,
        this.config,
        {
          initialData: this.initialData,
          consumerRootReducer: (state: any, action: any) => {   //new action for diagramMaker to update the consumerData when editing a node
            switch (action.type) {
              case 'UPDATE_NODE':
                const newNode: any = {};
                newNode[action.payload.id] = action.payload;
                const newNodes = Object.assign({}, state.nodes, newNode);
                return Object.assign({}, state, {nodes: newNodes});
              default:
                return state;
            }
          }
        },
      );

      window.addEventListener('resize', () => {
        this.diagramMaker.updateContainer();
      });
      this.configClass.getApiSwitch('layout', this.diagramMaker);
    }, 500);

  }

  ngOnDestroy(): void {     //code to save the workflow (in the frontend) e.g. while switching tabs
    //console.dir('Diagram onDestroy');
    this.project.pipelineDefinition.stages = [];
    const nodeMap = new Map(Object.entries(this.diagramMaker.store.getState().nodes));
    const edgeMap = new Map(Object.entries(this.diagramMaker.store.getState().edges));
    let i = 0;
    for (let storeNode of nodeMap.values()){
      if (i > 0){
        let node = JSON.parse(JSON.stringify(storeNode))
        this.project.pipelineDefinition.stages.push(node.consumerData as StageDefinitionInfoUnion);
        this.project.pipelineDefinition.stages[i-1].nextStages = new Array();
      }
      i++;
    }
    for(let edge of edgeMap.values()){
      let index = this.project.pipelineDefinition.stages.findIndex(function(stage) {
        return stage.id == edge.src
      });
      if (index >= 0 ) {
        console.log(index);
        this.project.pipelineDefinition.stages[index].nextStages.push(edge.dest);
      }
    }
    if (this.diagramEditorContainer.nativeElement != null) {    //diagrammaker unload/destroy
      this.diagramMaker.destroy();
      console.log("destroyed")
    }
    this.nodeComponentInstances.forEach(instance => instance.destroy());
  }
}
