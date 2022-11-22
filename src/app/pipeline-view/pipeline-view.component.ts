import {
  AfterViewInit,
  Component,
  ComponentFactoryResolver,
  ElementRef,
  Input,
  OnDestroy,
  OnInit,
  ViewChild,
  ViewContainerRef
} from '@angular/core';
import {
  Action,
  ConnectorPlacement, CreateEdgeAction,
  CreateNodeAction,
  DiagramMaker,
  DiagramMakerActions,
  DiagramMakerConfig,
  DiagramMakerData,
  DiagramMakerEdge,
  DiagramMakerNode,
  DiagramMakerPotentialNode,
  Dispatch,
  EditorMode, Layout, PositionAnchor, VisibleConnectorTypes, WorkflowLayoutDirection,
} from 'diagram-maker';

import {ImageInfo, ProjectInfo, StageDefinitionInfo} from "../api/project-api.service";
import {DiagramNodeComponent} from "./diagram-node/diagram-node.component";
import {DiagramLibraryComponent} from "./diagram-library/diagram-library.component";
import {DiagramConfig} from "./diagram-config";
import {DiagramInitialData} from "./diagram-initial-data";
import {AddToolsComponent} from "./add-tools/add-tools.component";
import {DiagramGatewayComponent} from "./diagram-gateway/diagram-gateway.component";

@Component({
  selector: 'app-pipeline-view',
  templateUrl: './pipeline-view.component.html',
  styleUrls: ['./pipeline-view.component.css']
})
export class PipelineViewComponent implements OnInit, AfterViewInit, OnDestroy {

  @Input() public project: ProjectInfo;

