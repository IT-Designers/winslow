import {ConnectorPlacement, DiagramMaker, Layout, VisibleConnectorTypes, WorkflowLayoutDirection} from "diagram-maker";

export type DiagramAction = 'fit' | 'layout' | 'zoomIn' | 'zoomOut' | 'undo' | 'redo' | 'save'


export class DiagramConfigHelper {

  getNodeTypes() {
    return {
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
    };
  }

  getApiSwitch(action: DiagramAction, diagramMaker: DiagramMaker): void {   //used by the function icons in the edit board
    {
      switch (action) {
        case 'fit':
          diagramMaker.api.focusNode(Object.keys(diagramMaker.store.getState().nodes)[0]);
          diagramMaker.store.dispatch({
            type: 'WORKSPACE_DRAG',
            payload: {
              position: {
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

