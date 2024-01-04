import {Component, EventEmitter, Input, OnInit, Output, ViewChild} from '@angular/core';
import { CommonModule } from '@angular/common';
import {MatButtonModule} from "@angular/material/button";
import {MatFormFieldModule} from "@angular/material/form-field";
import {MatIconModule} from "@angular/material/icon";
import {MatInputModule} from "@angular/material/input";
import {MatTooltipModule} from "@angular/material/tooltip";
import {StageWorkerDefinitionInfo} from "../../../api/winslow-api";
import {findIndex} from "rxjs";

@Component({
  selector: 'app-argument-list',
  templateUrl: './argument-list.component.html',
  styleUrl: './argument-list.component.css'
})
export class ArgumentListComponent implements OnInit{
  @Input() argList: string[] | undefined;
  @Input() argName: string = 'Arg';
  @Output() changeEmitter = new EventEmitter();
  changedArgList: string[] = [''];

  @ViewChild('newArgInput') input: any;


  ngOnInit() {
  }

  deleteArg(arg: string): void {
    if (this.argList) {
      console.log('Delete ' + arg + ' from array');
      let delIndex: number = this.argList.findIndex((value) => value === arg);
      this.argList.splice(delIndex, 1);
      this.changeEmitter.emit(this.argList)
    }
  }

  addArg(arg: string): void {
    if (this.argList) {
      this.argList.push(arg);
      this.input.nativeElement.value = '';
      this.changeEmitter.emit(this.argList);
    }
  }
}
