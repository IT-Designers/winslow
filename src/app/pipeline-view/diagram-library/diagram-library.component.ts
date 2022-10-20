import {Component, Input, OnInit, Output, EventEmitter} from '@angular/core';
import {DiagramMakerNode} from "diagram-maker";
import {StageDefinitionInfo} from "../../api/project-api.service";
import {FormGroup, FormBuilder} from "@angular/forms";

//te

@Component({
  selector: 'app-diagram-library',
  templateUrl: './diagram-library.component.html',
  styleUrls: ['./diagram-library.component.css']
})
export class DiagramLibraryComponent implements OnInit {

  selectedNode$?: DiagramMakerNode<StageDefinitionInfo>;
  editForm: FormGroup;

  constructor( private fb: FormBuilder) {
  }
  ngOnInit(): void {
    this.editForm = this.fb.group({
      stageName: `${this.selectedNode$?.consumerData?.name ? this.selectedNode$.consumerData.name : ''}`,
      imageName: `${this.selectedNode$?.consumerData?.image?.name ? this.selectedNode$.consumerData.image.name : ''}`,
      id: `${this.selectedNode$?.id}`
    });

  }
  saveEdit(){
    //console.log(this.editForm);
    this.editNode.emit(this.editForm.value);
    this.cancelEdit();
  }
  cancelEdit() {
    this.selectedNode$ = undefined;
    this.resetSelectedNode.emit();
  }

  @Input()
  set selectedNode(selectedNode: DiagramMakerNode<StageDefinitionInfo>) {
    this.selectedNode$ = selectedNode;
  }

  @Output() resetSelectedNode = new EventEmitter();
  @Output() editNode = new EventEmitter();


}
