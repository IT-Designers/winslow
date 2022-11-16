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
  EditorMode, VisibleConnectorTypes,
} from 'diagram-maker';

import {ImageInfo, ProjectInfo, StageDefinitionInfo} from "../api/project-api.service";
import {DiagramNodeComponent} from "./diagram-node/diagram-node.component";
import {DiagramLibraryComponent} from "./diagram-library/diagram-library.component";

@Component({
  selector: 'app-pipeline-view',
  templateUrl: './pipeline-view.component.html',
  styleUrls: ['./pipeline-view.component.css']
})
export class PipelineViewComponent implements OnInit, AfterViewInit, OnDestroy{

  @Input() public project: ProjectInfo;

  public diagramMaker!: DiagramMaker;
  public initialData!: DiagramMakerData<StageDefinitionInfo, {}>;
  public currentNode?: DiagramMakerNode<StageDefinitionInfo>;
  public componentFactory = this.componentFactoryResolver.resolveComponentFactory(DiagramLibraryComponent);
  public libraryComponent = null;

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
          if (this.libraryComponent == null){
          this.libraryComponent = this.viewContainerRef.createComponent(this.componentFactory);
          this.libraryComponent.instance.editNode.subscribe(editForm => this.editState(editForm));
          this.libraryComponent.instance.resetSelectedNode.subscribe(() => this.currentNode = undefined);
          //console.log(this.currentNode);
          diagramMakerContainer.appendChild(this.libraryComponent.location.nativeElement);
          }
          if (this.currentNode) {
              this.libraryComponent.instance.selectedNode = this.currentNode;
          }
        }
      },
    },
    actionInterceptor: (action: Action, dispatch: Dispatch<Action>) => {
      if (action.type === DiagramMakerActions.NODE_CREATE) {
        const createAction = action as CreateNodeAction<any>;
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
      else if (action.type === DiagramMakerActions.EDGE_CREATE){
        let edgePossible = true;
        const edgeMap = new Map(Object.entries(this.diagramMaker.store.getState().edges))
        let createEdgeAction = action as CreateEdgeAction<{}>;
        for (let edge of edgeMap.values()){
          if (edge.dest == createEdgeAction.payload.dest){ return edgePossible = false }
          else {edgePossible = true;}
        }
        if (edgePossible){
          dispatch(createEdgeAction);
        }
      }
      else {
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

    let edges: { [id: string]: DiagramMakerEdge<{}> } = {};
    let nodes: { [id: string]: DiagramMakerNode<StageDefinitionInfo> } = {};

    for (let i = 0; i < this.project.pipelineDefinition.stages.length; i++) {
      nodes[`n${i}`] = {
        id: `n${i}`,
        typeId: `${ i == 0 ? "node-start" : "node-normal" }`,
        diagramMakerData: {
          position: {x: 200+250 * (i + 1), y: 200},
          size: {width: 200, height: 75},
        },
        consumerData: this.project.pipelineDefinition.stages[i]
      }
      if (i < (this.project.pipelineDefinition.stages.length - 1)) {
        edges[`edge${i}`] = {
          id: `edge${i}`,
          src: `n${i}`,
          dest: `n${i + 1}`,
          diagramMakerData: {}
        }

      }
    }

    this.initialData = {
      nodes,
      edges,
      panels: {
        library: {
          id: 'library',
          position: {x: 20, y: 20},
          size: {width: 320, height: 600},
        },
      },
      workspace: {
        position: {x: 0, y: 0},
        scale: 1,
        canvasSize: {width: 5000, height: 5000},
        viewContainerSize: {
          width: window.innerWidth,
          height: window.innerHeight,
        },
      },
      editor: {mode: EditorMode.DRAG},
    }

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
