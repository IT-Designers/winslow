import {Component, Input, OnInit} from '@angular/core';
import {IState} from '../api/winslow-api';

@Component({
  selector: 'app-state-icon',
  templateUrl: './state-icon.component.html',
  styleUrls: ['./state-icon.component.css']
})
export class StateIconComponent implements OnInit {

  @Input() state: IState;
  @Input() animation = false;
  @Input() configured = false;

  constructor() { }

  ngOnInit() {
  }

  stateString() {
    if (this.configured && this.state !== 'Failed') {
      return 'configured';
    } else {
      return this.state != null ? (this.state + '').toLowerCase() : null;
    }
  }

}
