import {
  AfterViewInit,
  OnInit,
  Component,
  ElementRef,
  OnDestroy,
  ViewChild,
  Input,
  OnChanges, ViewContainerRef, ComponentFactoryResolver
} from '@angular/core';
import {
  DiagramMaker,
  DiagramMakerConfig,
  DiagramMakerNode,
  DiagramMakerData,
  EditorMode,
  ConnectorPlacement,
  DiagramMakerEdge,
  DiagramMakerPotentialNode,
  Action,
  Dispatch,
  DiagramMakerActions, CreateNodeAction
} from 'diagram-maker';

import {ProjectInfo, StageDefinitionInfo} from "../api/project-api.service";
import {DiagramNodeComponent} from "./diagram-node/diagram-node.component";
import {DiagramLibraryComponent} from "./diagram-library/diagram-library.component";

@Component({
  selector: 'app-pipeline-view',
  templateUrl: './pipeline-view.component.html',
  styleUrls: ['./pipeline-view.component.css']
})
export class PipelineViewComponent implements OnInit, AfterViewInit, OnDestroy, OnChanges {

  @Input() public project: ProjectInfo;

  public diagramMaker!: DiagramMaker;
  public initialData!: DiagramMakerData<StageDefinitionInfo, {}>;
  public currentNode?: DiagramMakerNode<StageDefinitionInfo>;

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
        // this.nodeComponentInstances.push(componentInstance);
        // console.log(this.nodeComponentInstances);
        if (node.diagramMakerData.selected) {
          componentInstance.instance.selected = true;
          this.currentNode = node;
        }
      },
      potentialNode: (node: DiagramMakerPotentialNode, diagramMakerContainer: HTMLElement) => {
        diagramMakerContainer.innerHTML = '';
        const componentFactory = this.componentFactoryResolver.resolveComponentFactory(DiagramNodeComponent);
        const componentInstance = this.viewContainerRef.createComponent(componentFactory);
        console.log("test")
        diagramMakerContainer.appendChild(componentInstance.location.nativeElement);
        this.nodeComponentInstances.push(componentInstance);
      },
      destroy: (diagramMakerContainer: HTMLElement, consumerContainer?: HTMLElement | void) => {
        // TODO update nodeComponentInstances
      },
      panels: {
        library: (panel: any, state: any, diagramMakerContainer: HTMLElement) => {
          diagramMakerContainer.innerHTML = '';
          const componentFactory = this.componentFactoryResolver.resolveComponentFactory(DiagramLibraryComponent);
          const componentInstance = this.viewContainerRef.createComponent(componentFactory);
          componentInstance.instance.resetSelectedNode.subscribe(() => this.currentNode = undefined)
          diagramMakerContainer.appendChild(componentInstance.location.nativeElement);
          if (this.currentNode) {
            componentInstance.instance.selectedNode = this.currentNode;
          }
        }
      },
    },
    actionInterceptor: (action: Action, dispatch: Dispatch<Action>, getState: () => DiagramMakerData<{}, {}>) => {
      if (action.type === DiagramMakerActions.NODE_CREATE) {
        const createAction = action as CreateNodeAction<any>;
        const stageDef = new StageDefinitionInfo();
        stageDef.name = "NewStage123"
        console.log(stageDef);
        this.project.pipelineDefinition.stages.push(stageDef)
        let newAction: CreateNodeAction<{}> = {
          type: DiagramMakerActions.NODE_CREATE,
          payload: {
            id: `n${createAction.payload.id}`,
            typeId: 'node',
            position: {x: createAction.payload.position.x, y: createAction.payload.position.y},
            size: {width: 200, height: 75},
            consumerData: stageDef
          }
        }
        dispatch(newAction);
        console.log(this.project.pipelineDefinition.stages)
      }
      if (action.type !== DiagramMakerActions.NODE_CREATE) {
        dispatch(action);
      }
      console.log(action);
    },
  };

  nodeComponentInstances = [];

  constructor(private viewContainerRef: ViewContainerRef,
              private componentFactoryResolver: ComponentFactoryResolver) {
  }


  ngOnInit() {

    let edges: { [id: string]: DiagramMakerEdge<{}> } = {};
    let nodes: { [id: string]: DiagramMakerNode<StageDefinitionInfo> } = {};

    for (let i = 0; i < this.project.pipelineDefinition.stages.length; i++) {
      nodes[`n${i}`] = {
        id: `n${i}`,
        typeId: 'node',
        diagramMakerData: {
          position: {x: 350 * (i + 1), y: 200},
          size: {width: 200, height: 75},
        },
        consumerData: this.project.pipelineDefinition.stages[i]
      }
      if (i < (this.project.pipelineDefinition.stages.length - 1)) {
        edges[`n${i}`] = {
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
          position: {x: 20, y: 0},
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
    this.diagramMaker = new DiagramMaker(
      this.diagramEditorContainer.nativeElement,
      this.config,
      {initialData: this.initialData}
    );

    window.addEventListener('resize', () => {
      this.diagramMaker.updateContainer();
    });
  }

  ngOnChanges() {
  }

  ngOnDestroy(): void {
    if (this.diagramEditorContainer.nativeElement != null) {
      this.diagramMaker.destroy();
    }
    this.nodeComponentInstances.forEach(instance => instance.destroy());
  }


}
