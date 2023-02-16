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


  @Input() objPlace;
  @Output() onCollectData : EventEmitter<Object> = new EventEmitter();
  @Output() onTriggerSaveData : EventEmitter<Object> = new EventEmitter();
  editForm: FormGroup;
  extended: boolean[] = [];
  formMap$;
  formObj$;

  @ViewChildren('form') childForm:QueryList<EditFormsComponent>;

  constructor( private fb: FormBuilder) { }


  ngOnInit(): void {
  }

  @Input()
  set formObj(formObj){
    this.formObj$ = JSON.parse(JSON.stringify(formObj));
    //console.log(formObj);
    //console.log(typeof formObj)
    this.editForm = this.fb.group(formObj);
    console.log(this.editForm);
    //console.log("inputFormObj")
    //console.log(this.formObj$)
    if (this.formMap$) {
      this.extended = Array(this.formMap$.lenght);
      this.extended.fill(false);
      //console.log("inputForMap")
      //console.log(this.formMap$)
    }
  };
  @Input()
  set formMap(formMap){
    let formHtmlMap = new Map();
    for (const key of Object.keys(formMap)) {
      formHtmlMap.set(key, formMap[key]);
    }
    this.formMap$ = formHtmlMap;

  };

  public keepOriginalOrder = (a, b) => a.key;

  isNotObject(prop) : boolean {
    if (typeof prop == "number" || typeof prop == "string" || typeof prop == "boolean"){
      return true;
    }
    else {return false;}
  }

  collectFormData(collectedFormData){
    this.formObj$ = this.editForm.value;
    this.formObj$[collectedFormData[0]] = collectedFormData[1];
    console.log(collectedFormData)
    //this.editForm.setValue(this.formObj.value);
  }
  sendFormData(){
    if (this.childForm){
      this.childForm.forEach(ProfileImage => {
        ProfileImage.sendFormData();
      });
    }
    console.log(this.objPlace);
    console.log(this.formObj$);
    this.onCollectData.emit([this.objPlace, this.formObj$]);
  }
  triggerSaveData(){
    this.onTriggerSaveData.emit();
  }

  extendData(index){
    this.extended[index] = !this.extended[index];
  }
  addContent(entry){                                //entered by clicking the plus to add a new entry to an array
    //console.log(entry.value instanceof Array);
    //console.log(this.formObj$);
    if (entry.value instanceof Array){
      //let newArray = new Array();
      let newArray : Array<String>  = Object.assign([], this.formObj$[entry.key]);
      newArray.push("New Entry");
      console.log(this.formObj$[entry.key]);
      this.formObj$[entry.key] = newArray as Array<String>;
      console.log(this.formMap$);
      this.formMap$.set(entry.key , newArray);
      //this.editForm.patchValue({entry.key: })
      //this.triggerSaveData();
    }
    //console.log(this.formObj$);
    //console.log(this.formMap$);
    //console.log(this.editForm);
  }
  deleteContent(entry){                                //entered by clicking the plus to add a new entry to an array
    if (entry.value instanceof Array){
      //let newArray = new Array();
      let newArray : Array<String>  = Object.assign([], this.formObj$[entry.key]);
      newArray.pop();
      console.log(this.formObj$[entry.key]);
      this.formObj$[entry.key] = newArray as Array<String>;
      console.log(this.formMap$);
      this.formMap$.set(entry.key , newArray);
    }
  }
  toDisplayProp(entry){
    if (entry ==  "id" || entry == "nextStages" || entry == "@type" || entry == "gatewaySubType"){
      return false;
    }
    else{return true;}
  }
  isArray(entry){
    //console.log(entry);
    if (entry instanceof Array){
      return true;
    }
    else{return false;}
  }


}
