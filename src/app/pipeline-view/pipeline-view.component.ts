import {
  AfterViewInit,
  Component,
  ComponentFactoryResolver,
  ComponentRef,
  ElementRef,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  Output,
  SimpleChanges,
  ViewChild,
  ViewContainerRef
} from '@angular/core';
import {
  Action, ActionsQueue,
  ConnectorPlacement,
  CreateEdgeAction,
  CreateNodeAction,
  DeleteItemsAction,
  DeselectAction,
  DiagramMaker, DiagramMakerAction,
  DiagramMakerActions,
  DiagramMakerConfig,
  DiagramMakerData,
  DiagramMakerNode,
  DiagramMakerPotentialNode,
  Dispatch,
  DragPanelAction,
  Layout,
  ResizePanelAction,
  WorkflowLayoutDirection,
} from 'diagram-maker';


import {DiagramNodeComponent} from './diagram-node/diagram-node.component';
import {DiagramLibraryComponent} from './diagram-library/diagram-library.component';
import {DiagramConfigHelper} from './diagram-config-helper';
import {DiagramInitialData} from './diagram-initial-data';
import {AddToolsComponent} from './add-tools/add-tools.component';
import {DiagramGatewayComponent} from './diagram-gateway/diagram-gateway.component';
import {PipelineDefinitionInfo, Raw, StageDefinitionInfo, StageDefinitionInfoUnion,} from '../api/winslow-api';
import {DefaultApiServiceService} from "../api/default-api-service.service";
import {HttpClient} from "@angular/common/http";

interface DeletedStage {
  stageIndex: number;
  deletedStage: StageDefinitionInfoUnion;
  idsOfPreviousStages?: string[];
}

@Component({
  selector: 'app-pipeline-view',
  templateUrl: './pipeline-view.component.html',
  styleUrls: ['./pipeline-view.component.css']
})
export class PipelineViewComponent implements OnInit, AfterViewInit, OnChanges, OnDestroy {

  @Input() public pipelineDefinition!: PipelineDefinitionInfo;    //used to store the state of the pipeline before it was changed
  @Input() public pipelineDefinitionEdit!: PipelineDefinitionInfo;    //used to store all changes made to the pipeline
  @Output() public onSave = new EventEmitter<PipelineDefinitionInfo>();

  public diagramMaker!: DiagramMaker;
  public initialData!: DiagramMakerData<StageDefinitionInfo, {}>;
  public currentNode?: DiagramMakerNode<StageDefinitionInfo>;
  public componentFactory = this.componentFactoryResolver.resolveComponentFactory(DiagramLibraryComponent);
  public libraryComponent: ComponentRef<DiagramLibraryComponent> | null = null;
  public configClass = new DiagramConfigHelper();
  public initClass = new DiagramInitialData();
  public defaultGetter = new DefaultApiServiceService(this.client);
  public saveStatus: boolean = true;


  @ViewChild('diagramEditorContainer')
  diagramEditorContainer!: ElementRef;

  deletedStages:  DeletedStage[] = [];

  returnUndoHistoryAsCorrectObject(hist: any | undefined): CreateNodeAction<any> {
    /*if (hist implements CreateNodeAction<any>)*/
    return hist as CreateNodeAction<any>;
  }

