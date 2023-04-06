import {
  AfterViewInit,
  Component,
  ComponentFactoryResolver,
  ElementRef, EventEmitter,
  Input,
  OnDestroy,
  OnInit, Output,
  ViewChild,
  ViewContainerRef
} from '@angular/core';
import {
  Action,
  ConnectorPlacement,
  CreateEdgeAction,
  CreateNodeAction,
  DeleteItemsAction,
  DiagramMaker,
  DiagramMakerActions,
  DiagramMakerConfig,
  DiagramMakerData,
  DiagramMakerNode,
  DiagramMakerPotentialNode,
  Dispatch, DragPanelAction,
} from 'diagram-maker';


import {DiagramNodeComponent} from './diagram-node/diagram-node.component';
import {DiagramLibraryComponent} from './diagram-library/diagram-library.component';
import {DiagramConfigHelper} from './diagram-config-helper';
import {DiagramInitialData} from './diagram-initial-data';
import {AddToolsComponent} from './add-tools/add-tools.component';
import {DiagramGatewayComponent} from './diagram-gateway/diagram-gateway.component';
import {
  PipelineDefinitionInfo,
  StageAndGatewayDefinitionInfo,
  StageDefinitionInfo,
  StageDefinitionInfoUnion, StageWorkerDefinitionInfo, StageXOrGatewayDefinitionInfo,
} from '../api/winslow-api';
import {DefaultApiServiceService} from "../api/default-api-service.service";
import {HttpClient} from "@angular/common/http";

@Component({
  selector: 'app-pipeline-view',
  templateUrl: './pipeline-view.component.html',
  styleUrls: ['./pipeline-view.component.css']
})
export class PipelineViewComponent implements OnInit, AfterViewInit, OnDestroy {

  @Input() public pipelineDefinition: PipelineDefinitionInfo;
  @Output() public onSave = new EventEmitter<PipelineDefinitionInfo>();