  public diagramMaker!: DiagramMaker;
  public initialData!: DiagramMakerData<StageDefinitionInfo, {}>;
  public currentNode?: DiagramMakerNode<StageDefinitionInfo>;
  public componentFactory = this.componentFactoryResolver.resolveComponentFactory(DiagramLibraryComponent);
  public libraryComponent = null;
  public initClass = new DiagramInitialData();

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
        if (node.typeId == "node-normal" || node.typeId == "node-start"){
        const componentFactory = this.componentFactoryResolver.resolveComponentFactory(DiagramNodeComponent);
         componentInstance = this.viewContainerRef.createComponent(componentFactory);
        }
        else {
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
        //setTimeout(() => { componentInstance.destroy(); }, 1000);
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
            this.libraryComponent.instance.diagramApiCall.subscribe((action : String)=> {
              console.log(action);
              switch (action){
                case 'fit':
                  this.diagramMaker.api.fit();
                  console.log("fit");
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
                default: break;
              }
            }
            );
            diagramMakerContainer.appendChild(this.libraryComponent.location.nativeElement);
          }
          if (this.currentNode) {
            this.libraryComponent.instance.selectedNode = this.currentNode;
          }
        },
        tools: (panel: any, state : any, diagramMakerContainer: HTMLElement ) => {    //Bar to Add Nodes to diagramMaker
          let addToolsFactory = this.componentFactoryResolver.resolveComponentFactory(AddToolsComponent);
          let addToolsComponent = this.viewContainerRef.createComponent(addToolsFactory);
          diagramMakerContainer.appendChild(addToolsComponent.location.nativeElement);
        }
      },
    },
    actionInterceptor: (action: Action, dispatch: Dispatch<Action>) => {  //Intercepts actions before diagramMaker saves them into the store
      if (action.type === DiagramMakerActions.NODE_CREATE) {      //required extra steps to append Data to the nodes before dispatching
        const createAction = action as CreateNodeAction<any>;
        if (createAction.payload.typeId == "node-normal" || createAction.payload.typeId ==  "node-start"){

          const stageDef = new StageDefinitionInfo();
          stageDef.name = "New Stage";
          stageDef.image = new ImageInfo();
          stageDef.requiredEnvVariables = [];
          stageDef.requiredResources = null;
          stageDef.env = new Map;
          //console.log(stageDef);
          this.project.pipelineDefinition.stages.push(stageDef)
          let newAction: CreateNodeAction<{}> = {
            type: DiagramMakerActions.NODE_CREATE,
            payload: {
              id: `n${createAction.payload.id}`,
              typeId: `${createAction.payload.typeId}`,
              position: {x: createAction.payload.position.x, y: createAction.payload.position.y},
              size: {width: 200, height: 75},
              consumerData: stageDef
            }
          }
          dispatch(newAction);
        }
        if (createAction.payload.typeId == "node-and-splitter" ||
          createAction.payload.typeId == "node-if-splitter" ||
          createAction.payload.typeId == "node-all-merger" ||
          createAction.payload.typeId == "node-any-merger"
        ){
          const stageDef = new StageDefinitionInfo();
          stageDef.name = `${createAction.payload.typeId}`;
          let newAction: CreateNodeAction<{}> = {
            type: DiagramMakerActions.NODE_CREATE,
            payload: {
              id: `n${createAction.payload.id}`,
              typeId: `${createAction.payload.typeId}`,
              position: {x: createAction.payload.position.x, y: createAction.payload.position.y},
              size: {width: 150, height: 75},
              consumerData: stageDef,
            }
          }
          dispatch(newAction);
        }
      }
      else if (action.type === DiagramMakerActions.EDGE_CREATE) {   //Logic if a Edge can be created or not
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
        if (nodeMap[createEdgeAction.payload.dest].typeId == "node-any-merger" ||
          nodeMap[createEdgeAction.payload.dest].typeId == "node-all-merger"
        ){
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
        if (nodeMap[createEdgeAction.payload.src].typeId == "node-and-splitter" ||
          nodeMap[createEdgeAction.payload.src].typeId == "node-if-splitter"
        ){
          edgeSrcPossible = true;
        }
        if (edgeDestPossible && edgeSrcPossible) {
          dispatch(createEdgeAction);
        }
      }
      else {      //Default dispatch action for all actions that get not intercepted
        dispatch(action);
      }
    },
    nodeTypeConfig: {
      'node-normal': {
        size: {width: 200, height: 75},
        connectorPlacementOverride: ConnectorPlacement.LEFT_RIGHT,
      },
      'node-start': {
        size: {width: 200, height: 75},
        connectorPlacementOverride: ConnectorPlacement.LEFT_RIGHT,
        visibleConnectorTypes: VisibleConnectorTypes.OUTPUT_ONLY,
      },
      'node-if-splitter': {
        size: {width: 150, height: 75},
        connectorPlacementOverride: ConnectorPlacement.LEFT_RIGHT,
      },
      'node-and-splitter': {
        size: {width: 150, height: 75},
        connectorPlacementOverride: ConnectorPlacement.LEFT_RIGHT,
      },
      'node-all-merger': {
        size: {width: 150, height: 75},
        connectorPlacementOverride: ConnectorPlacement.LEFT_RIGHT,
      },
      'node-any-merger': {
        size: {width: 150, height: 75},
        connectorPlacementOverride: ConnectorPlacement.LEFT_RIGHT,
      },
    }
  };

  nodeComponentInstances = [];

  constructor(private viewContainerRef: ViewContainerRef,
              private componentFactoryResolver: ComponentFactoryResolver) {
  }

  editState(editForm) {
    //console.log(editForm);
    const currentState = this.diagramMaker.store.getState();
    let editNode = currentState.nodes[editForm.id];
    if (editNode) {
      let editData = JSON.parse(JSON.stringify(editNode.consumerData));
      let i = this.project.pipelineDefinition.stages.map(function (stage) {
        return stage.name;
      }).indexOf(`${editData.name}`);
      editData = editForm;
      delete editData.id;
      editNode = Object.assign({}, editNode, {
        consumerData: editData,
        diagramMakerData: {
          selected: true,
          size: editNode.diagramMakerData.size,
          position: editNode.diagramMakerData.position,
        },
      });
      this.diagramMaker.store.dispatch({
        type: 'UPDATE_NODE',
        payload: editNode,
      });
      this.project.pipelineDefinition.stages[i] = editData;
    }
  }

  ngOnInit() {
    const stageDef = this.project.pipelineDefinition.stages[1] as StageDefinitionInfo;
    //console.log(stageDef.env instanceof Map);
    //console.log(this.project.pipelineDefinition.stages[1].env instanceof Map);
    this.initialData = this.initClass.getInitData(this.project);

  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.diagramMaker = new DiagramMaker(
        this.diagramEditorContainer.nativeElement,
        this.config,
        {
          initialData: this.initialData,
          consumerRootReducer: (state: any, action: any) => {
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
    }, 1000);

  }

  ngOnDestroy(): void {
    if (this.diagramEditorContainer.nativeElement != null) {
      this.diagramMaker.destroy();
    }
    this.nodeComponentInstances.forEach(instance => instance.destroy());
  }


}
