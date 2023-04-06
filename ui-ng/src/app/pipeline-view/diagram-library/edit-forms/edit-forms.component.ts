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
    let newFormObj = JSON.parse(JSON.stringify(formObj));   //copy necessary because of read only values
    for(let [key, value] of Object.entries(this.formObj$)){   //Array values get replaced with Array-like Object - necessary for Angular form Group
      if (value instanceof Array){
        newFormObj[key] = Object.assign({}, value);
      }
    }
    this.editForm = this.fb.group(newFormObj);
    if (this.formMap$) {
      this.extended = Array(this.formMap$.lenght);    //Array to determine which Abject fields are extended
      this.extended.fill(false);
    }
  };
  @Input()
  set formMap(formMap){     //Map for the display of the values in html
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

  collectFormData(collectedFormData){   //puts the received data in the form obj
    this.formObj$ = this.editForm.value;
    this.formObj$[collectedFormData[0]] = collectedFormData[1];
  }
  sendFormData(){           //collects data from childForms down the recursion and sends the collected data one layer up the recursion
    if (this.childForm){
      this.childForm.forEach(ProfileImage => {
        ProfileImage.sendFormData();
      });
    }
   if(this.formObj$ instanceof Array){
     let newArray : Array<String>  = Object.assign([], this.editForm.value);
     this.formObj$ = newArray;
   }
    this.onCollectData.emit([this.objPlace, this.formObj$]);
  }
  triggerSaveData(){        //goes up the recursion to trigger the save on top level
    this.onTriggerSaveData.emit();
  }

  extendData(index){
    this.extended[index] = !this.extended[index];
  }
  addContent(entry){                                //entered by clicking the plus to add a new entry to an array
    if (entry.value instanceof Array){
      let newArray : Array<String>  = Object.assign([], this.formObj$[entry.key]);
      newArray.push("New Entry");
      this.formObj$[entry.key] = newArray as Array<String>;
      this.formMap$.set(entry.key , newArray);
      //this.editForm.patchValue({entry.key: })
      //this.triggerSaveData();
    }
  }
  deleteContent(entry){                                //entered by clicking the minus to delete the entry from the array
    if (this.formObj$.includes(entry.value)){
      let newArray : Array<String>  = Object.assign([], this.formObj$);
      newArray.splice(entry.key, 1);
      this.formObj$ = newArray as Array<String>;
      this.formMap$ = new Map();
      for (const key of Object.keys(this.formObj$)) {
        this.formMap$.set(key, this.formObj$[key]);
      }
    }
  }
  toDisplayProp(entry){
    if (entry ==  "id" || entry == "nextStages" || entry == "@type" || entry == "gatewaySubType"){
      return false;
    }
    else{return true;}
  }
  isArray(entry){
    if (entry instanceof Array){
      return true;
    }
    else{return false;}
  }
  isInsideArray(entry){
    if (this.formObj$ instanceof Array){
      if(this.formObj$.includes(entry)){
        return true;
      }
    }
    else{return false;}
  }


}
