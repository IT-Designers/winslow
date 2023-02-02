import {DiagramMakerEdge, DiagramMakerNode, EditorMode, PositionAnchor} from "diagram-maker";
import {PipelineDefinitionInfo, StageDefinitionInfo} from "../api/winslow-api";

export class DiagramInitialData {

  getInitData(project) {
    let edges: { [id: string]: DiagramMakerEdge<{}> } = {};
    let nodes: { [id: string]: DiagramMakerNode<StageDefinitionInfo> } = {};
    let pipelineInfo = new PipelineDefinitionInfo(Object.assign({}, project.pipelineDefinition));
    delete pipelineInfo.stages; delete pipelineInfo.hasActionMarker;  delete pipelineInfo.hasActionMarkerFor;
    delete pipelineInfo.userInput.requiredEnvVariables;
    nodes[pipelineInfo.id] = {
      id: pipelineInfo.id,
      typeId: "node-start",
      diagramMakerData: {
        position: {x: 50, y: 500},
        size: {width: 200, height: 75},
      },
      // @ts-ignore
      consumerData: pipelineInfo
    };
    for (let i = 0; i < project.pipelineDefinition.stages.length; i++) {
      let nodeType : String;
      if (project.pipelineDefinition.stages[i]['@type'] == 'Worker'){nodeType = 'node-normal';}
      else if(project.pipelineDefinition.stages[i]['@type'] == 'AndGateway'){
        if (project.pipelineDefinition.stages[i].gatewaySubType == 'SPLITTER') { nodeType = 'node-and-splitter'}
        else{ nodeType = 'node-all-merger'}
      }
      else if(project.pipelineDefinition.stages[i]['@type'] == 'XorGateway'){
        if (project.pipelineDefinition.stages[i].gatewaySubType == 'SPLITTER') { nodeType = 'node-if-splitter' }
        else{nodeType = 'node-any-merger'}
      }
      nodes[project.pipelineDefinition.stages[i].id] = {
        id: project.pipelineDefinition.stages[i].id,
        typeId: `${nodeType}`,
        diagramMakerData: {
          position: {x: 250 * (i + 2) - 200, y: 500},
          size: {width: 200, height: 75},
        },
        consumerData: project.pipelineDefinition.stages[i]
      };
      if (i < (project.pipelineDefinition.stages.length - 1)) {
        for(let u = 0; u < project.pipelineDefinition.stages[i].nextStages.length; u++){
          edges[`edge${i}-${u}`] = {
            id: `edge${i}-${u}`,
            src: project.pipelineDefinition.stages[i].id,
            dest: project.pipelineDefinition.stages[i].nextStages[u],
            diagramMakerData: {}
          }
        }
      }
    }
    edges["edgeStart"] = {
      id: 'edgeStart',
      src: pipelineInfo.id,
      dest: project.pipelineDefinition.stages[0].id,
      diagramMakerData: {}
    }
    console.log(edges);

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
          size: {width: 630, height: 46},
        },
      },
      workspace: {
        position: {x: 0, y: 0},
        scale: 1,
        canvasSize: {width: 5000, height: 3000},
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
