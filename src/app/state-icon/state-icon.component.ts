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
  @Input() configured = false;

  constructor() { }

  ngOnInit() {
  }

  stateString() {
    if (this.configured && this.state !== State.Failed) {
      return 'configured';
    } else {
      return this.state != null ? (this.state + '').toLowerCase() : null;
    }
  }

}
