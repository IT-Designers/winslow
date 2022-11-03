import {
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
  QueryList,
  ViewChildren
} from '@angular/core';
import {FormBuilder, FormGroup} from "@angular/forms";

@Component({
  selector: 'app-edit-forms',
  templateUrl: './edit-forms.component.html',
  styleUrls: ['./edit-forms.component.css']
})
export class EditFormsComponent implements OnInit {

  @Input() formMap;
  @Input() formObj;
  @Input() objPlace;
  @Output() onCollectData : EventEmitter<Object> = new EventEmitter();
  editForm: FormGroup;
  extended: boolean[];

  @ViewChildren('form') childForm:QueryList<EditFormsComponent>;

  constructor( private fb: FormBuilder) { }

  ngOnInit(): void {
    this.extended =Array(this.formMap.lenght);
    this.extended.fill(false);
    //console.log(this.formObj);
    console.log(this.objPlace);
    this.editForm = this.fb.group(this.formObj);
    //console.log(this.editForm);
    //this.editForm.valueChanges.subscribe(value => this.sendFormData())
  }

  public keepOriginalOrder = (a, b) => a.key;

  isNotObject(prop) : boolean {
    if (typeof prop === "object" && prop != null){
      //console.log(Object.keys(prop).length <= 0);
      return Object.keys(prop).length <= 0;
    }
    else {return true;}
  }

  collectFormData(collectedFormData){
    this.formObj = this.editForm.value;
    this.formObj[collectedFormData[0]] = collectedFormData[1];
    this.editForm = this.fb.group(this.formObj);
  }
  sendFormData(){
    if (this.childForm){
      this.childForm.forEach(ProfileImage => {
        ProfileImage.sendFormData();
      });
    }
    this.onCollectData.emit([this.objPlace, this.editForm.value]);
  }

  extendData(index){
    this.extended[index] = !this.extended[index];
  }

}
