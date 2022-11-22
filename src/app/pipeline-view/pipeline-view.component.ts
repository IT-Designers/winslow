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
  EditorMode, PositionAnchor, VisibleConnectorTypes,
} from 'diagram-maker';

import {ImageInfo, ProjectInfo, StageDefinitionInfo} from "../api/project-api.service";
import {DiagramNodeComponent} from "./diagram-node/diagram-node.component";
import {DiagramLibraryComponent} from "./diagram-library/diagram-library.component";
import {DiagramConfig} from "./diagram-config";
import {DiagramInitialData} from "./diagram-initial-data";
import {AddToolsComponent} from "./add-tools/add-tools.component";

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
        const componentFactory = this.componentFactoryResolver.resolveComponentFactory(DiagramNodeComponent);
        const componentInstance = this.viewContainerRef.createComponent(componentFactory);
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
        library: (panel: any, state: any, diagramMakerContainer: HTMLElement) => {
          //diagramMakerContainer.innerHTML = '';
          console.log(this.libraryComponent == null || this.libraryComponent?.instance?.selectedNode$?.id != this.currentNode?.id)
          console.log( this.libraryComponent?.instance?.selectedNode$?.id +" "+ this.currentNode?.id)
          if (this.libraryComponent == null) {
            this.libraryComponent = this.viewContainerRef.createComponent(this.componentFactory);
            this.libraryComponent.instance.editNode.subscribe(editForm => this.editState(editForm));
            this.libraryComponent.instance.resetSelectedNode.subscribe(() => this.currentNode = undefined);
            //console.log(this.currentNode);
            diagramMakerContainer.appendChild(this.libraryComponent.location.nativeElement);
          }
          if (this.currentNode) {
            this.libraryComponent.instance.selectedNode = this.currentNode;
          }
        },
        tools: (panel: any, state : any, diagramMakerContainer: HTMLElement ) => {
          let addToolsFactory = this.componentFactoryResolver.resolveComponentFactory(AddToolsComponent);
          let addToolsComponent = this.viewContainerRef.createComponent(addToolsFactory);
          diagramMakerContainer.appendChild(addToolsComponent.location.nativeElement);
        }
      },
    },
    actionInterceptor: (action: Action, dispatch: Dispatch<Action>) => {
      if (action.type === DiagramMakerActions.NODE_CREATE) {
        const createAction = action as CreateNodeAction<any>;
        if (createAction.payload.typeId == "node-normal" || createAction.payload.typeId == "node-start"){

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
      else if (action.type === DiagramMakerActions.EDGE_CREATE) {
        let edgePossible = true;
        const edgeMap = new Map(Object.entries(this.diagramMaker.store.getState().edges))
        let createEdgeAction = action as CreateEdgeAction<{}>;
        for (let edge of edgeMap.values()) {
          if (edge.dest == createEdgeAction.payload.dest) {
            return edgePossible = false
          } else {
            edgePossible = true;
          }
        }
        if (edgePossible) {
          dispatch(createEdgeAction);
        }
      } else {
        dispatch(action);
      }
      //console.log(action);
      //console.log(this.nodeComponentInstances);
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
    console.log(stageDef.env instanceof Map);
    console.log(this.project.pipelineDefinition.stages[1].env instanceof Map);
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