  public diagramMaker!: DiagramMaker;
  public initialData!: DiagramMakerData<StageDefinitionInfo, {}>;
  public currentNode?: DiagramMakerNode<StageDefinitionInfo>;
  public componentFactory = this.componentFactoryResolver.resolveComponentFactory(DiagramLibraryComponent);
  public libraryComponent = null;
  public configClass = new DiagramConfigHelper();
  public initClass = new DiagramInitialData();
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
            this.libraryComponent.instance.editNode.subscribe(editForm => this.editState(editForm));
            this.libraryComponent.instance.resetSelectedNode.subscribe(() => this.currentNode = undefined);
            this.libraryComponent.instance.diagramApiCall.subscribe((action: String) => {
              if (action == "save"){this.saveStatus = true; this.onSave.emit(this.pipelineDefinition);}
              this.configClass.getApiSwitch(action, this.diagramMaker)
            });
            diagramMakerContainer.appendChild(this.libraryComponent.location.nativeElement);
          }
          this.libraryComponent.instance.saveStatus = this.saveStatus;
          if (this.currentNode) {
            this.libraryComponent.instance.selectedNode = this.currentNode;
          }
        },
        tools: (panel: any, state: any, diagramMakerContainer: HTMLElement) => {    //Bar to Add Nodes to diagramMaker
          let addToolsFactory = this.componentFactoryResolver.resolveComponentFactory(AddToolsComponent);
          let addToolsComponent = this.viewContainerRef.createComponent(addToolsFactory);
          diagramMakerContainer.appendChild(addToolsComponent.location.nativeElement);
        }
      },
    },
    actionInterceptor: (action: Action, dispatch: Dispatch<Action>) => {  //Intercepts actions before diagramMaker saves them into the store
      //console.log(action);
      if (action.type === DiagramMakerActions.NODE_CREATE) {      //required extra steps to append Data to the nodes before dispatching
        const createAction = action as CreateNodeAction<any>;
        if (createAction.payload.typeId == 'node-normal' || createAction.payload.typeId == 'node-start') {
          let stageData : StageWorkerDefinitionInfo;
          this.defaultGetter.getWorkerDefinition().then((data) => {   //get default Stage/Gatewaydata from the api
            stageData = data;
            dispatch(this.createElement(stageData, createAction));
            return;
          }, function (err){
              console.error("No element created: Error while getting the data for the new element. " + err);
          });

        }
        else if (createAction.payload.typeId == 'node-and-splitter') {
            let stageData : StageAndGatewayDefinitionInfo;
            this.defaultGetter.getAndSplitterDefinition().then((data) =>{
              stageData = data;
              dispatch(this.createElement(stageData, createAction));
              return;
            }, function (err){
              console.log("No element created: Error while getting the data for the new element. " + err);
            });
        }
        else if(createAction.payload.typeId == 'node-if-splitter'){
          let stageData : StageXOrGatewayDefinitionInfo;
          this.defaultGetter.getIfSplitterDefinition().then((data) =>{
            stageData = data;
            dispatch(this.createElement(stageData, createAction));
            return;
          }, function (err){
            console.log("No element created: Error while getting the data for the new element. " + err);
          });
        }
        else if(createAction.payload.typeId == 'node-all-merger'){
          let stageData : StageAndGatewayDefinitionInfo;
          this.defaultGetter.getAllMergerDefinition().then((data) =>{
            stageData = data;
            dispatch(this.createElement(stageData, createAction));
            return;
          }, function(err){
            console.log("No element created: Error while getting the data for the new element. " + err);
          });
        }
        else if(createAction.payload.typeId == 'node-any-merger'){
          let stageData : StageXOrGatewayDefinitionInfo;
          this.defaultGetter.getAnyMergerDefinition().then((data) => {
            stageData = data;
            dispatch(this.createElement(stageData, createAction));
            return;
          }, function(err){
            console.log("No element created: Error while getting the data for the new element. " + err);
          });
        }
      } else if (action.type === DiagramMakerActions.EDGE_CREATE) {   //Logic if a Edge can be created or not
        let edgeDestPossible = true;
        let edgeSrcPossible = true;
        const edgeMap = new Map(Object.entries(this.diagramMaker.store.getState().edges));
        const nodeMap = this.diagramMaker.store.getState().nodes;
        let createEdgeAction = action as CreateEdgeAction<{}>;
        //Check if edge can be created depending on destination node
        for (let edge of edgeMap.values()) {                //For Stages and Splitter Gateways
          if (edge.dest == createEdgeAction.payload.dest) {
            edgeDestPossible = false;
            break;
          } else {
            edgeDestPossible = true;
          }
        }
        //for Mergers
        if (nodeMap[createEdgeAction.payload.dest].typeId == 'node-any-merger' ||
          nodeMap[createEdgeAction.payload.dest].typeId == 'node-all-merger'
        ) {
          edgeDestPossible = true;
        }
        //Check if edge can be created depending on source Node
        for (let edge of edgeMap.values()) {                //For Stages and Merger Gateways
          if (edge.src == createEdgeAction.payload.src) {
            edgeSrcPossible = false;
            break;
          } else {
            edgeSrcPossible = true;
          }
        } //For Splitters
        if (nodeMap[createEdgeAction.payload.src].typeId == 'node-and-splitter' ||
          nodeMap[createEdgeAction.payload.src].typeId == 'node-if-splitter'
        ) {
          edgeSrcPossible = true;
        }
        if (edgeDestPossible && edgeSrcPossible) {
          this.saveStatus = false;
          dispatch(createEdgeAction);
        }
      } else if (action.type === DiagramMakerActions.DELETE_ITEMS) {
        let deleteAction = action as DeleteItemsAction;
        const nodeMap = new Map(Object.entries(this.diagramMaker.store.getState().nodes));
        const keyIter = nodeMap.keys()
        console.log(deleteAction.payload.nodeIds);
        if (deleteAction.payload.nodeIds.includes(keyIter.next().value)){}
        else {
          this.saveStatus = false;
          dispatch(deleteAction);
        }
      }  else if (action.type === DiagramMakerActions.PANEL_DRAG) {         //Interceptor to fix a bug where the panel clips at the top edge
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
              private client:  HttpClient
  ) {
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

  createElement(stageData, createAction){ //helper function used for the intercepted createNode Actions
    let nodeWidth : number;
    if (createAction.payload.typeId == 'node-normal'){nodeWidth = 200}
    else {nodeWidth = 150}
    let newAction: CreateNodeAction<{}> = {
      type: DiagramMakerActions.NODE_CREATE,
      payload: {
        id: `${stageData.id}`,
        typeId: `${createAction.payload.typeId}`,
        position: {x: createAction.payload.position.x, y: createAction.payload.position.y},
        size: {width: nodeWidth, height: 75},
        consumerData: stageData,
      }
    };
    this.saveStatus = false;
    return newAction;
  }

  ngOnInit() {
    this.initialData = this.initClass.getInitData(this.pipelineDefinition);
    console.log(this.pipelineDefinition.stages)

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
    }, 1000);

  }

  ngOnDestroy(): void {     //code to save the workflow (in the frontend) e.g. while switching tabs
    this.pipelineDefinition.stages = [];
    const nodeMap = new Map(Object.entries(this.diagramMaker.store.getState().nodes));
    const edgeMap = new Map(Object.entries(this.diagramMaker.store.getState().edges));
    let i = 0;
    for (let storeNode of nodeMap.values()){
      if (i > 0){
        let node = JSON.parse(JSON.stringify(storeNode))
        this.pipelineDefinition.stages.push(node.consumerData as StageDefinitionInfoUnion);
        this.pipelineDefinition.stages[i-1].nextStages = new Array();
      }
      i++;
    }
    for(let edge of edgeMap.values()){
      let index = this.pipelineDefinition.stages.findIndex(function(stage) {
        return stage.id == edge.src
      });
      if (index >= 0 ) {
        console.log(index);
        this.pipelineDefinition.stages[index].nextStages.push(edge.dest);
      }
    }
    if (this.diagramEditorContainer.nativeElement != null) {    //diagrammaker unload/destroy
      this.diagramMaker.destroy();
      console.log("destroyed")
    }
    this.nodeComponentInstances.forEach(instance => instance.destroy());
  }


}
