import {Component, OnInit} from '@angular/core';
import {RxStompService} from '@stomp/ng2-stompjs';
import {RxStompState} from '@stomp/rx-stomp';
import {UserApiService} from '../api/user-api.service';

@Component({
  selector: 'app-topbar',
  templateUrl: './topbar.component.html',
  styleUrls: ['./topbar.component.css']
})
export class TopbarComponent implements OnInit {

  connected = false;

  myUserName: string;

  constructor(private rxStompService: RxStompService, private userApi: UserApiService) {
    rxStompService
      .connectionState$
      .subscribe((connected) => {
        this.connected = (connected === RxStompState.OPEN);
      });
  }

  ngOnInit() {
    this.userApi.getSelfUserName()
      .then((name) => this.myUserName = name);
  }

}
