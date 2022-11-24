import {Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges} from '@angular/core';
import {UserApiService, UserInfo} from '../../api/user-api.service';
import {DialogService} from '../../dialog.service';

@Component({
  selector: 'app-user-details',
  templateUrl: './user-details.component.html',
  styleUrls: ['./user-details.component.css']
})
export class UserDetailsComponent implements OnInit, OnChanges {

  @Input() selectedUser: UserInfo = null;
  @Input() myName: string = null;

  @Output() deletedUserEmitter = new EventEmitter();

  canIEditUser = false;



  constructor(private userApi: UserApiService, private dialog: DialogService) { }

  ngOnInit(): void {
  }

  ngOnChanges(changes: SimpleChanges) {
    if (this.myName === this.selectedUser.name) {
      this.canIEditUser = true;
    } else {
      this.userApi.hasSuperPrivileges(this.myName)
        .then((bool) => {
          console.log('Am I a super user? ' + bool);
          this.canIEditUser = bool;
        });
    }
    console.log('Can i edit the user? ' + this.canIEditUser);
  }

  onUserDelete() {
    this.deletedUserEmitter.emit(this.selectedUser);
  }

  canIUpdate() {
    if (this.myName === this.selectedUser.name) {
      this.canIEditUser = true;
    } else {
      this.userApi.hasSuperPrivileges(this.myName)
        .then((bool) => {
          console.log('Am I a super user? ' + bool);
          this.canIEditUser = bool;
        });
    }
  }

  onUpdate() {
    console.dir(this.selectedUser);
    this.dialog.openLoadingIndicator(this.userApi.updateUser(this.selectedUser),
      'Updating User');
  }

}