  config: DiagramMakerConfig<StageDefinitionInfo, unknown> = {
    options: {
      connectorPlacement: ConnectorPlacement.LEFT_RIGHT,
      showArrowhead: true,
    },
    renderCallbacks: {
      node: (node: DiagramMakerNode<StageDefinitionInfo>, diagramMakerContainer: HTMLElement): void => {
        diagramMakerContainer.innerHTML = '';
        let componentInstance = undefined;
        if (node.typeId == 'node-normal' || node.typeId == 'node-start') {
          const componentFactory = this.componentFactoryResolver.resolveComponentFactory(DiagramNodeComponent);
          componentInstance = this.viewContainerRef.createComponent(componentFactory);
        } else {
          const componentFactory = this.componentFactoryResolver.resolveComponentFactory(DiagramGatewayComponent);
          componentInstance = this.viewContainerRef.createComponent(componentFactory);
        }
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
      },
      destroy: () => {
        this.nodeComponentInstances.forEach(instance => instance.destroy());
        // TODO update nodeComponentInstances
      },
      panels: {
        library: (panel: any, state: any, diagramMakerContainer: HTMLElement) => {  //panel to edit Stages
          if (this.libraryComponent == null) {
            this.libraryComponent = this.viewContainerRef.createComponent(this.componentFactory);
            this.libraryComponent.instance.editNode.subscribe(editForm => { //edit Form is what is stored in Diagram Maker of the definition of the stage / pipeline
              //gets triggered when value is changed
              console.dir(editForm);

              //---------- All stages have the @type attribute, but not the pipeline itself ----------
              if (!editForm["@type"]) {//---------- catches changes in the pipeline def ----------
                //---------- The editForm object of the pipeline doesn't have the stages ----------
                //---------- Thus it cant completely overwrite this.pipelineDefinitionEdit ----------
                this.pipelineDefinitionEdit.id = editForm.id;
                this.pipelineDefinitionEdit.name = editForm.name;
                this.pipelineDefinitionEdit.description = editForm.description;
                this.pipelineDefinitionEdit.userInput = editForm.userInput;
                this.pipelineDefinitionEdit.environment = editForm.environment;
                this.pipelineDefinitionEdit.deletionPolicy = editForm.deletionPolicy;
                this.pipelineDefinitionEdit.publicAccess = editForm.publicAccess;
              } else {    //catches changes on the stages
                editForm.nextStages = Object.values(editForm.nextStages);
                this.pipelineDefinitionEdit.stages.find((stage) => stage.id === editForm.id);
                for (let i = 0; i < this.pipelineDefinitionEdit.stages.length; i++) {
                  if (this.pipelineDefinitionEdit.stages[i].id === editForm.id) {
                    this.pipelineDefinitionEdit.stages[i] = editForm;
                  }
                }

              }
              this.editState(editForm);
            });
            this.libraryComponent.instance.resetSelectedNode.subscribe(() => {
              this.currentNode = undefined;
              let deselectAction: DeselectAction = {
                type: DiagramMakerActions.WORKSPACE_DESELECT,
              }
              this.diagramMaker.api.dispatch(deselectAction);
              let makeSmallAction: ResizePanelAction = {
                type: DiagramMakerActions.PANEL_RESIZE,
                payload: {
                  id: this.diagramMaker.store.getState().panels.library.id,
                  size: {width: 500, height: 60},
                }
              };
              this.diagramMaker.api.dispatch(makeSmallAction);
            });
            this.libraryComponent.instance.diagramApiCall.subscribe((event) => {   //these are calls made by the buttons in the library panel
              switch (event.action) {
                case 'fit':
                  this.diagramMaker.api.focusNode(Object.keys(this.diagramMaker.store.getState().nodes)[0]);
                  this.diagramMaker.store.dispatch({
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
                  this.diagramMaker.api.layout({
                    direction: WorkflowLayoutDirection.LEFT_RIGHT,
                    distanceMin: 50,
                    layoutType: Layout.WORKFLOW,
                    fixedNodeId: this.diagramMaker.store.getState().nodes[Object.keys(this.diagramMaker.store.getState().nodes)[0]].id,
                  })
                  break;
                case 'zoomIn':
                  this.diagramMaker.api.zoomIn(100);
                  break;
                case 'zoomOut':
                  this.diagramMaker.api.zoomOut(100);
                  break;
                case 'undo':
                  //console.dir(this.diagramMaker.store.getState().undoHistory);
                  const undoQueue: ActionsQueue<DiagramMakerAction<{}, {}>> | undefined = this.diagramMaker.store.getState().undoHistory?.undoQueue;
                  if (undoQueue) {
                    if ("action" in undoQueue[0]) {
                      const lastAction: DiagramMakerAction<{}, {}> = undoQueue[0].action as DiagramMakerAction<{}, {}>
                      console.dir(lastAction);
                      if (lastAction.type === DiagramMakerActions.NODE_CREATE) {    //undo creation of stages
                        const undoAction: CreateNodeAction<any> = undoQueue[0].action as CreateNodeAction<any>;
                        this.removeStageFromEditObject(undoAction?.payload.id);
                      } else if (lastAction.type === DiagramMakerActions.DELETE_ITEMS) {    //undo deletion of stages
                        const undoAction: DeleteItemsAction = undoQueue[0].action as DeleteItemsAction;
                        for (let stageId of undoAction.payload.nodeIds) {
                          this.reAddStageToEditObject(stageId);
                        }
                      }
                    }
                  }
                  this.diagramMaker.api.undo();
                  break;
                case 'redo':
                  this.diagramMaker.store.getState().nodes;
                  const redoQueue: ActionsQueue<DiagramMakerAction<{}, {}>> | undefined = this.diagramMaker.store.getState().undoHistory?.redoQueue;
                  if (redoQueue) {
                    if ("action" in redoQueue[0]) {
                      const lastAction: DiagramMakerAction<{}, {}> = redoQueue[0].action as DiagramMakerAction<{}, {}>
                      if (lastAction.type === DiagramMakerActions.NODE_CREATE) {    //redo creation of stages
                        const redoAction: CreateNodeAction<any> = redoQueue[0].action as CreateNodeAction<any>;
                        this.reAddStageToEditObject(redoAction.payload.id);
                      } else if (lastAction.type === DiagramMakerActions.DELETE_ITEMS) {    //redo deletion of stages
                        const redoAction: DeleteItemsAction = redoQueue[0].action as DeleteItemsAction;
                        for (let stageId of redoAction.payload.nodeIds) {
                          this.removeStageFromEditObject(stageId);
                        }
                      }
                    }
                  }
                  this.diagramMaker.api.redo();
                  break;
                case 'save':
                  this.saveStatus = true;
                  console.log("---------------------------------- SAVE ----------------------------------");
                  this.onSave.emit(this.pipelineDefinitionEdit);
                  break;
                case 'flat-delete':
                  this.flatDeleteElement(event.node);
                  break;
                /*case 'deep-delete':
                  this.deepDeleteElement(event.node);
                  break;*/
                default:
                  break;
              }
              // this.configClass.getApiSwitch(action, this.diagramMaker);   // is the same switch as above
            });
            diagramMakerContainer.appendChild(this.libraryComponent.location.nativeElement);
          }
          this.libraryComponent.instance.saveStatus = this.saveStatus;
          if (this.currentNode) {
            this.libraryComponent.instance.selectedNode = this.currentNode;
          }
        },
        tools: (panel: any, state: any, diagramMakerContainer: HTMLElement) => {    //Bar to Add Nodes to diagramMaker
          let addToolsFactory = this.componentFactoryResolver.resolveComponentFactory(AddToolsComponent);
          let addToolsComponent = this.viewContainerRef.createComponent(addToolsFactory);
          diagramMakerContainer.appendChild(addToolsComponent.location.nativeElement);
        }
      },
    },
    actionInterceptor: (action: Action, dispatch: Dispatch<Action>) => {  //Intercepts actions before diagramMaker saves them into the store
      //console.log(action);

      //-------------------- This intercepts the creations of new nodes --------------------
      if (action.type === DiagramMakerActions.NODE_CREATE) {      //required extra steps to append Data to the nodes before dispatching
        const createAction = action as CreateNodeAction<any>;
        //-------------------- Differentiated by the typeId of the node --------------------
        if (createAction.payload.typeId == 'node-normal' || createAction.payload.typeId == 'node-start') {
          let stageData: StageDefinitionInfoUnion;
          //-------------------- They get new Stage Definitions from the api --------------------
          this.defaultGetter.getWorkerDefinition().then((data: StageDefinitionInfoUnion) => {
            //-------------------- Which are used to update the pipelineDefinitionEdit Object --------------------
            this.pipelineDefinitionEdit.stages.push(data);
            stageData = data;
            dispatch(this.createElement(stageData, createAction));
            return;
          }, function (err) {
            console.error("No element created: Error while getting the data for the new element. " + err);
          });

        } else if (createAction.payload.typeId == 'node-and-splitter') {
          let stageData: StageDefinitionInfoUnion;
          this.defaultGetter.getAndSplitterDefinition().then((data: StageDefinitionInfoUnion) => {
            this.pipelineDefinitionEdit.stages.push(data);
            stageData = data;
            dispatch(this.createElement(stageData, createAction));
            return;
          }, function (err) {
            console.log("No element created: Error while getting the data for the new element. " + err);
          });
        } else if (createAction.payload.typeId == 'node-if-splitter') {
          let stageData: StageDefinitionInfoUnion;
          this.defaultGetter.getIfSplitterDefinition().then((data: StageDefinitionInfoUnion) => {
            this.pipelineDefinitionEdit.stages.push(data);
            stageData = data;
            dispatch(this.createElement(stageData, createAction));
            return;
          }, function (err) {
            console.log("No element created: Error while getting the data for the new element. " + err);
          });
        } else if (createAction.payload.typeId == 'node-all-merger') {
          let stageData: StageDefinitionInfoUnion;
          this.defaultGetter.getAllMergerDefinition().then((data: StageDefinitionInfoUnion) => {
            this.pipelineDefinitionEdit.stages.push(data);
            stageData = data;
            dispatch(this.createElement(stageData, createAction));
            return;
          }, function (err) {
            console.log("No element created: Error while getting the data for the new element. " + err);
          });
        } else if (createAction.payload.typeId == 'node-any-merger') {
          let stageData: StageDefinitionInfoUnion;
          this.defaultGetter.getAnyMergerDefinition().then((data: StageDefinitionInfoUnion) => {
            this.pipelineDefinitionEdit.stages.push(data);
            stageData = data;
            dispatch(this.createElement(stageData, createAction));
            return;
          }, function (err) {
            console.log("No element created: Error while getting the data for the new element. " + err);
          });
        }
      } else if (action.type === DiagramMakerActions.EDGE_CREATE) {   //Logic if a Edge can be created or not
        let edgeDestPossible: boolean = true;
        let edgeSrcPossible: boolean = true;
        const edgeMap = new Map(Object.entries(this.diagramMaker.store.getState().edges));
        const nodeMap = this.diagramMaker.store.getState().nodes;
        let createEdgeAction = action as CreateEdgeAction<{}>;
        //Check if edge can be created depending on destination node
        for (let edge of edgeMap.values()) {                //For Stages and Splitter Gateways
          if (edge.dest == createEdgeAction.payload.dest) {
            edgeDestPossible = false;
            break;
          } else {
            edgeDestPossible = true;
          }
        }
        //for Mergers
        if (nodeMap[createEdgeAction.payload.dest].typeId == 'node-any-merger' ||
          nodeMap[createEdgeAction.payload.dest].typeId == 'node-all-merger'
        ) {
          edgeDestPossible = true;
        }
        //Check if edge can be created depending on source Node
        for (let edge of edgeMap.values()) {                //For Stages and Merger Gateways
          if (edge.src == createEdgeAction.payload.src) {
            edgeSrcPossible = false;
            break;
          } else {
            edgeSrcPossible = true;
          }
        } //For Splitters
        if (nodeMap[createEdgeAction.payload.src].typeId == 'node-and-splitter' ||
          nodeMap[createEdgeAction.payload.src].typeId == 'node-if-splitter'
        ) {
          edgeSrcPossible = true;
        }
        if (edgeDestPossible && edgeSrcPossible) {
          this.saveStatus = false;
          let stageToEdit: StageDefinitionInfoUnion | undefined = this.pipelineDefinitionEdit.stages.find((element: StageDefinitionInfoUnion) => element.id === createEdgeAction.payload.src);
          if (stageToEdit) {
            stageToEdit.nextStages.push(createEdgeAction.payload.dest);
          }
          dispatch(createEdgeAction);
        }
      } else if (action.type === DiagramMakerActions.DELETE_ITEMS) {
        let deleteAction = action as DeleteItemsAction;
        const nodeMap = new Map(Object.entries(this.diagramMaker.store.getState().nodes));
        const keyIter = nodeMap.keys()
        if (deleteAction.payload.nodeIds.includes(keyIter.next().value)) {
        }
        else {
          this.saveStatus = false;
          // delete stage from edit object
          this.removeStageFromEditObject(deleteAction.payload.nodeIds[0]);
          // delete stage as nextStage if it was used
          for (let stage of this.pipelineDefinitionEdit.stages) {
            if (stage.nextStages.includes(deleteAction.payload.nodeIds[0])) {
              let delIndexNext = stage.nextStages.findIndex(s => s === deleteAction.payload.nodeIds[0]);
              if (delIndexNext != -1) {
                if (this.deletedStages[0].idsOfPreviousStages) {
                  this.deletedStages[0].idsOfPreviousStages.push(stage.id);
                }

                stage.nextStages.splice(delIndexNext, 1);
              }
            }
          }
          dispatch(deleteAction);
        }
      } else if (action.type === DiagramMakerActions.PANEL_DRAG) {         //Interceptor to fix a bug where the panel clips at the top edge
        let dragAction: DragPanelAction = JSON.parse(JSON.stringify(action));
        dragAction.payload.viewContainerSize.height = 5000;
        dispatch(dragAction);
      } else if (action.type === DiagramMakerActions.NODE_SELECT) {
        let makeBigAction: ResizePanelAction = {
          type: DiagramMakerActions.PANEL_RESIZE,
          payload: {
            id: this.diagramMaker.store.getState().panels.library.id,
            size: {width: 500, height: 500},
          }
        };
        this.diagramMaker.api.dispatch(makeBigAction);
        dispatch(action);
      } else {      //Default dispatch action for all actions that get not intercepted
        dispatch(action);
      }
    },
    nodeTypeConfig: this.configClass.getNodeTypes(),
  };

  nodeComponentInstances: any[] = []; // todo if used add proper type else remove

  constructor(private viewContainerRef: ViewContainerRef,
              private componentFactoryResolver: ComponentFactoryResolver,
              private client: HttpClient,
  ) {
    this.pipelineDefinitionEdit = this.pipelineDefinition;
  }

  editState(editForm: Raw<PipelineDefinitionInfo>) { //used when saving the edits of a node, dispatching them ito the stor of diagrammaker with the custom Update_node action
    const currentState = this.diagramMaker.store.getState();
    let editNode = currentState.nodes[editForm.id];
    if (editNode) {
      let editData = JSON.parse(JSON.stringify(editNode.consumerData));
      editData = editForm;
      editNode = Object.assign({}, editNode, {
        consumerData: editData,
        diagramMakerData: {
          selected: true,
          size: editNode.diagramMakerData.size,
          position: editNode.diagramMakerData.position,
        },
      });
      this.saveStatus = false;
      this.diagramMaker.store.dispatch({
        type: 'UPDATE_NODE',
        payload: editNode,
      });
    }
  }

  createElement(stageData: Raw<StageDefinitionInfo>, createAction: CreateNodeAction<StageDefinitionInfo>) { //helper function used for the intercepted createNode Actions
    let nodeWidth: number;
    if (createAction.payload.typeId == 'node-normal') {
      nodeWidth = 200
    } else {
      nodeWidth = 150
    }
    let newAction: CreateNodeAction<{}> = {
      type: DiagramMakerActions.NODE_CREATE,
      payload: {
        id: `${stageData.id}`,
        typeId: `${createAction.payload.typeId}`,
        position: {x: createAction.payload.position.x, y: createAction.payload.position.y},
        size: {width: nodeWidth, height: 75},
        consumerData: stageData,
      }
    };
    this.saveStatus = false;
    return newAction;
  }

  flatDeleteElement(node: StageDefinitionInfo) {
    let edgeIdsToDelete: string[] = [];
    let edges = Object.values(this.diagramMaker.store.getState().edges);
    for (const edge of edges) {
      if (edge.src === node.id || edge.dest === node.id) {
        edgeIdsToDelete.push(edge.id);
      }
    }
    let deleteAction: DeleteItemsAction = {
      type: DiagramMakerActions.DELETE_ITEMS,
      payload: {
        nodeIds: [node.id],
        edgeIds: edgeIdsToDelete,
      }
    }
    this.diagramMaker.api.dispatch(deleteAction)
    return deleteAction;
  }

  removeStageFromEditObject(stageId: string) {
    const delIndex: number = this.pipelineDefinition.stages.findIndex((stage) => stage.id === stageId);
    const delObject: DeletedStage = {
      stageIndex: delIndex,
      deletedStage: this.pipelineDefinitionEdit.stages[delIndex],
      idsOfPreviousStages: [],
    }
    this.deletedStages.unshift(delObject);
    this.pipelineDefinitionEdit.stages.splice(delIndex, 1);
  }

  reAddStageToEditObject(stageId: string) {
    for (let delStage of this.deletedStages) {
      if (delStage.deletedStage.id === stageId) {
        this.pipelineDefinitionEdit.stages.splice(delStage.stageIndex, 0, delStage.deletedStage);
        this.deletedStages.shift();
      }
      if (delStage.idsOfPreviousStages) {
        for (let prevStage of delStage.idsOfPreviousStages) {
          let editIndex: number = this.pipelineDefinitionEdit.stages.findIndex((stage) => stage.id === prevStage);
          this.pipelineDefinitionEdit.stages[editIndex].nextStages.push(delStage.deletedStage.id);
        }
      }

    }
  }

  /*deepDeleteElement(node: StageDefinitionInfo) {
    console.dir(this.diagramMaker.store.getState());
    let edgeIdsToDelete: string[] = [];
    let nodeIdsToDelete: string[] =[node.id];
    const edges = Object.values(this.diagramMaker.store.getState().edges);
    const nodes = Object.values(this.diagramMaker.store.getState().nodes);

    let nodesToCheck: string[] = [];

    for (let edge of edges) {
      if (edge.dest === node.id) {
        edgeIdsToDelete.push(edge.id);
      } else if (edge.src === node.id) {
        nodesToCheck.push(edge.dest);
      }
    }

    while (nodesToCheck.length > 0) {
      for (let node of nodesToCheck) {
        for (let edge of edges) {
          if (edge.dest === node) {
            edgeIdsToDelete.push(edge.id);
          }
        }
        nodeIdsToDelete.push(node);
        for (let edge of edges) {
          if (edge.src === node) {
            console.log("Edge " + edge.id + " to " + edge.dest);
            nodesToCheck.push(edge.dest);
          }
        }
      }
      nodesToCheck.shift();
      //import pipelines button
    }

    let deleteAction: DeleteItemsAction = {
      type: DiagramMakerActions.DELETE_ITEMS,
      payload: {
        nodeIds: nodeIdsToDelete,
        edgeIds: edgeIdsToDelete,
      }
    }
    this.diagramMaker.api.dispatch(deleteAction)
    return deleteAction;
  }*/

  ngOnInit() {
    this.initialData = this.initClass.getInitData(this.pipelineDefinition);

  }

  ngOnChanges(changes:SimpleChanges) {
    setTimeout(() => {
      if (changes.pipelineDefinition) {
        if(changes.pipelineDefinition.currentValue && changes.pipelineDefinition.previousValue)
          if (changes.pipelineDefinition.currentValue.id !== changes.pipelineDefinition.previousValue.id) {
            this.ngOnDestroy();
            this.libraryComponent?.instance.cancelEdit();
            this.libraryComponent = null;
            this.initialData = this.initClass.getInitData(this.pipelineDefinition);
            this.ngAfterViewInit();
          }
      }
    }, 100);

  }


  ngAfterViewInit(): void {
    setTimeout(() => {
      this.diagramMaker = new DiagramMaker(
        this.diagramEditorContainer.nativeElement,
        this.config,
        {
          initialData: this.initialData,
          consumerRootReducer: (state: any, action: any) => {   //new action for diagramMaker to update the consumerData when editing a node
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
      this.configClass.getApiSwitch('layout', this.diagramMaker);
    }, 500);

  }

  ngOnDestroy(): void {
    this.diagramMaker.destroy();
    this.libraryComponent?.destroy();
    this.nodeComponentInstances.forEach(instance => instance.destroy());
  }


}
