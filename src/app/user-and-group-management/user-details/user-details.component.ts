import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {UserInfo} from '../../api/user-api.service';

@Component({
  selector: 'app-user-details',
  templateUrl: './user-details.component.html',
  styleUrls: ['./user-details.component.css']
})
export class UserDetailsComponent implements OnInit {

  @Input() selectedUser: UserInfo = null;

  @Output() deletedUserEmitter = new EventEmitter();

  constructor() { }

  ngOnInit(): void {
  }

  onUserDelete() {
    this.deletedUserEmitter.emit(this.selectedUser);
  }

}
