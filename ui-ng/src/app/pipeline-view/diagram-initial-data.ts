import {DiagramMakerEdge, DiagramMakerNode, EditorMode, PositionAnchor} from "diagram-maker";
import {PipelineDefinitionInfo, StageDefinitionInfo} from "../api/winslow-api";

export class DiagramInitialData {

  getInitData(pipelineDefinition) {
    let edges: { [id: string]: DiagramMakerEdge<{}> } = {};
    let nodes: { [id: string]: DiagramMakerNode<StageDefinitionInfo> } = {};
    let pipelineInfo = new PipelineDefinitionInfo(Object.assign({}, pipelineDefinition));
    // HELPER: deletes unnecessary parts of the object
    delete pipelineInfo.stages;
    nodes[pipelineInfo.id] = {    //first node - PipelineDefInfo
      id: pipelineInfo.id,
      typeId: "node-start",
      diagramMakerData: {
        position: {x: 50, y: 500},
        size: {width: 200, height: 75},
      },
      // @ts-ignore
      consumerData: pipelineInfo
    };
    for (let i = 0; i < pipelineDefinition.stages.length; i++) {    //All other Stages and Gateways
      let nodeType : String;
      if (pipelineDefinition.stages[i]['@type'] == 'Worker'){nodeType = 'node-normal';}
      else if(pipelineDefinition.stages[i]['@type'] == 'AndGateway'){
        if (pipelineDefinition.stages[i].gatewaySubType == 'SPLITTER') { nodeType = 'node-and-splitter'}
        else{ nodeType = 'node-all-merger'}
      }
      else if(pipelineDefinition.stages[i]['@type'] == 'XorGateway'){
        if (pipelineDefinition.stages[i].gatewaySubType == 'SPLITTER') { nodeType = 'node-if-splitter' }
        else{nodeType = 'node-any-merger'}
      }
      nodes[pipelineDefinition.stages[i].id] = {
        id: pipelineDefinition.stages[i].id,
        typeId: `${nodeType}`,
        diagramMakerData: {
          position: {x: 250 * (i + 2) - 200, y: 500},
          size: {width: 200, height: 75},
        },
        consumerData: pipelineDefinition.stages[i]
      };
      if (i < (pipelineDefinition.stages.length - 1)) {
        for(let u = 0; u < pipelineDefinition.stages[i].nextStages.length; u++){    //edges created by stage.nextStages Array
          edges[`edge${i}-${u}`] = {
            id: `edge${i}-${u}`,
            src: pipelineDefinition.stages[i].id,
            dest: pipelineDefinition.stages[i].nextStages[u],
            diagramMakerData: {}
          }
        }
      }
    }
    edges["edgeStart"] = {    //first edge from PipelineDef to the first stage
      id: 'edgeStart',
      src: pipelineInfo.id,
      dest: pipelineDefinition.stages[0].id,
      diagramMakerData: {}
    }
    /*console.log(edges);*/

    let initialData = {
      nodes,
      edges,
      panels: {
        library: {    //edit-board data
          id: 'library',
          position: {x: 10, y: 10},
          size: {width: 350, height: 500},
          positionAnchor: PositionAnchor.TOP_RIGHT,
        },
        tools: {      //add Elements board data
          id: 'tools',
          position: {x: 10, y: 10},
          size: {width: 650, height: 46},
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
