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
  ConnectorPlacement, DiagramMakerEdge, DiagramMakerPotentialNode,
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
  public inputDiagram!: DiagramMakerData<StageDefinitionInfo, {}>;

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
        this.nodeComponentInstances.push(componentInstance);
        if (node.diagramMakerData.selected) {
          componentInstance.instance.selected = true;
        }
      },
      potentialNode: (node: DiagramMakerPotentialNode, diagramMakerContainer: HTMLElement) => {
        diagramMakerContainer.innerHTML = '';
        const componentFactory = this.componentFactoryResolver.resolveComponentFactory(DiagramNodeComponent);
        const componentInstance = this.viewContainerRef.createComponent(componentFactory);
        diagramMakerContainer.appendChild(componentInstance.location.nativeElement);
        this.nodeComponentInstances.push(componentInstance);
      },
      destroy: (diagramMakerContainer: HTMLElement, consumerContainer?: HTMLElement | void) => {
        // TODO update nodeComponentInstances
      },
      panels: {
        library: ( panel: any, state: any, diagramMakerContainer: HTMLElement) => {
          diagramMakerContainer.innerHTML = '';
          const componentFactory = this.componentFactoryResolver.resolveComponentFactory(DiagramLibraryComponent);
          const componentInstance = this.viewContainerRef.createComponent(componentFactory);
          diagramMakerContainer.appendChild(componentInstance.location.nativeElement);
        }
      },
    },
  };

  nodeComponentInstances = [];

  constructor(private viewContainerRef: ViewContainerRef,
              private componentFactoryResolver: ComponentFactoryResolver) {
  }

  ngOnInit() {
    let nodes: { [id: string]: DiagramMakerNode<StageDefinitionInfo> } = {};
    let edges: { [id: string]: DiagramMakerEdge<StageDefinitionInfo> } = {};

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
    console.log()

    this.inputDiagram = {
      nodes,
      edges,
      panels: {
        library: {
          id: 'library',
          position: {x: 0, y: 0},
          size: {width: 300, height: 900},
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

  /*
  this.inputDiagram = {
    nodes: {
      node1: {
        id: 'node1',
        diagramMakerData: {
          position: {x: 200, y: 150},
          size: {width: 100, height: 50},
        },
      },
      node2: {
        id: 'node2',
        diagramMakerData: {
          position: {x: 400, y: 300},
          size: {width: 100, height: 50},
        },
      },
    },
    edges: {
        edge1: {
         id: 'edge1',
          src: 'node1',
          dest: 'node2',
          diagramMakerData: {}
      }
    },
    panels: {},
    workspace: {
      position: {x: 0, y: 0},
      scale: 1,
      canvasSize: {width: 3200, height: 1600},
      viewContainerSize: {
        width: window.innerWidth,
        height: window.innerHeight,
      },
    },
    editor: {mode: EditorMode.DRAG},
  };*/

  ngAfterViewInit(): void {
    this.diagramMaker = new DiagramMaker(
      this.diagramEditorContainer.nativeElement,
      this.config,
      {initialData: this.inputDiagram}
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
    this.nodeComponentInstances.forEach( instance => instance.destroy());
  }


}
