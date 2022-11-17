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
import {ResourceInfo} from "../../../api/pipeline-api.service";
import {ImageInfo} from "../../../api/project-api.service";

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
  @Output() onTriggerSaveData : EventEmitter<Object> = new EventEmitter();
  editForm: FormGroup;
  extended: boolean[];

  @ViewChildren('form') childForm:QueryList<EditFormsComponent>;

  constructor( private fb: FormBuilder) { }

  ngOnInit(): void {
    console.log(this.formObj);
    console.log(this.formMap);
    this.extended = Array(this.formMap.lenght);
    this.extended.fill(false);
    this.editForm = this.fb.group(this.formObj);

    //this.editForm.valueChanges.subscribe(value => this.triggerSaveData())
  }

  public keepOriginalOrder = (a, b) => a.key;

  isNotObject(prop) : boolean {
    if (typeof prop == "number" || typeof prop == "string"){
      return true;
    }
    else {return false;}
  }

  collectFormData(collectedFormData){
    this.formObj = this.editForm.value;
    this.formObj[collectedFormData[0]] = collectedFormData[1];
    //console.log(collectedFormData)
    //this.editForm.setValue(this.formObj.value);
  }
  sendFormData(){
    if (this.childForm){
      this.childForm.forEach(ProfileImage => {
        ProfileImage.sendFormData();
      });
    }
    this.onCollectData.emit([this.objPlace, this.editForm.value]);
  }
  triggerSaveData(){
    this.onTriggerSaveData.emit();
  }

  extendData(index){
    this.extended[index] = !this.extended[index];
  }
  addContent(entry){
    //console.log(entry.value instanceof Array);
    console.log(entry.value instanceof ImageInfo);
    //console.log(entry);
    //console.log(this.editForm)
    if (entry.value instanceof Array){
      //let newArray = new Array();
      let newArray : String[]  = Object.assign([], this.formObj[entry.key]);
      console.log(newArray);
      newArray.push("New Entry");
      this.formObj[entry.key] = newArray;
      this.formMap.set(entry.key , newArray);
      //this.editForm.patchValue({entry.key: })
      //this.triggerSaveData();
    }
    console.log(this.formObj);
    console.log(this.formMap);
    console.log(this.editForm);
  }

}
