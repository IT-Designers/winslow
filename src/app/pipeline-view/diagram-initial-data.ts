import {DiagramMakerEdge, DiagramMakerNode, EditorMode, PositionAnchor} from "diagram-maker";
import {StageDefinitionInfo} from "../api/project-api.service";

export class DiagramInitialData {

  getInitData(project) {
    let edges: { [id: string]: DiagramMakerEdge<{}> } = {};
    let nodes: { [id: string]: DiagramMakerNode<StageDefinitionInfo> } = {};

    for (let i = 0; i < project.pipelineDefinition.stages.length; i++) {
      nodes[`n${i}`] = {
        id: `n${i}`,
        typeId: `${i == 0 ? "node-start" : "node-normal"}`,
        diagramMakerData: {
          position: {x: 250 * (i + 1) - 200, y: 200},
          size: {width: 200, height: 75},
        },
        consumerData: project.pipelineDefinition.stages[i]
      }
      if (i < (project.pipelineDefinition.stages.length - 1)) {
        edges[`edge${i}`] = {
          id: `edge${i}`,
          src: `n${i}`,
          dest: `n${i + 1}`,
          diagramMakerData: {}
        }

      }
    }

    let initialData = {
      nodes,
      edges,
      panels: {
        library: {
          id: 'library',
          position: {x: 10, y: 10},
          size: {width: 320, height: 400},
          positionAnchor: PositionAnchor.TOP_RIGHT,
        },
        tools: {
          id: 'tools',
          position: {x: 10, y: 10},
          size: {width: 600, height: 46},
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
    return initialData;
  }
}
