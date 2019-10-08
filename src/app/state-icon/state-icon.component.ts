import {Component, Input, OnInit} from '@angular/core';
import {State} from '../api/project-api.service';

@Component({
  selector: 'app-state-icon',
  templateUrl: './state-icon.component.html',
  styleUrls: ['./state-icon.component.css']
})
export class StateIconComponent implements OnInit {

  @Input() state: State;
  @Input() animation = false;

  constructor() { }

  ngOnInit() {
  }

  stateString() {
    return this.state != null ? (this.state + '').toLowerCase() : null;
  }

}
