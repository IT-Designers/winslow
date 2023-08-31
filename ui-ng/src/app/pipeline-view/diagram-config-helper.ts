import {
  ConnectorPlacement,
  DragWorkspaceAction,
  Layout,
  VisibleConnectorTypes,
  WorkflowLayoutDirection,
  WorkspaceActionsType
} from "diagram-maker";

export class DiagramConfigHelper {

  getNodeTypes(){
    let nodeTypes = {
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
    return nodeTypes;
  }

  getApiSwitch(action , diagramMaker) {   //used by the function icons in the edit board
    {
      switch (action) {
        case 'fit':
          diagramMaker.api.focusNode(Object.keys(diagramMaker.store.getState().nodes)[0]);
          diagramMaker.store.dispatch({
            type: 'WORKSPACE_DRAG',
            payload: {
              position:{
                x: 0,
                y: 400
              }
            }
          });
          break;
        case 'layout':
          diagramMaker.api.layout({
            direction: WorkflowLayoutDirection.LEFT_RIGHT,
            distanceMin: 50,
            layoutType: Layout.WORKFLOW,
            fixedNodeId: diagramMaker.store.getState().nodes[Object.keys(diagramMaker.store.getState().nodes)[0]].id,
          })
          break;
        case 'zoomIn':
          diagramMaker.api.zoomIn(100);
          break;
        case 'zoomOut':
          diagramMaker.api.zoomOut(100);
          break;
        case 'undo':
          diagramMaker.api.undo();
          break;
        case 'redo':
          diagramMaker.api.redo();
          break;
        case 'save':
          console.log('Save API call');

          console.dir(diagramMaker.store.getState())
          break;
        default:
          break;
      }
    }
  }
}

