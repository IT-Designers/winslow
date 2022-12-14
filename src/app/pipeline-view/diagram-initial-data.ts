import {DiagramMakerEdge, DiagramMakerNode, EditorMode, PositionAnchor} from "diagram-maker";
import {PipelineDefinitionInfo, StageDefinitionInfo} from "../api/winslow-api";
import {PipelineDefinitionInfoExt} from '../api/project-api.service';

export class DiagramInitialData {

  getInitData(project) {
    let edges: { [id: string]: DiagramMakerEdge<{}> } = {};
    let nodes: { [id: string]: DiagramMakerNode<StageDefinitionInfo> } = {};
    let pipelineInfo = new PipelineDefinitionInfoExt(Object.assign({}, project.pipelineDefinition));
    delete pipelineInfo.stages; delete pipelineInfo.hasActionMarker;  delete pipelineInfo.hasActionMarkerFor;
    delete pipelineInfo.userInput.requiredEnvVariables;
    nodes[pipelineInfo.id] = {
      id: pipelineInfo.id,
      typeId: "node-start",
      diagramMakerData: {
        position: {x: 50, y: 200},
        size: {width: 200, height: 75},
      },
      // @ts-ignore
      consumerData: pipelineInfo
    };
    for (let i = 0; i < project.pipelineDefinition.stages.length; i++) {
      nodes[project.pipelineDefinition.stages[i].id] = {
        id: project.pipelineDefinition.stages[i].id,
        typeId: "node-normal",
        diagramMakerData: {
          position: {x: 250 * (i + 2) - 200, y: 200},
          size: {width: 200, height: 75},
        },
        consumerData: project.pipelineDefinition.stages[i]
      };
      if (i < (project.pipelineDefinition.stages.length - 1)) {
        edges[`edge${i}`] = {
          id: `edge${i}`,
          src: project.pipelineDefinition.stages[i].id,
          dest: project.pipelineDefinition.stages[i+1].id,
          diagramMakerData: {}
        }

      }
    }
    edges["edgeStart"] = {
      id: 'edgeStart',
      src: pipelineInfo.id,
      dest: project.pipelineDefinition.stages[0].id,
      diagramMakerData: {}
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
          size: {width: 521, height: 46},
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
