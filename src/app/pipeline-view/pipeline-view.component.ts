import {
  AfterViewInit,
  Component,
  ElementRef,
  OnDestroy,
  ViewChild,
  Input,
  OnChanges
} from '@angular/core';
import {
  DiagramMaker,
  DiagramMakerConfig,
  DiagramMakerNode,
  DiagramMakerData,
  EditorMode,
  ConnectorPlacement,
} from 'diagram-maker';

import {ProjectInfo} from "../api/project-api.service";

@Component({
  selector: 'app-pipeline-view',
  templateUrl: './pipeline-view.component.html',
  styleUrls: ['./pipeline-view.component.css']
})
export class PipelineViewComponent implements AfterViewInit, OnDestroy, OnChanges {

  @Input() public project: ProjectInfo;

  diagramMaker!: DiagramMaker;

  @ViewChild('diagramEditorContainer')
  diagramEditorContainer!: ElementRef;

  config: DiagramMakerConfig<{}, {}> = {
    options: {
      connectorPlacement: ConnectorPlacement.LEFT_RIGHT,
      showArrowhead: true,
    },

    renderCallbacks: {
      node: (node: DiagramMakerNode<{}>, diagramMakerContainer: HTMLElement) => {
        const newDiv = document.createElement('div');
        const newContent = document.createTextNode(node.id);
        newDiv.appendChild(newContent);
        newDiv.classList.add('example-node');
        if (node.diagramMakerData.selected) {
          newDiv.classList.add('selected');
        }
        diagramMakerContainer.innerHTML = '';
        diagramMakerContainer.appendChild(newDiv);
      },
      destroy: (container: HTMLElement) => {
      },
      panels: {},
    },
  };

  graph: DiagramMakerData<{}, {}> = {
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
  };

  ngAfterViewInit(): void {
    this.diagramMaker = new DiagramMaker(
      this.diagramEditorContainer.nativeElement,
      this.config,
      {initialData: this.graph}
    );
  }

  ngOnChanges() {
    window.addEventListener('resize', () => {
      this.diagramMaker.updateContainer();
    });
  }

  ngOnDestroy(): void {
    if (this.diagramEditorContainer.nativeElement != null) {
      this.diagramMaker.destroy();
    }
  }


}
