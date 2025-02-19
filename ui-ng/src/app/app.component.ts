import { Component } from '@angular/core';
import {RxStompService} from './rx-stomp.service';
import {RxStompState} from '@stomp/rx-stomp';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {

  connected = false;

  constructor(private rxStompService: RxStompService) {
    rxStompService
      .connectionState$
      .subscribe((connected) => {
        this.connected = (connected === RxStompState.OPEN);
      });
  }
}
