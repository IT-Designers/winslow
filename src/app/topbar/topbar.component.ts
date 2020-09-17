import {Component, OnInit} from '@angular/core';
import {RxStompService} from '@stomp/ng2-stompjs';
import {RxStompState} from '@stomp/rx-stomp';

@Component({
  selector: 'app-topbar',
  templateUrl: './topbar.component.html',
  styleUrls: ['./topbar.component.css']
})
export class TopbarComponent implements OnInit {

  connected = false;

  constructor(private rxStompService: RxStompService) {
    rxStompService
      .connected$
      .subscribe((connected) => {
        this.connected = (connected === RxStompState.OPEN);
      });
  }

  ngOnInit() {
  }

}
